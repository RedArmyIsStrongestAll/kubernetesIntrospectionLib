package kubernetes.introspection.useCases.main.owner.delegate;

import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerInfo;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.ports.KubernetesOwnerPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public class OwnerServiceDeploymentExt extends OwnerService {

    private static final String OWNER_SERVICE_NAME = "OwnerServiceDeploymentExt";
    private static final OwnerTypeEnum OWNER_SERVICE_TYPE = OwnerTypeEnum.DEPLOYMENT;

    public OwnerServiceDeploymentExt(KubernetesOwnerPort ownerPort, String namespace) {
        super(ownerPort, namespace);
    }

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
    public OwnerDto getOwnerDto(OwnerReferenceInfo ownerRef) {
        log.info("{}: fetching Deployment owner: {}", OWNER_SERVICE_NAME, ownerRef.getName());
        OwnerInfo ownerInfo = ownerPort.getDeployment(ownerRef.getName(), namespace);
        if (ownerInfo == null) {
            log.error("{}: Deployment not found", OWNER_SERVICE_NAME);
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }
        return new OwnerDto(ownerInfo, OwnerTypeEnum.DEPLOYMENT);
    }
}
