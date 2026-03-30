package kubernetes.introspection.entities.services.utils;

import kubernetes.introspection.entities.models.enviroment.CollectionError;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;

import java.time.Instant;
import java.util.List;

public class ConvertorToCollectionErrorUtil {
    public static List<CollectionError> convertToCollectionErrors(PermissionInfo permissionInfo, String namespace) {
        return permissionInfo.getPermissions().stream()
                .filter(p -> !p.isAllowed())
                .map(p -> {
                    String resourceType = (p.getResource() == null) ? "unknown" : p.getResource().getStringValue();

                    return CollectionError.builder()
                            .resourceType(resourceType)
                            .resourceName("unknown")
                            .namespace(namespace)
                            .errorCodeEnum(ErrorCodeEnum.FORBIDDEN)
                            .message(ErrorCodeEnum.FORBIDDEN.getMessage())
                            .timestamp(Instant.now().toString())
                            .build();
                })
                .toList();
    }

}
