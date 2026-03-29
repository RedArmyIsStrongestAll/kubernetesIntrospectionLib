package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
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
public class OwnerServiceStatefulSetExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceStatefulSetExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.STATEFULSET;

    public OwnerServiceStatefulSetExt(KubernetesClient kubernetesClient, String namespace) {
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
        return List.of(ResourcePermissionEnum.STATEFULSETS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching StatefulSet owner: {}", SERVICE_NAME, ownerRef.getName());

        StatefulSet statefulSet = kubernetesClient.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (statefulSet == null) {
            log.error("{}: StatefulSet not found: {}", SERVICE_NAME, ownerRef.getName());
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }

        OwnerInfo ownerInfo = createOwnerInfo(statefulSet);

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.STATEFULSET, statefulSet);
    }

    private OwnerInfo createOwnerInfo(StatefulSet statefulSet) {
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.STATEFULSET)
                .name(
                        Optional.ofNullable(statefulSet.getMetadata())
                                .map(ObjectMeta::getName)
                                .orElse(null)
                )
                .exists(true)
                .selector(
                        Optional.ofNullable(statefulSet.getSpec())
                                .map(StatefulSetSpec::getSelector)
                                .map(LabelSelector::getMatchLabels)
                                .orElse(Collections.emptyMap())
                )
                .desiredReplicas(
                        Optional.ofNullable(statefulSet.getSpec())
                                .map(StatefulSetSpec::getReplicas)
                                .orElse(null)
                )
                .availableReplicas(
                        Optional.ofNullable(statefulSet.getStatus())
                                .map(StatefulSetStatus::getReadyReplicas)
                                .orElse(null)
                )
                .jobStatus(null)
                .lastSuccessfulTime(null)
                .build();
    }
}
