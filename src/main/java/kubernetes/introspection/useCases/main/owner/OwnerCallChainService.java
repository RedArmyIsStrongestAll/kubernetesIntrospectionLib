package kubernetes.introspection.useCases.main.owner;

import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.permision.PermissionInfo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_NOT_FOUND;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OwnerCallChainService {
    private List<OwnerService> callServiceList;

    public boolean addCallService(OwnerService callService) {
        return callServiceList.add(callService);
    }

    public OwnerService.OwnerDto getOwnerWithPermission(OwnerReferenceInfo ownerRef, PermissionInfo permissionInfo) {
        log.info("Starting getOwnerWithPermission");
        for (OwnerService service : callServiceList) {
            try {
                log.info("Start service {}", service.getNameClassExt());
                OwnerService.OwnerDto owner = service.getOwnerWithPermission(ownerRef, permissionInfo);
                if (owner != null) return owner;
            } catch (KubernetesException e) {
                log.warn("Service {} failed: {}", service.getNameClassExt(), e.getErrorCodeEnum().getName(), e);
            }
        }
        log.error("No service was able to get the current Owner information.");
        throw new KubernetesException(OWNER_NOT_FOUND);
    }

    public OwnerService.OwnerDto getOwner(OwnerReferenceInfo ownerRef) {
        log.info("Starting getOwner");
        for (OwnerService service : callServiceList) {
            try {
                log.info("Start service {}", service.getNameClassExt());
                OwnerService.OwnerDto owner = service.getOwner(ownerRef);
                if (owner != null) return owner;
            } catch (KubernetesException e) {
                log.warn("Service {} failed: {}", service.getNameClassExt(), e.getErrorCodeEnum().getName(), e);
            }
        }
        log.error("No service was able to get the current Owner information.");
        throw new KubernetesException(OWNER_NOT_FOUND);
    }
}
