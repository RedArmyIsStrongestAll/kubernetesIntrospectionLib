package kubernetes.introspection.adapters.kubernetes;

import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReview;
import io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.ports.KubernetesPermissionPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Fabric8PermissionAdapter implements KubernetesPermissionPort {

    private final KubernetesClient client;

    public Fabric8PermissionAdapter(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public PermissionInfo.PermissionInfoDto checkSinglePermission(ResourcePermissionEnum permission, String namespace) {
        try {
            SelfSubjectAccessReview review = new SelfSubjectAccessReviewBuilder()
                    .withNewSpec()
                    .withNewResourceAttributes()
                    .withResource(permission.getResource())
                    .withVerb(permission.getVerb())
                    .withNamespace(namespace)
                    .endResourceAttributes()
                    .endSpec()
                    .build();

            SelfSubjectAccessReview response = client.resource(review).create();

            boolean allowed = response.getStatus() != null && Boolean.TRUE.equals(response.getStatus().getAllowed());

            ResourcePermissionEnum resourcePermissionEnum = ResourcePermissionEnum.findByResourceAndVerb(
                    permission.getResource(),
                    permission.getVerb());

            return new PermissionInfo.PermissionInfoDto(resourcePermissionEnum, allowed);

        } catch (Exception e) {
            log.error("Error executing kubernetes access review for permission: {}", permission);
            return new PermissionInfo.PermissionInfoDto(permission, false);
        }
    }
}
