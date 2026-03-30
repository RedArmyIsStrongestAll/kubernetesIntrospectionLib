package kubernetes.introspection.entities.services.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ReplicationController;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class OwnerLabelServiceReplicationControllerExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceReplicationControllerExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.REPLICATION_CONTROLLER;

    public OwnerLabelServiceReplicationControllerExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        ReplicationController rc = (ReplicationController) hasMetadata;
        Map<String, String> selector = rc.getSpec().getSelector();
        return new LabelSelector(null, selector);
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET);
    }

}
