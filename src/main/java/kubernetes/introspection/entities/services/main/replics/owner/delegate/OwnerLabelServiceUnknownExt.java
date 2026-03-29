package kubernetes.introspection.entities.services.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Обработка случая, когда тип владельца неизвестен.
 */
@Slf4j
public class OwnerLabelServiceUnknownExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceUnknownExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.UNKNOWN;

    public OwnerLabelServiceUnknownExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        log.warn("{}: label selector extraction not supported for UNKNOWN type", SERVICE_NAME);
        return null;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of();
    }
}