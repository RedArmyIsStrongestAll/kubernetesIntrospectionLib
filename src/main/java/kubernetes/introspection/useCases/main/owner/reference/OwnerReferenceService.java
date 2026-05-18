package kubernetes.introspection.useCases.main.owner.reference;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import lombok.extern.slf4j.Slf4j;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_REFERENCE_NOT_FOUND;

@Slf4j
public class OwnerReferenceService {

    public OwnerReferenceService() {
    }

    public OwnerReference getPodOwner(Pod pod) {
        log.info("Start getPodOwner");

        try {
            ObjectMeta metadata = pod.getMetadata();
            if (metadata == null || metadata.getOwnerReferences() == null || metadata.getOwnerReferences().isEmpty()) {
                log.info("Owner not found for pod: {}", pod.getMetadata().getName());
                log.info("Owner is pod");
                OwnerReference ownerReference = new OwnerReference();
                ownerReference.setKind(null);
                return ownerReference;
            }

            return metadata.getOwnerReferences().stream()
                    .filter(ref -> Boolean.TRUE.equals(ref.getController()))
                    .findFirst()
                    .orElse(metadata.getOwnerReferences().get(0));
        } catch (Exception e) {
            log.error("Error getPodOwner: ", e);
            throw new KubernetesException(OWNER_REFERENCE_NOT_FOUND);
        }
    }
}

