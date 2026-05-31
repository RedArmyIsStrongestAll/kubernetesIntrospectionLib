package kubernetes.introspection.useCases.main.pod;

import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.BROKEN_NAME_IN_POD;
import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.POD_NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
public abstract class CurrentPodService {

    protected final KubernetesPodPort podPort;
    protected String podName;
    protected final String namespace;


    public CurrentPodDto getCurrentPodWithCheckPermissions(PermissionInfo permissionInfo) {
        log.info("Start getCurrentPodInfoWithCheckPermissions in {}", getNameClassExt());
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, this::getPermissionResource);
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

            PodInfo podInfo = getPodInfo();
            if (podInfo == null) {
                log.error(POD_NOT_FOUND.getMessage());
                throw new Exception();
            }
            log.info("{}: current pod was found", getNameClassExt());

            return new CurrentPodDto(podInfo);

        } catch (Exception e) {
            log.error("Error getCurrentPod in {}, ", getNameClassExt(), e);
            log.error(POD_NOT_FOUND.getMessage());
            throw new KubernetesException(POD_NOT_FOUND);
        }
    }


    protected abstract List<ResourcePermissionEnum> getPermissionResource();

    protected abstract String getNameClassExt();

    protected abstract String getPodName() throws Exception;

    protected abstract PodInfo getPodInfo() throws Exception;


    @AllArgsConstructor
    @Getter
    public static class CurrentPodDto {
        private PodInfo podInfo;
    }
}
