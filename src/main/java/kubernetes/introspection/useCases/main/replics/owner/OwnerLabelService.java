package kubernetes.introspection.useCases.main.replics.owner;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_NOT_FOUND;


@Slf4j
@Getter
public abstract class OwnerLabelService {
    private final String nameClassImpl;
    private final OwnerTypeEnum kindOwnerType;

    public OwnerLabelService(String nameClassImpl, OwnerTypeEnum kindOwnerType) {
        this.nameClassImpl = nameClassImpl;
        this.kindOwnerType = kindOwnerType;
    }


    public LabelSelector extractLabelSelectorWithPermission(HasMetadata hasMetadata, PermissionInfo permissionInfo) {
        log.info("Start extractLabelSelectorWithPermission");
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, this::getPermissionResource);

            return extractLabelSelector(hasMetadata);
        } catch (Exception e) {
            log.error("Error extractLabelSelectorWithPermission: ", e);
            throw new KubernetesException(OWNER_NOT_FOUND);
        }
    }

    public LabelSelector extractLabelSelector(HasMetadata hasMetadata) throws Exception {
        log.info("Start {} extractLabelSelector", nameClassImpl);
        try {
            if (!isValidType(hasMetadata)) {
                log.warn("Invalid type for {}: {}", nameClassImpl, hasMetadata.getKind());
                return null;
            }

            LabelSelector selector = doExtractLabelSelector(hasMetadata);
            log.info("Success {} extractLabelSelector", nameClassImpl);
            return selector;
        } catch (Exception e) {
            log.error("{}: error in extractLabelSelector: {}", nameClassImpl, e.getMessage());
            throw e;
        }
    }

    private boolean isValidType(HasMetadata hasMetadata) {
        return hasMetadata.getKind().equalsIgnoreCase(kindOwnerType.getOriginalName());
    }


    protected abstract LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) throws Exception;

    protected abstract List<ResourcePermissionEnum> getPermissionResource();

}