package kubernetes.introspection.entities.services.init;


import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.enviroment.CollectionError;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.permision.SsarKubernetesRequestDto;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
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
     * Проверка для Pods
     */
    private List<PermissionInfo.PermissionInfoDto> checkPodResources(String namespace) {
        log.info("Checking pods resources for namespace: {}", namespace);
        List<SsarKubernetesRequestDto> requests = ResourcePermissionEnum.getPodPermissions().stream()
                .map(permission -> new SsarKubernetesRequestDto(permission, namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }

    /**
     * Проверка для Owners
     */
    private List<PermissionInfo.PermissionInfoDto> checkOwnerResources(String namespace) {
        log.info("Checking owner resources for namespace: {}", namespace);
        List<SsarKubernetesRequestDto> requests = ResourcePermissionEnum.getOwnerPermissions().stream()
                .map(permission -> new SsarKubernetesRequestDto(permission, namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }

    /**
     * Проверка для Services
     */
    private List<PermissionInfo.PermissionInfoDto> checkServiceResources(String namespace) {
        log.info("Checking service resources for namespace: {}", namespace);
        List<SsarKubernetesRequestDto> requests = ResourcePermissionEnum.getServicePermissions().stream()
                .map(permission -> new SsarKubernetesRequestDto(permission, namespace))
                .collect(Collectors.toList());
        return executeAccessReviews(requests);
    }

    /**
     * Проверка для Resources
     */
    private List<PermissionInfo.PermissionInfoDto> checkConfigResources(String namespace) {
        log.info("Checking config resources for namespace: {}", namespace);
        List<SsarKubernetesRequestDto> requests = ResourcePermissionEnum.getConfigPermissions().stream()
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
        try {
            SelfSubjectAccessReview review = new SelfSubjectAccessReviewBuilder()
                    .withNewSpec()
                    .withNewResourceAttributes()
                    .withResource(request.getResourcePermissionEnum().getResource())
                    .withVerb(request.getResourcePermissionEnum().getVerb())
                    .withNamespace(request.getNamespace())
                    .endResourceAttributes()
                    .endSpec()
                    .build();

            SelfSubjectAccessReview response = kubernetesClient
                    .resource(review)
                    .create();

            boolean allowed = response.getStatus() != null && Boolean.TRUE.equals(response.getStatus().getAllowed());

            ResourcePermissionEnum resourcePermissionEnum = ResourcePermissionEnum.findByResourceAndVerb(
                    request.getResourcePermissionEnum().getResource(),
                    request.getResourcePermissionEnum().getVerb());

            return new PermissionInfo.PermissionInfoDto(resourcePermissionEnum, allowed);

        } catch (Exception e) {
            log.error("Error executing kubernetes access review for resource: {}", request);
            return new PermissionInfo.PermissionInfoDto(request.getResourcePermissionEnum(), false);
        }
    }


    /**
     * Парсирует ответ checkPermissions метода в список CollectionError.
     *
     * <p> Ресур имеет измененый вид от ресурса k8s: pods/get@namespace -> pods/get </p>
     *
     * @return List<CollectionError> с результатами проверки прав
     */
    public static List<CollectionError> convertToCollectionErrors(PermissionInfo permissionInfo, String namespace) {
        log.info("Start convertToCollectionErrors");
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
