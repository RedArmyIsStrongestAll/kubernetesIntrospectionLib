package kubernetes.introspection.useCases.init;


import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.permision.SsarKubernetesRequestDto;
import kubernetes.introspection.useCases.ports.KubernetesPermissionPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@AllArgsConstructor
public class InitPermissionsService {

    private final KubernetesPermissionPort permissionPort;


    public PermissionInfo checkPermissions(String currentNamespace) {
        try {
            log.info("Start permission check for namespace: {}", currentNamespace);
            List<PermissionInfo.PermissionInfoDto> permissions = new ArrayList<>();

            permissions.addAll(checkActualResources(currentNamespace));

            boolean allAllowed = permissions.stream()
                    .allMatch(PermissionInfo.PermissionInfoDto::isAllowed);

            log.info("Success permission check with result: {}", allAllowed);
            return new PermissionInfo(allAllowed, permissions);

        } catch (Exception e) {
            log.info("Fall permission check: ", e);
            log.info(ErrorCodeEnum.RBAC_NOT_FOUND.getMessage());
            throw new KubernetesException(ErrorCodeEnum.RBAC_NOT_FOUND);
        }
    }


    private List<PermissionInfo.PermissionInfoDto> checkActualResources(String namespace) {
        log.info("Checking pods resources for namespace: {}", namespace);
        List<SsarKubernetesRequestDto> requests = ResourcePermissionEnum.getActualPermissions().stream()
                .map(permission -> new SsarKubernetesRequestDto(permission, namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }


    private List<PermissionInfo.PermissionInfoDto> executeAccessReviews(List<SsarKubernetesRequestDto> requests) {
        log.info("Permission convert to PermissionInfo.PermissionInfoDto");
        return requests.parallelStream()
                .map(this::executeSingleAccessReview)
                .collect(Collectors.toList());
    }

    private PermissionInfo.PermissionInfoDto executeSingleAccessReview(SsarKubernetesRequestDto request) {
        return permissionPort.checkSinglePermission(request.getResourcePermissionEnum(), request.getNamespace());
    }
}
