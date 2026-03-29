package kubernetes.introspection.entities.services.main.replics.owner;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public abstract class OwnerLabelService {
    private final String nameClassImpl;
    private final OwnerTypeEnum kindOwnerType;

    public OwnerLabelService(String nameClassImpl, OwnerTypeEnum kindOwnerType) {
        this.nameClassImpl = nameClassImpl;
        this.kindOwnerType = kindOwnerType;
    }


    public final LabelSelector extractLabelSelector(HasMetadata hasMetadata) throws Exception {
        try {
            log.info("Start {} extractLabelSelector", nameClassImpl);
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


    protected boolean isValidType(HasMetadata hasMetadata) {
        return hasMetadata.getKind().equalsIgnoreCase(kindOwnerType.getOriginalName());
    }

    protected abstract LabelSelector doExtractLabelSelector(HasMetadata hasMetadata) throws Exception;


}