package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.ReplicationController;
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
public class OwnerServiceReplicationControllerExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceReplicationControllerExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.REPLICATION_CONTROLLER;

    public OwnerServiceReplicationControllerExt(KubernetesClient kubernetesClient, String namespace) {
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
        return List.of(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching ReplicationController owner: {}", SERVICE_NAME, ownerRef.getName());

        ReplicationController rc = kubernetesClient
                .replicationControllers()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (rc == null) {
            log.error("{}: ReplicationController not found: {}", SERVICE_NAME, ownerRef.getName());
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }

        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(OwnerTypeEnum.REPLICATION_CONTROLLER)
                .name(rc.getMetadata().getName())
                .exists(true)
                .selector(rc.getSpec().getSelector())
                .desiredReplicas(rc.getSpec().getReplicas())
                .availableReplicas(rc.getStatus().getReadyReplicas())
                .build();

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.REPLICATION_CONTROLLER, rc);
    }
}
