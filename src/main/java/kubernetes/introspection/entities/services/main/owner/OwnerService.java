package kubernetes.introspection.entities.services.main.owner;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.permision.PermissionService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_NOT_FOUND;
import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public abstract class OwnerService {

    protected final KubernetesClient kubernetesClient;
    protected final String namespace;

    public OwnerService(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
    }

    public OwnerService.OwnerDto getOwnerWithPermission(OwnerReference ownerRef, PermissionInfo permissionInfo) {
        log.info("Start getOwnerWithPermission");

        try {
            PermissionService.checkPermission(permissionInfo, this::getPermissionResource);

            return getOwner(ownerRef);
        } catch (Exception e) {
            log.error("Error getOwnerWithPermission: ", e);
            throw new KubernetesException(OWNER_NOT_FOUND);
        }
    }

    public OwnerService.OwnerDto getOwner(OwnerReference ownerRef) {
        log.info("Starting getOwner in {}", getNameClassExt());

        if (ownerRef.getKind() == null && getKindOwnerType().getOriginalName() == null ||
                ownerRef.getKind().equals(getKindOwnerType().getOriginalName())) {
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

    protected abstract OwnerDto getOwnerDto(OwnerReference ownerRef);

    @AllArgsConstructor
    @Getter
    public static class OwnerDto {
        private OwnerInfo ownerInfo;
        private OwnerTypeEnum k8sType;
        private HasMetadata k8sObject;
    }

}

