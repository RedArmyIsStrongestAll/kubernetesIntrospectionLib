package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public class OwnerServiceReplicaSetExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceReplicaSetExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.REPLICASET;

    public OwnerServiceReplicaSetExt(KubernetesClient kubernetesClient, String namespace) {
        super(kubernetesClient, namespace);
    }

    @Override
    protected String getNameClassExt() {
        return SERVICE_NAME;
    }

    @Override
    protected OwnerTypeEnum getKindOwnerType() {
        return OWNER_TYPE;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.REPLICASETS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching ReplicaSet owner: {}", SERVICE_NAME, ownerRef.getName());

        ReplicaSet replicaSet = kubernetesClient.apps()
                .replicaSets()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (replicaSet == null) {
            log.error("{}: ReplicaSet not found: {}", SERVICE_NAME, ownerRef.getName());
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }

        OwnerInfo ownerInfo = createOwnerInfo(replicaSet);

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.REPLICASET, replicaSet);
    }

    private OwnerInfo createOwnerInfo(ReplicaSet replicaSet) {
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.REPLICASET)
                .name(
                        Optional.ofNullable(replicaSet.getMetadata())
                                .map(ObjectMeta::getName)
                                .orElse(null)
                )
                .exists(true)
                .selector(
                        Optional.ofNullable(replicaSet.getSpec())
                                .map(ReplicaSetSpec::getSelector)
                                .map(LabelSelector::getMatchLabels)
                                .orElse(Collections.emptyMap())
                )
                .desiredReplicas(
                        Optional.ofNullable(replicaSet.getSpec())
                                .map(ReplicaSetSpec::getReplicas)
                                .orElse(null)
                )
                .availableReplicas(
                        Optional.ofNullable(replicaSet.getStatus())
                                .map(ReplicaSetStatus::getReadyReplicas)
                                .orElse(null)
                )
                .jobStatus(null)
                .lastSuccessfulTime(null)
                .build();
    }
}
