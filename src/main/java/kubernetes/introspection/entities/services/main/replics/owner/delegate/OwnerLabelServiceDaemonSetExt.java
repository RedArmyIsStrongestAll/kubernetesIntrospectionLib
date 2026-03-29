package kubernetes.introspection.entities.services.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class OwnerLabelServiceDaemonSetExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceDaemonSetExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.DAEMONSET;

    public OwnerLabelServiceDaemonSetExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        DaemonSet daemonSet = (DaemonSet) hasMetadata;
        return daemonSet.getSpec().getSelector();
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.DAEMONSETS_GET);
    }
}