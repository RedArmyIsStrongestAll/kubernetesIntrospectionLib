package kubernetes.introspection.useCases.ports;

import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;

public interface KubernetesPermissionPort {
    PermissionInfo.PermissionInfoDto checkSinglePermission(ResourcePermissionEnum permission, String namespace);
}
