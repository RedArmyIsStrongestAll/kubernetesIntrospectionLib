package kubernetes.introspection.entities.services.main.replics.owner;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_NOT_FOUND;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class OwnerLabelCallChainService {

    private List<OwnerLabelService> callServiceList;

    public boolean addCallService(OwnerLabelService callService) {
        return callServiceList.add(callService);
    }

    public LabelSelector getSelectorWithPermission(OwnerTypeEnum ownerType,
                                                   HasMetadata hasMetadata,
                                                   PermissionInfo permissionInfo) {
        log.info("Starting getSelectorWithPermission for resource");
        if (Objects.isNull(hasMetadata)) {
            log.warn("Provided HasMetadata object is null.");
            throw new KubernetesException(OWNER_NOT_FOUND);
        }

        log.info("Detected owner type: {}", ownerType);
        for (OwnerLabelService service : callServiceList) {
            try {
                if (service.getKindOwnerType() == ownerType) {
                    log.info("Using {} to extract LabelSelector", service.getNameClassImpl());
                    return service.extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
                }
            } catch (Exception e) {
                log.error("Service {} failed to extract LabelSelector: {}", service.getNameClassImpl(), e.getMessage(), e);
                throw new KubernetesException(OWNER_NOT_FOUND);
            }
        }

        log.warn("No service found to extract LabelSelector for type: {}", ownerType);
        throw new KubernetesException(OWNER_NOT_FOUND);
    }

    public LabelSelector getSelector(OwnerTypeEnum ownerType,
                                     HasMetadata hasMetadata) throws Exception {
        log.info("Starting getSelector for resource");
        if (Objects.isNull(hasMetadata)) {
            log.warn("Provided HasMetadata object is null.");
            throw new KubernetesException(OWNER_NOT_FOUND);
        }

        log.info("Detected owner type: {}", ownerType);
        for (OwnerLabelService service : callServiceList) {
            try {
                if (service.getKindOwnerType() == ownerType) {
                    log.info("Using {} to extract LabelSelector", service.getNameClassImpl());
                    return service.extractLabelSelector(hasMetadata);
                }
            } catch (Exception e) {
                log.error("Service {} failed to extract LabelSelector: {}", service.getNameClassImpl(), e.getMessage(), e);
                throw new KubernetesException(OWNER_NOT_FOUND);
            }
        }

        log.warn("No service found to extract LabelSelector for type: {}", ownerType);
        throw new KubernetesException(OWNER_NOT_FOUND);
    }

}