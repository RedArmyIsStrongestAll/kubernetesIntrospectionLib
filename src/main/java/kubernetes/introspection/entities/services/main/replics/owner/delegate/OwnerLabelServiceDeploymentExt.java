package kubernetes.introspection.entities.services.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class OwnerLabelServiceDeploymentExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceDeploymentExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.DEPLOYMENT;

    public OwnerLabelServiceDeploymentExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        Deployment deployment = (Deployment) hasMetadata;
        return deployment.getSpec().getSelector();
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.DEPLOYMENTS_GET);
    }
}