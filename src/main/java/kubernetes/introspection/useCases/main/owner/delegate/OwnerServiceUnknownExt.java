package kubernetes.introspection.useCases.main.owner.delegate;

import kubernetes.introspection.entities.owner.OwnerInfo;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.ports.KubernetesOwnerPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OwnerServiceUnknownExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceUnknownExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.UNKNOWN;

    public OwnerServiceUnknownExt(KubernetesOwnerPort ownerPort, String namespace) {
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
        return List.of();
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReferenceInfo ownerRef) {
        log.info("{}: ownerRef is null or unknown type", SERVICE_NAME);
        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(OwnerTypeEnum.UNKNOWN)
                .name("Unknown")
                .exists(false)
                .build();
        return new OwnerDto(ownerInfo, OwnerTypeEnum.UNKNOWN);
    }
}
