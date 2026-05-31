package kubernetes.introspection.useCases.main.owner.reference;

import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.pod.PodInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_REFERENCE_NOT_FOUND;

@Slf4j
public class OwnerReferenceService {

    public OwnerReferenceService() {
    }

    public OwnerReferenceInfo getPodOwner(PodInfo podInfo) {
        log.info("Start getPodOwner");
        try {
            List<OwnerReferenceInfo> ownerRefs = podInfo.getOwnerReferences();
            if (ownerRefs == null || ownerRefs.isEmpty()) {
                log.info("Owner not found for pod: {}", podInfo.getName());
                return OwnerReferenceInfo.builder().kind(null).build();
            }

            return ownerRefs.stream()
                    .filter(ref -> Boolean.TRUE.equals(ref.getController()))
                    .findFirst()
                    .orElse(ownerRefs.get(0));
        } catch (Exception e) {
            log.error("Error getPodOwner: ", e);
            throw new KubernetesException(OWNER_REFERENCE_NOT_FOUND);
        }
    }
}
