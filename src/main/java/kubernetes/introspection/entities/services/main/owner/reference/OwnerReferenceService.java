package kubernetes.introspection.entities.services.main.owner.reference;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.permision.PermissionService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REFERENCE_NOT_FOUND;

@Slf4j
public class OwnerReferenceService {

    protected final String namespace;

    public OwnerReferenceService(String namespace) {
        this.namespace = namespace;
    }

    public OwnerReference getPodOwnerWithPermission(Pod pod, PermissionInfo permissionInfo) {
        log.info("Start getPodOwnerWithPermission");
        try {
            PermissionService.checkPermission(permissionInfo, this::getPermissionResource);

            return getPodOwner(pod);
        } catch (Exception e) {
            log.error("Error getPodOwnerWithPermission: ", e);
            throw new KubernetesException(OWNER_REFERENCE_NOT_FOUND);
        }
    }

    public OwnerReference getPodOwner(Pod pod) {
        log.info("Start getPodOwner");

        try {
            ObjectMeta metadata = pod.getMetadata();
            if (metadata == null || metadata.getOwnerReferences() == null || metadata.getOwnerReferences().isEmpty()) {
                log.info("Owner not found for pod: {}", pod.getMetadata().getName());
                log.info("Owner is pod");
                OwnerReference ownerReference = new OwnerReference();
                ownerReference.setKind(null);
                return ownerReference;
            }

            return metadata.getOwnerReferences().stream()
                    .filter(ref -> Boolean.TRUE.equals(ref.getController()))
                    .findFirst()
                    .orElse(metadata.getOwnerReferences().get(0));
        } catch (Exception e) {
            log.error("Error getPodOwner: ", e);
            throw new KubernetesException(OWNER_REFERENCE_NOT_FOUND);
        }
    }

    public List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.PODS_GET, ResourcePermissionEnum.PODS_LIST);
    }

}

