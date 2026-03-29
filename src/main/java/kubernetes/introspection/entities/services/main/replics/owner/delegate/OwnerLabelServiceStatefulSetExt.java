package kubernetes.introspection.entities.services.main.replics.owner.delegate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class OwnerLabelServiceStatefulSetExt extends OwnerLabelService {
    private static final String SERVICE_NAME = "OwnerLabelServiceStatefulSetExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.STATEFULSET;

    public OwnerLabelServiceStatefulSetExt() {
        super(SERVICE_NAME, OWNER_TYPE);
    }

    @Override
    protected LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) {
        StatefulSet sts = (StatefulSet) hasMetadata;
        return sts.getSpec().getSelector();
    }

}