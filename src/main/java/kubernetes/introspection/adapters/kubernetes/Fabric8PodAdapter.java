package kubernetes.introspection.adapters.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.pod.ContainerInfo;
import kubernetes.introspection.entities.pod.ContainerStateEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;

import java.util.*;
import java.util.stream.Collectors;

public class Fabric8PodAdapter implements KubernetesPodPort {

    private final KubernetesClient client;

    public Fabric8PodAdapter(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public PodInfo getPodByName(String name, String namespace) {
        Pod pod = client.pods().inNamespace(namespace).withName(name).get();
        return mapToPodInfo(pod);
    }

    @Override
    public List<PodInfo> listPodsByLabels(Map<String, String> labels, String namespace) {
        return client.pods().inNamespace(namespace).withLabels(labels).list().getItems()
                .stream().map(this::mapToPodInfo).collect(Collectors.toList());
    }

    @Override
    public List<PodInfo> listAllPods(String namespace) {
        return client.pods().inNamespace(namespace).list().getItems()
                .stream().map(this::mapToPodInfo).collect(Collectors.toList());
    }

    private PodInfo mapToPodInfo(Pod pod) {
        if (pod == null) return null;

        ObjectMeta metadata = pod.getMetadata();
        PodSpec spec = pod.getSpec();
        PodStatus status = pod.getStatus();

        return PodInfo.builder()
                .name(metadata != null ? metadata.getName() : null)
                .uid(metadata != null ? metadata.getUid() : null)
                .namespace(metadata != null ? metadata.getNamespace() : null)
                .labels(metadata != null ? metadata.getLabels() : Collections.emptyMap())
                .phase(status != null ? status.getPhase() : null)
                .qosClass(status != null ? status.getQosClass() : null)
                .creationTimestamp(metadata != null ? metadata.getCreationTimestamp() : null)
                .deletionTimestamp(metadata != null ? metadata.getDeletionTimestamp() : null)
                .nodeName(spec != null ? spec.getNodeName() : null)
                .podIP(status != null ? status.getPodIP() : null)
                .containers(extractContainers(pod))
                .ownerReferences(extractOwnerReferences(metadata))
                .configMapRefs(extractConfigMapRefs(pod))
                .secretRefs(extractSecretRefs(pod))
                .build();
    }

    private List<OwnerReferenceInfo> extractOwnerReferences(ObjectMeta metadata) {
        if (metadata == null || metadata.getOwnerReferences() == null) {
            return Collections.emptyList();
        }
        return metadata.getOwnerReferences().stream()
                .map(ref -> OwnerReferenceInfo.builder()
                        .kind(ref.getKind())
                        .name(ref.getName())
                        .uid(ref.getUid())
                        .apiVersion(ref.getApiVersion())
                        .controller(ref.getController())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ContainerInfo> extractContainers(Pod pod) {
        PodSpec podSpec = pod.getSpec();
        if (podSpec == null || podSpec.getContainers() == null) {
            return Collections.emptyList();
        }

        List<ContainerStatus> containerStatuses = Optional.ofNullable(pod.getStatus())
                .map(PodStatus::getContainerStatuses)
                .orElse(Collections.emptyList());

        return podSpec.getContainers().stream()
                .map(container -> {
                    ContainerStatus cs = containerStatuses.stream()
                            .filter(s -> container.getName().equals(s.getName()))
                            .findFirst()
                            .orElse(null);

                    return ContainerInfo.builder()
                            .name(container.getName())
                            .image(container.getImage())
                            .imageID(cs != null ? cs.getImageID() : null)
                            .containerID(cs != null ? cs.getContainerID() : null)
                            .state(cs != null && cs.getState() != null
                                    ? ContainerStateEnum.parserFromKubernetes(cs.getState().toString())
                                    : null)
                            .stateReason(cs != null && cs.getState() != null && cs.getState().getWaiting() != null
                                    ? cs.getState().getWaiting().getReason() : null)
                            .restartCount(cs != null ? cs.getRestartCount() : 0)
                            .lastTerminationReason(cs != null && cs.getLastState() != null
                                    && cs.getLastState().getTerminated() != null
                                    ? cs.getLastState().getTerminated().getReason() : null)
                            .waitingMessage(cs != null && cs.getState() != null
                                    && cs.getState().getWaiting() != null
                                    ? cs.getState().getWaiting().getMessage() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<String, Set<ConfigUsageTypeEnum>> extractConfigMapRefs(Pod pod) {
        if (pod.getSpec() == null) return Collections.emptyMap();
        Map<String, Set<ConfigUsageTypeEnum>> result = new HashMap<>();

        if (pod.getSpec().getContainers() != null) {
            pod.getSpec().getContainers().forEach(container -> {
                if (container.getEnv() != null) {
                    container.getEnv().forEach(envVar -> {
                        if (envVar.getValueFrom() != null && envVar.getValueFrom().getConfigMapKeyRef() != null) {
                            String name = envVar.getValueFrom().getConfigMapKeyRef().getName();
                            result.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV);
                        }
                    });
                }
                if (container.getEnvFrom() != null) {
                    container.getEnvFrom().forEach(envFrom -> {
                        if (envFrom.getConfigMapRef() != null) {
                            String name = envFrom.getConfigMapRef().getName();
                            result.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV_FROM);
                        }
                    });
                }
            });
        }

        if (pod.getSpec().getVolumes() != null) {
            pod.getSpec().getVolumes().forEach(volume -> {
                if (volume.getConfigMap() != null) {
                    String name = volume.getConfigMap().getName();
                    result.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.VOLUME);
                }
            });
        }

        return result;
    }

    private Map<String, Set<ConfigUsageTypeEnum>> extractSecretRefs(Pod pod) {
        if (pod.getSpec() == null) return Collections.emptyMap();
        Map<String, Set<ConfigUsageTypeEnum>> result = new HashMap<>();

        if (pod.getSpec().getContainers() != null) {
            pod.getSpec().getContainers().forEach(container -> {
                if (container.getEnv() != null) {
                    container.getEnv().forEach(envVar -> {
                        if (envVar.getValueFrom() != null && envVar.getValueFrom().getSecretKeyRef() != null) {
                            String name = envVar.getValueFrom().getSecretKeyRef().getName();
                            result.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV);
                        }
                    });
                }
                if (container.getEnvFrom() != null) {
                    container.getEnvFrom().forEach(envFrom -> {
                        if (envFrom.getSecretRef() != null && envFrom.getSecretRef().getName() != null) {
                            String name = envFrom.getSecretRef().getName();
                            result.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV_FROM);
                        }
                    });
                }
            });
        }

        if (pod.getSpec().getVolumes() != null) {
            pod.getSpec().getVolumes().forEach(volume -> {
                if (volume.getSecret() != null) {
                    String name = volume.getSecret().getSecretName();
                    result.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.VOLUME);
                }
            });
        }

        return result;
    }
}
