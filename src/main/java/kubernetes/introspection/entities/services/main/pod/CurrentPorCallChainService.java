package kubernetes.introspection.entities.services.main.pod;

import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.POD_NOT_FOUND;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CurrentPorCallChainService {
    private List<CurrentPodService> callServiceList;

    public boolean addCallService(CurrentPodService callService) {
        return callServiceList.add(callService);
    }

    public CurrentPodService.CurrentPodDto getPodWithPermission(PermissionInfo permissionInfo) {
        log.info("Starting getPod");
        for (CurrentPodService service : callServiceList) {
            try {
                log.info("Start service {}", service.getNameClassExt());

                CurrentPodService.CurrentPodDto pod = service.getCurrentPodWithCheckPermissions(permissionInfo);
                if (pod != null) {
                    return pod;
                }
            } catch (KubernetesException e) {
                log.warn("Service {} failed: {}", service.getNameClassExt(), e.getErrorCodeEnum().getName(), e);
            }
        }

        log.error("No service was able to get the current Pod information.");
        throw new KubernetesException(POD_NOT_FOUND);
    }

    public CurrentPodService.CurrentPodDto getPod() {
        log.info("Starting getPod");
        for (CurrentPodService service : callServiceList) {
            try {
                log.info("Start service {}", service.getNameClassExt());

                CurrentPodService.CurrentPodDto pod = service.getCurrentPod();
                if (pod != null) {
                    return pod;
                }
            } catch (KubernetesException e) {
                log.warn("Service {} failed: {}", service.getNameClassExt(), e.getErrorCodeEnum().getName(), e);
            }
        }

        log.error("No service was able to get the current Pod information.");
        throw new KubernetesException(POD_NOT_FOUND);
    }

}
