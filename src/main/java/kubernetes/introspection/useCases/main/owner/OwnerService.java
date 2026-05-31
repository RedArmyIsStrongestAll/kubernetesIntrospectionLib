package kubernetes.introspection.useCases.main.owner;

import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerInfo;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.ports.KubernetesOwnerPort;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_NOT_FOUND;
import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public abstract class OwnerService {

    protected final KubernetesOwnerPort ownerPort;
    protected final String namespace;

    public OwnerService(KubernetesOwnerPort ownerPort, String namespace) {
        this.ownerPort = ownerPort;
        this.namespace = namespace;
    }


    public OwnerDto getOwnerWithPermission(OwnerReferenceInfo ownerRef, PermissionInfo permissionInfo) {
        log.info("Start getOwnerWithPermission");
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, this::getPermissionResource);
            return getOwner(ownerRef);
        } catch (Exception e) {
            log.error("Error getOwnerWithPermission: ", e);
            throw new KubernetesException(OWNER_NOT_FOUND);
        }
    }

    public OwnerDto getOwner(OwnerReferenceInfo ownerRef) {
        log.info("Starting getOwner in {}", getNameClassExt());

        if ((ownerRef.getKind() == null && getKindOwnerType().getOriginalName() == null) ||
                (ownerRef.getKind() != null && ownerRef.getKind().equals(getKindOwnerType().getOriginalName()))) {
            log.info("Switch getOwner in {}", getNameClassExt());
            try {
                return getOwnerDto(ownerRef);
            } catch (Exception e) {
                log.error("Error getOwner: ", e);
                log.error(String.format(OWNER_REALIZED_NOT_FOUND.getMessage(), getKindOwnerType().getOriginalName()));
                throw new KubernetesException(OWNER_NOT_FOUND);
            }
        }

        log.info("Kind type is not type {}", getNameClassExt());
        return null;
    }


    protected abstract List<ResourcePermissionEnum> getPermissionResource();

    protected abstract String getNameClassExt();

    protected abstract OwnerTypeEnum getKindOwnerType();

    protected abstract OwnerDto getOwnerDto(OwnerReferenceInfo ownerRef);


    @AllArgsConstructor
    @Getter
    public static class OwnerDto {
        private OwnerInfo ownerInfo;
        private OwnerTypeEnum k8sType;
    }
}
