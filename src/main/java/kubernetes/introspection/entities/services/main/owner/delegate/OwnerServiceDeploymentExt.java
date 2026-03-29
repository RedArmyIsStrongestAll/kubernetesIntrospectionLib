package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public class OwnerServiceDeploymentExt extends OwnerService {

    public OwnerServiceDeploymentExt(KubernetesClient kubernetesClient, String namespace) {
        super(kubernetesClient, namespace);
    }

    private static final String OWNER_SERVICE_NAME = "OwnerServiceDeploymentExt";
    private static final OwnerTypeEnum OWNER_SERVICE_TYPE = OwnerTypeEnum.DEPLOYMENT;

    @Override
    protected String getNameClassExt() {
        return OWNER_SERVICE_NAME;
    }

    @Override
    protected OwnerTypeEnum getKindOwnerType() {
        return OWNER_SERVICE_TYPE;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.DEPLOYMENTS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching Deployment owner: {}", OWNER_SERVICE_NAME, ownerRef.getName());

        Deployment deployment = kubernetesClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (deployment == null) {
            log.error("{}: error fetching Deployment owner: return null", OWNER_SERVICE_NAME);
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }
        log.info("{}: fetching Deployment owner: find", OWNER_SERVICE_NAME);

        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(OwnerTypeEnum.DEPLOYMENT)
                .name(deployment.getMetadata().getName())
                .exists(true)
                .selector(deployment.getSpec().getSelector().getMatchLabels())
                .desiredReplicas(deployment.getSpec().getReplicas())
                .availableReplicas(deployment.getStatus() != null ? deployment.getStatus().getAvailableReplicas() : null)
                .build();
        log.info("{}: created info: {}", OWNER_SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.DEPLOYMENT, deployment);
    }
}
