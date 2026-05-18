package usesCases.main.owner;

import engine.owners.OwnerAnalyzerDeployment;
import usesCases.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceDeploymentExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceDeploymentExtTest extends OwnerServiceTestAbstract {
    private static final String DEPLOYMENT_NAME = "test-deployment";

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void getOwnerWithPermissionValidDeploymentOwner() throws Exception {
        OwnerAnalyzerDeployment ownerEngine = (OwnerAnalyzerDeployment) getOwnerAnalyzer("rbac/test-rbac.yaml",
                "owner/pod-with-deployment-owner.yaml", OwnerAnalyzerDeployment.class);

        setupMockServerWithValidOwnerDeployment(ownerEngine, DEPLOYMENT_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DEPLOYMENTS_GET, true)));

        OwnerReference ownerRef = getOwnerReference(DEPLOYMENT_NAME, OwnerTypeEnum.DEPLOYMENT);
        OwnerService ownerService = new OwnerServiceDeploymentExt(client, NAMESPACE);

        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.DEPLOYMENT, ownerDto.getOwnerInfo().getType());
        assertEquals(DEPLOYMENT_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(3, ownerDto.getOwnerInfo().getDesiredReplicas());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoValidNoPermissionDeploymentOwner() throws Exception {
        OwnerAnalyzerDeployment ownerEngine = (OwnerAnalyzerDeployment) getOwnerAnalyzer("rbac/test-rbac.yaml",
                "owner/pod-with-deployment-owner.yaml", OwnerAnalyzerDeployment.class);
        setupMockServerWithValidOwnerDeployment(ownerEngine, DEPLOYMENT_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DEPLOYMENTS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(DEPLOYMENT_NAME, OwnerTypeEnum.DEPLOYMENT);
        OwnerService ownerService = new OwnerServiceDeploymentExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoValidNoPermissionDeploymentOwner() throws Exception {
        OwnerAnalyzerDeployment ownerEngine = (OwnerAnalyzerDeployment) getOwnerAnalyzer("rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-deployment-owner.yaml", OwnerAnalyzerDeployment.class);
        setupMockServerWithValidOwnerDeployment(ownerEngine, DEPLOYMENT_NAME);

        OwnerReference ownerRef = getOwnerReference(DEPLOYMENT_NAME, OwnerTypeEnum.DEPLOYMENT);
        OwnerService ownerService = new OwnerServiceDeploymentExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoValidNoKubernetesAnswerDeploymentOwner() throws Exception {
        OwnerAnalyzerDeployment ownerEngine = (OwnerAnalyzerDeployment) getOwnerAnalyzer("rbac/test-rbac.yaml",
                "owner/pod-with-deployment-owner.yaml", OwnerAnalyzerDeployment.class);
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DEPLOYMENTS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(DEPLOYMENT_NAME, OwnerTypeEnum.DEPLOYMENT);
        OwnerService ownerService = new OwnerServiceDeploymentExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}
