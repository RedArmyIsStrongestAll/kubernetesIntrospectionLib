package kubernetes.introspection.entities.services.main.pod;


import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.pod.ContainerInfo;
import kubernetes.introspection.entities.models.dto.pod.ContainerStateEnum;
import kubernetes.introspection.entities.models.dto.pod.PodInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.permision.PermissionService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.BROKEN_NAME_IN_POD;
import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.POD_NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
public abstract class CurrentPodService {

    protected final KubernetesClient kubernetesClient;
    protected String podName;
    protected final String namespace;


    public static PodInfo mapToPodInfo(Pod pod) {
        log.info("Start mapToPodInfo");
        try {
            if (pod == null) {
                log.warn("Pod is null in mapToPodInfo");
                return null;
            }

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
                    .build();
        } catch (Exception e) {
            log.error("Error on mapToPodInfo", e);
            throw e;
        }
    }

    public static List<ContainerInfo> extractContainers(Pod pod) {
        log.info("Start extractContainers");
        try {
            PodSpec podSpec = pod.getSpec();
            if (podSpec == null || podSpec.getContainers() == null) {
                log.warn("PodSpec or containers list is null");
                return Collections.emptyList();
            }

            List<ContainerStatus> containerStatuses = Optional.ofNullable(pod.getStatus())
                    .map(PodStatus::getContainerStatuses)
                    .orElse(Collections.emptyList());

            return podSpec.getContainers().stream()
                    .map(container -> {
                        ContainerStatus status = containerStatuses.stream()
                                .filter(s -> container.getName().equals(s.getName()))
                                .findFirst()
                                .orElse(null);

                        return ContainerInfo.builder()
                                .name(container.getName())
                                .image(container.getImage())
                                .imageID(status != null ? status.getImageID() : null)
                                .containerID(status != null ? status.getContainerID() : null)
                                .state(
                                        status != null && status.getState() != null
                                                ? ContainerStateEnum.parserFromKubernetes(status.getState().toString())
                                                : null
                                )
                                .stateReason(
                                        status != null
                                                && status.getState() != null
                                                && status.getState().getWaiting() != null
                                                ? status.getState().getWaiting().getReason()
                                                : null
                                )
                                .restartCount(status != null ? status.getRestartCount() : 0)
                                .lastTerminationReason(
                                        status != null
                                                && status.getLastState() != null
                                                && status.getLastState().getTerminated() != null
                                                ? status.getLastState().getTerminated().getReason()
                                                : null
                                )
                                .waitingMessage(
                                        status != null
                                                && status.getState() != null
                                                && status.getState().getWaiting() != null
                                                ? status.getState().getWaiting().getMessage()
                                                : null
                                )
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error on extractContainers", e);
            throw e;
        }
    }


    public CurrentPodDto getCurrentPodWithCheckPermissions(PermissionInfo permissionInfo) {
        log.info("Start getCurrentPodInfoWithCheckPermissions in {}", getNameClassExt());
        try {
            PermissionService.checkPermission(permissionInfo, this::getPermissionResource);

            return getCurrentPod();
        } catch (Exception e) {
            log.error("Error getCurrentPodInfoWithCheckPermissions in {}, ", getNameClassExt(), e);
            throw new KubernetesException(POD_NOT_FOUND);
        }
    }

    public CurrentPodDto getCurrentPod() {
        log.info("Start getCurrentPodInfo in {}", getNameClassExt());

        try {
            podName = getPodName();
            log.info("Possible name of the pod is {}", podName);

            if (podName == null || podName.isBlank()) {
                log.error("Error start getCurrentPodInfo in {}", getNameClassExt());
                log.error(BROKEN_NAME_IN_POD.getMessage());
                throw new Exception();
            }

            Pod pod = getPod();
            if (pod == null) {
                log.error(POD_NOT_FOUND.getMessage());
                throw new Exception();
            }
            log.info("{}: current pod was found", getNameClassExt());

            PodInfo podInfo = mapToPodInfo(pod);
            log.info("{}: create info for current pod ", getNameClassExt());

            return new CurrentPodDto(pod, podInfo);

        } catch (Exception e) {
            log.error("Error getCurrentPod in {}, ", getNameClassExt(), e);
            log.error(POD_NOT_FOUND.getMessage());
            throw new KubernetesException(POD_NOT_FOUND);
        }
    }


    protected abstract List<ResourcePermissionEnum> getPermissionResource();

    protected abstract String getNameClassExt();

    protected abstract String getPodName() throws Exception;

    protected abstract Pod getPod() throws Exception;


    @AllArgsConstructor
    @Getter
    public static class CurrentPodDto {
        private Pod k8sPod;
        private PodInfo podInfo;
    }
}