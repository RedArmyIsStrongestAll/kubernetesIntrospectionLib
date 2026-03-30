package kubernetes.introspection.entities.services.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class OwnerLabelServiceReplicaSetExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceReplicaSetExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.REPLICASET;

    public OwnerLabelServiceReplicaSetExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        ReplicaSet replicaSet = (ReplicaSet) hasMetadata;
        return replicaSet.getSpec().getSelector();
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.REPLICASETS_GET);
    }
}