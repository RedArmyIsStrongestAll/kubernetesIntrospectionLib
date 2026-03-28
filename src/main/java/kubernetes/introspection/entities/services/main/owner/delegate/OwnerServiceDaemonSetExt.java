package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public class OwnerServiceDaemonSetExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceDaemonSetExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.DAEMONSET;

    public OwnerServiceDaemonSetExt(KubernetesClient kubernetesClient, String namespace) {
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
        return List.of(ResourcePermissionEnum.DAEMONSETS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching DaemonSet owner: {}", SERVICE_NAME, ownerRef.getName());

        DaemonSet daemonSet = kubernetesClient.apps()
                .daemonSets()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (daemonSet == null) {
            log.error("{}: DaemonSet not found: {}", SERVICE_NAME, ownerRef.getName());
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }

        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(OwnerTypeEnum.DAEMONSET)
                .name(daemonSet.getMetadata().getName())
                .exists(true)
                .selector(daemonSet.getSpec().getSelector().getMatchLabels())
                .desiredReplicas(daemonSet.getStatus().getDesiredNumberScheduled())
                .availableReplicas(daemonSet.getStatus().getNumberReady())
                .build();

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.DAEMONSET, daemonSet);
    }
}
