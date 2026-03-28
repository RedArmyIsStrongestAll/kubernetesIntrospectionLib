package kubernetes.introspection.entities.services.main.pod;


import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
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

import java.util.List;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.BROKEN_NAME_IN_POD;
import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.POD_NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
public abstract class CurrentPodService {

    protected final KubernetesClient kubernetesClient;
    protected String podName;
    protected final String namespace;

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

            PodInfo podInfo = getPodInfo(pod);
            log.info("{}: create info for current pod ", getNameClassExt());

            return new CurrentPodDto(pod, podInfo);

        } catch (Exception e) {
            log.error("Error getCurrentPod in {}, ", getNameClassExt(), e);
            log.error(POD_NOT_FOUND.getMessage());
            throw new KubernetesException(POD_NOT_FOUND);
        }
    }

    public PodInfo getPodInfo(Pod pod) {
        log.info("Start getPodInfo");
        return mapToPodInfo(pod);
    }

    protected PodInfo mapToPodInfo(Pod pod) {
        log.info("Start mapToPodInfo");
        try {
            return PodInfo.builder()
                    .name(pod.getMetadata().getName())
                    .uid(pod.getMetadata().getUid())
                    .namespace(pod.getMetadata().getNamespace())
                    .labels(pod.getMetadata().getLabels())
                    .phase(pod.getStatus().getPhase())
                    .qosClass(pod.getStatus().getQosClass())
                    .creationTimestamp(pod.getMetadata().getCreationTimestamp())
                    .deletionTimestamp(pod.getMetadata().getDeletionTimestamp() != null ? pod.getMetadata().getDeletionTimestamp() : null)
                    .nodeName(pod.getSpec().getNodeName())
                    .podIP(pod.getStatus().getPodIP())
                    .containers(extractContainers(pod))
                    .build();
        } catch (Exception e) {
            log.error("Error on mapToPodInfo, ", e);
            throw e;
        }
    }

    protected List<ContainerInfo> extractContainers(Pod pod) {
        log.info("Start extractContainers");
        try {
            return pod.getSpec().getContainers().stream()
                    .map(container -> {
                        ContainerStatus status = pod.getStatus().getContainerStatuses().stream()
                                .filter(s -> s.getName().equals(container.getName()))
                                .findFirst()
                                .orElse(null);

                        return ContainerInfo.builder()
                                .name(container.getName())
                                .image(container.getImage())
                                .imageID(status != null ? status.getImageID() : null)
                                .containerID(status != null ? status.getContainerID() : null)
                                .state(status != null ? ContainerStateEnum.parserFromKubernetes(status.getState().toString()) : null)
                                .stateReason(status != null && status.getState().getWaiting() != null ? status.getState().getWaiting().getReason() : null)
                                .restartCount(status != null ? status.getRestartCount() : 0)
                                .lastTerminationReason(status != null && status.getLastState() != null && status.getLastState().getTerminated() != null
                                        ? status.getLastState().getTerminated().getReason() : null)
                                .waitingMessage(status != null && status.getState().getWaiting() != null
                                        ? status.getState().getWaiting().getMessage() : null)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error on extractContainers, ", e);
            throw e;
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