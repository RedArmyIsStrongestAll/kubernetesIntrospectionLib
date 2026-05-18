package kubernetes.introspection.useCases.utils;

import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public final class PermissionServiceUtil {

    public static void checkPermission(
            PermissionInfo permissionInfo,
            Supplier<List<ResourcePermissionEnum>> getPermissionResource)
            throws KubernetesException {

        log.info("Start checkPermission");

        List<ResourcePermissionEnum> resourcePermissionList = getPermissionResource.get();
        if (resourcePermissionList.isEmpty()) {
            return;
        }

        if (permissionInfo == null || permissionInfo.getPermissions() == null) {
            log.error("Error start: no permission info");
            throw new KubernetesException(ErrorCodeEnum.FORBIDDEN);
        }

        log.info("Resource permissions: {}", resourcePermissionList.stream()
                .map(ResourcePermissionEnum::getStringValue)
                .collect(Collectors.joining(", ")));
        log.info("App permissions: {}", permissionInfo.getPermissions().stream()
                .map(p -> p.getResource().getStringValue())
                .collect(Collectors.joining(", ")));

        boolean hasPermission = resourcePermissionList.stream().allMatch(requiredPerm ->
                permissionInfo.getPermissions().stream()
                        .anyMatch(appPerm -> appPerm.isAllowed() &&
                                appPerm.getResource().getResource().equals(requiredPerm.getResource()) &&
                                appPerm.getResource().getVerb().equals(requiredPerm.getVerb()))
        );
        if (!hasPermission) {
            log.error("No permission: forbidden");
            throw new KubernetesException(ErrorCodeEnum.FORBIDDEN);
        }
    }
}
