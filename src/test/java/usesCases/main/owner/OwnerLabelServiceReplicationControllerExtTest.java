package usesCases.main.owner;

import engine.owners.OwnerAnalyzerReplicationController;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.kubernetes.Fabric8OwnerAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceReplicationControllerExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.owner.parent.OwnerServiceTestAbstract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceReplicationControllerExtTest extends OwnerServiceTestAbstract {
    private static final String RC_NAME = "test-replication-controller";

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
    void getOwnerWithPermissionValidReplicationControllerOwner() throws Exception {
        OwnerAnalyzerReplicationController ownerEngine = (OwnerAnalyzerReplicationController) getOwnerAnalyzer(
                "rbac/test-rbac.yaml", "owner/pod-with-replication-controller-owner.yaml", OwnerAnalyzerReplicationController.class);
        setupMockServerWithValidReplicationController(ownerEngine, RC_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.REPLICATION_CONTROLLER, ownerDto.getOwnerInfo().getType());
        assertEquals(RC_NAME, ownerDto.getOwnerInfo().getName());
    }

    @Test
    void getOwnerWithPermissionNoPermissionReplicationController() throws Exception {
        OwnerAnalyzerReplicationController ownerEngine = (OwnerAnalyzerReplicationController) getOwnerAnalyzer(
                "rbac/test-rbac.yaml", "owner/pod-with-replication-controller-owner.yaml", OwnerAnalyzerReplicationController.class);
        setupMockServerWithValidReplicationController(ownerEngine, RC_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET, false)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }

    @Test
    void getOwnerNoPermissionReplicationControllerByRbac() throws Exception {
        OwnerAnalyzerReplicationController ownerEngine = (OwnerAnalyzerReplicationController) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml", "owner/pod-with-replication-controller-owner.yaml", OwnerAnalyzerReplicationController.class);
        setupMockServerWithValidReplicationController(ownerEngine, RC_NAME);

        OwnerReferenceInfo ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> ownerService.getOwner(ownerRef));
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerReplicationController() throws Exception {
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }
}
