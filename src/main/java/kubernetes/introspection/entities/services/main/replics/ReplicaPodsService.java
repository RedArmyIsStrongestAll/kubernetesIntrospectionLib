package kubernetes.introspection.entities.services.main.replics;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.pod.PodInfo;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelCallChainService;
import kubernetes.introspection.entities.services.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.REPLICA_PODS_NOT_FOUND;

@Slf4j
public class ReplicaPodsService {
    private final KubernetesClient kubernetesClient;
    private final OwnerLabelCallChainService ownerLabelCallChainService;

    public ReplicaPodsService(KubernetesClient kubernetesClient, OwnerLabelCallChainService ownerLabelCallChainService) {
        this.kubernetesClient = kubernetesClient;
        this.ownerLabelCallChainService = ownerLabelCallChainService;
    }


    public ReplicaPodsDto getReplicaPodsWithPermission(OwnerReference ownerRef, OwnerDto ownerDto,
                                                       Pod currentPod, PermissionInfo permissionInfo) {
        log.info("Start getReplicaPodsWithPermission for owner: {} from pod {}", ownerDto.getK8sType().getOriginalName(), currentPod);
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(ResourcePermissionEnum.PODS_LIST,
                    ResourcePermissionEnum.PODS_GET));

            return getReplicaPods(ownerRef, ownerDto, currentPod);
        } catch (KubernetesException e) {
            log.error("Stop getReplicaPodsWithPermission: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }

    public ReplicaPodsDto getReplicaPods(OwnerReference ownerRef, OwnerDto ownerDto, Pod currentPod) {
        log.info("Start getReplicaPods for owner: {} from pod {}", ownerDto.getK8sType().getOriginalName(), currentPod);
        try {
            List<Pod> replicas = new ArrayList<>();

            if (OwnerTypeEnum.STATEFULSET.equals(ownerDto.getK8sType())) {
                replicas.addAll(findStatefulSetPods(ownerRef, currentPod));
            } else {
                replicas.addAll(findPodsByOwnerSelector(ownerDto, currentPod));
            }

            if (replicas.isEmpty()) {
                log.warn("No replicas found for owner {} from pod {}", ownerDto.getK8sType().getOriginalName(), currentPod);
                throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
            }

            return new ReplicaPodsDto(replicas);
        } catch (KubernetesException e) {
            log.error("Stop getReplicaPods: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }


    private List<Pod> findPodsByOwnerSelector(OwnerDto ownerDto, Pod currentPod) {
        log.info("Start findPodsByOwnerSelector");
        try {
            LabelSelector selector = ownerLabelCallChainService.getSelector(ownerDto.getK8sType(), ownerDto.getK8sObject());
            if (selector == null) return Collections.emptyList();
            Map<String, String> matchLabels = selector.getMatchLabels();
            if (matchLabels == null || matchLabels.isEmpty()) return Collections.emptyList();

            String namespace = currentPod.getMetadata().getNamespace();

            log.info("Start k8s request");
            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withLabels(matchLabels)
                    .list()
                    .getItems()
                    .stream()
                    .filter(pod -> !isCurrentPod(pod, currentPod))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Stop findPodsByOwnerSelector: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }

    private List<Pod> findStatefulSetPods(OwnerReference ownerRef, Pod currentPod) {
        log.info("Start findStatefulSetPods");
        try {
            String namespace = currentPod.getMetadata().getNamespace();
            String stsName = ownerRef.getName();
            String podNamePrefix = stsName + "-";

            log.info("Start k8s request");
            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .list()
                    .getItems()
                    .stream()
                    .filter(pod -> pod.getMetadata().getName().startsWith(podNamePrefix))
                    .filter(pod -> !isCurrentPod(pod, currentPod))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Stop findStatefulSetPods: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }

    private boolean isCurrentPod(Pod pod, Pod currentPod) {
        return currentPod.getMetadata().getName().equals(pod.getMetadata().getName()) &&
                currentPod.getMetadata().getNamespace().equals(pod.getMetadata().getNamespace());
    }


    @Getter
    public static class ReplicaPodsDto {
        private final List<Pod> k8sPodList;
        private final List<PodInfo> podInfoList;

        public ReplicaPodsDto(List<Pod> k8sPodList) {
            this.k8sPodList = k8sPodList;
            this.podInfoList = k8sPodList.stream()
                    .map(CurrentPodService::mapToPodInfo)
                    .collect(Collectors.toList());
        }

        public ReplicaPodsDto(List<Pod> k8sPodList, List<PodInfo> podInfoList) {
            this.k8sPodList = k8sPodList;
            this.podInfoList = podInfoList;
        }
    }
}