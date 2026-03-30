package kubernetes.introspection.entities.services.utils;

import kubernetes.introspection.entities.models.enviroment.CollectionError;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;

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

    public static CollectionError convertToCollectionErrors(ErrorCodeEnum errorCodeEnum) {
        return CollectionError.builder()
                .resourceType("unknown")
                .resourceName("unknown")
                .namespace("unknown")
                .errorCodeEnum(errorCodeEnum)
                .message(errorCodeEnum.getMessage())
                .timestamp(Instant.now().toString())
                .build();
    }

    public static CollectionError convertToCollectionErrors(ErrorCodeEnum errorCodeEnum, String namespace) {
        return CollectionError.builder()
                .resourceType("unknown")
                .resourceName("unknown")
                .namespace(namespace)
                .errorCodeEnum(errorCodeEnum)
                .message(errorCodeEnum.getMessage())
                .timestamp(Instant.now().toString())
                .build();
    }

    public static CollectionError convertToCollectionErrors(ErrorCodeEnum errorCodeEnum, String namespace, ResourcePermissionEnum resourceName) {
        return CollectionError.builder()
                .resourceType(resourceName.name())
                .resourceName("unknown")
                .namespace(namespace)
                .errorCodeEnum(errorCodeEnum)
                .message(errorCodeEnum.getMessage())
                .timestamp(Instant.now().toString())
                .build();
    }

    public static CollectionError convertToCollectionErrors(Exception e) {
        return CollectionError.builder()
                .resourceType("unknown")
                .resourceName("unknown")
                .namespace("unknown")
                .errorCodeEnum(null)
                .message(e.getMessage())
                .timestamp(Instant.now().toString())
                .build();
    }


}
