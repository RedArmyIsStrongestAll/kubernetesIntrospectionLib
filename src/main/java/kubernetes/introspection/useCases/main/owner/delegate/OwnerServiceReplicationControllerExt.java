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
public class OwnerServiceReplicationControllerExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceReplicationControllerExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.REPLICATION_CONTROLLER;

    public OwnerServiceReplicationControllerExt(KubernetesOwnerPort ownerPort, String namespace) {
        super(ownerPort, namespace);
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
    public OwnerDto getOwnerDto(OwnerReferenceInfo ownerRef) {
        log.info("{}: fetching ReplicationController: {}", SERVICE_NAME, ownerRef.getName());
        OwnerInfo ownerInfo = ownerPort.getReplicationController(ownerRef.getName(), namespace);
        if (ownerInfo == null) throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        return new OwnerDto(ownerInfo, OWNER_TYPE);
    }
}
