package kubernetes.introspection.entities.services.init;


import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.enviroment.CollectionError;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermission;
import kubernetes.introspection.entities.models.dto.permision.SsarRequestDto;
import kubernetes.introspection.entities.models.exceptions.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Проверяет права доступа к ресурсам Kubernetes, необходимым для интроспекции
 * В коснтруктор требуется KubernetesClient
 */
@Slf4j
public class InitPermissionsService {

    private final KubernetesClient kubernetesClient;

    public InitPermissionsService(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }


    /**
     * Проверяет права доступа к ресурсам Kubernetes, необходимым для интроспекции
     *
     * @return DTO с результатами проверки прав
     */
    public PermissionInfo checkPermissions(String currentNamespace) {
        try {
            log.info("Starting permission check for namespace: {}", currentNamespace);
            List<PermissionInfo.PermissionInfoDto> permissions = new ArrayList<>();

            permissions.addAll(checkPodResources(currentNamespace));

            permissions.addAll(checkOwnerResources(currentNamespace));

            permissions.addAll(checkServiceResources(currentNamespace));

            permissions.addAll(checkConfigResources(currentNamespace));

            boolean allAllowed = permissions.stream()
                    .allMatch(PermissionInfo.PermissionInfoDto::isAllowed);

            log.info("Success permission check with result: {}", allAllowed);
            return new PermissionInfo(allAllowed, permissions);

        } catch (Exception e) {
            log.info("Fall permission check: ", e);
            return new PermissionInfo(false, Collections.emptyList());
        }
    }

    /**
     * Парсирует ответ checkPermissions метода в список CollectionError.
     *
     * <p> Ресур имеет измененый вид от ресурса k8s: pods/get@namespace -> pods/get </p>
     *
     * @return List<CollectionError> с результатами проверки прав
     */
    public List<CollectionError> convertToCollectionErrors(PermissionInfo permissionInfo, String namespace) {
        log.info("Start convertToCollectionErrors");
        return permissionInfo.getPermissions().stream()
                .filter(p -> !p.isAllowed())
                .map(p -> {
                    String resourceType = extractResourceType(p.getResource());
                    return CollectionError.builder()
                            .resourceType(resourceType)
                            .resourceName("unknown")
                            .namespace(namespace)
                            .errorCode(ErrorCode.FORBIDDEN)
                            .message(ErrorCode.FORBIDDEN.getMessage())
                            .timestamp(Instant.now().toString())
                            .build();
                })
                .toList();
    }


    /**
     * Проверка для Pods
     */
    private List<PermissionInfo.PermissionInfoDto> checkPodResources(String namespace) {
        log.info("Checking pods resources for namespace: {}", namespace);
        List<SsarRequestDto> requests = ResourcePermission.getPodPermissions().stream()
                .map(permission -> permission.toSsarRequest(namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }

    /**
     * Проверка для Owners
     */
    private List<PermissionInfo.PermissionInfoDto> checkOwnerResources(String namespace) {
        log.info("Checking owner resources for namespace: {}", namespace);
        List<SsarRequestDto> requests = ResourcePermission.getOwnerPermissions().stream()
                .map(permission -> permission.toSsarRequest(namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }

    /**
     * Проверка для Services
     */
    private List<PermissionInfo.PermissionInfoDto> checkServiceResources(String namespace) {
        log.info("Checking service resources for namespace: {}", namespace);
        List<SsarRequestDto> requests = ResourcePermission.getServicePermissions().stream()
                .map(permission -> permission.toSsarRequest(namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }

    /**
     * Проверка для Resources
     */
    private List<PermissionInfo.PermissionInfoDto> checkConfigResources(String namespace) {
        log.info("Checking config resources for namespace: {}", namespace);
        List<SsarRequestDto> requests = ResourcePermission.getConfigPermissions().stream()
                .map(permission -> permission.toSsarRequest(namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }


    private List<PermissionInfo.PermissionInfoDto> executeAccessReviews(List<SsarRequestDto> requests) {
        return requests.parallelStream()
                .map(this::executeSingleAccessReview)
                .collect(Collectors.toList());
    }

    private PermissionInfo.PermissionInfoDto executeSingleAccessReview(SsarRequestDto request) {
        try {
            SelfSubjectAccessReview review = new SelfSubjectAccessReviewBuilder()
                    .withNewSpec()
                    .withNewResourceAttributes()
                    .withResource(request.getResource())
                    .withVerb(request.getVerb())
                    .withNamespace(request.getNamespace())
                    .endResourceAttributes()
                    .endSpec()
                    .build();

            SelfSubjectAccessReview response = kubernetesClient
                    .resource(review)
                    .create();

            boolean allowed = response.getStatus() != null && Boolean.TRUE.equals(response.getStatus().getAllowed());

            String permissionKey = String.format("%s/%s", request.getResource(), request.getVerb());
            if (request.getNamespace() != null && !request.getNamespace().isEmpty()) {
                permissionKey += "@" + request.getNamespace();
            }

            return new PermissionInfo.PermissionInfoDto(permissionKey, allowed);

        } catch (Exception e) {
            // Логирование если есть
            return new PermissionInfo.PermissionInfoDto(request.getResource() + "/" + request.getVerb(), false);
        }
    }

    /**
     * Конвертация ресурса k8s в русурс CollectionError
     * <p>
     * pods/get@namespace -> pods/get
     * </p>
     */
    private String extractResourceType(String resourceWithNamespace) {
        if (resourceWithNamespace == null) return "unknown";
        int atIndex = resourceWithNamespace.indexOf('@');
        if (atIndex == -1) return resourceWithNamespace;
        return resourceWithNamespace.substring(0, atIndex);
    }

}
