package kubernetes.introspection.useCases.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class OwnerLabelServiceJobExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceJobExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.JOB;

    public OwnerLabelServiceJobExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        Job job = (Job) hasMetadata;
        return job.getSpec().getSelector();
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.JOBS_GET);
    }

}