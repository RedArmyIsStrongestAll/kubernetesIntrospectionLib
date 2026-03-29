package kubernetes.introspection.entities.services.main.owner;

import io.fabric8.kubernetes.api.model.OwnerReference;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.POD_NOT_FOUND;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OwnerCallChainService {
    private List<OwnerService> callServiceList;

    public boolean addCallService(OwnerService callService) {
        return callServiceList.add(callService);
    }

    public OwnerService.OwnerDto getOwnerWithPermission(OwnerReference ownerRef, PermissionInfo permissionInfo) {
        log.info("Starting getOwnerWithPermission");
        for (OwnerService service : callServiceList) {
            try {
                log.info("Start service {}", service.getNameClassExt());

                OwnerService.OwnerDto owner = service.getOwnerWithPermission(ownerRef, permissionInfo);
                if (owner != null) {
                    return owner;
                }
            } catch (KubernetesException e) {
                log.warn("Service {} failed: {}", service.getNameClassExt(), e.getErrorCodeEnum().getName(), e);
            }
        }

        log.error("No service was able to get the current Owner information.");
        throw new KubernetesException(POD_NOT_FOUND);
    }

    public OwnerService.OwnerDto getOwner(OwnerReference ownerRef) {
        log.info("Starting getOwner");
        for (OwnerService service : callServiceList) {
            try {
                log.info("Start service {}", service.getNameClassExt());

                OwnerService.OwnerDto owner = service.getOwner(ownerRef);
                if (owner != null) {
                    return owner;
                }
            } catch (KubernetesException e) {
                log.warn("Service {} failed: {}", service.getNameClassExt(), e.getErrorCodeEnum().getName(), e);
            }
        }

        log.error("No service was able to get the current Owner information.");
        throw new KubernetesException(POD_NOT_FOUND);
    }

}
