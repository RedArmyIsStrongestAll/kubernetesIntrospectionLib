package entities.services.main.owner;

import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import engine.owner.OwnerAnalyzerDeployment;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceDeploymentExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static entities.services.utils.TestUtils.loadRbacYaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerServiceDeploymentExtTest {

    private static final String NAMESPACE = "test-namespace";
    private static final String POD_NAME = "test-pod";
    private static final String DEPLOYMENT_NAME = "test-deployment";

    private KubernetesMockServer mockServer;
    private KubernetesClient client;

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
    void getOwnerDtoWithValidDeploymentOwner() throws IOException {
        String yamlContent = loadRbacYaml("owner/pod-with-deployment-owner.yaml");

        PodAnalyzer podAnalyzer = new PodAnalyzer(yamlContent, new RbacAnalyzer(yamlContent));
        OwnerAnalyzerDeployment ownerEngine = new OwnerAnalyzerDeployment(yamlContent);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DEPLOYMENTS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DEPLOYMENTS_LIST, true)));

        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/deployments/" + DEPLOYMENT_NAME)
                .andReturn(200, ownerEngine.getOwner(DEPLOYMENT_NAME, NAMESPACE))
                .once();

        OwnerReference ownerRef = new OwnerReference();
        ownerRef.setKind(OwnerTypeEnum.DEPLOYMENT.getOriginalName());
        ownerRef.setName(DEPLOYMENT_NAME);

        OwnerService ownerService = new OwnerServiceDeploymentExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.DEPLOYMENT, ownerDto.getOwnerInfo().getType());
        assertEquals(DEPLOYMENT_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(3, ownerDto.getOwnerInfo().getDesiredReplicas());
        assertEquals(3, ownerDto.getOwnerInfo().getAvailableReplicas());
        assertNotNull(ownerDto.getK8sObject());
    }
}
