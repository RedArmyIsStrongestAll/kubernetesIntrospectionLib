package entities.services.main.owner;

import engine.owner.OwnerAnalyzerReplicationController;
import entities.services.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceReplicationControllerExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
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
                "rbac/test-rbac.yaml",
                "owner/pod-with-replication-controller-owner.yaml",
                OwnerAnalyzerReplicationController.class
        );
        setupMockServerWithValidReplicationController(ownerEngine, RC_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.REPLICATION_CONTROLLER, ownerDto.getOwnerInfo().getType());
        assertEquals(RC_NAME, ownerDto.getOwnerInfo().getName());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionReplicationController() throws Exception {
        OwnerAnalyzerReplicationController ownerEngine = (OwnerAnalyzerReplicationController) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-replication-controller-owner.yaml",
                OwnerAnalyzerReplicationController.class
        );
        setupMockServerWithValidReplicationController(ownerEngine, RC_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoPermissionReplicationControllerByRbac() throws Exception {
        OwnerAnalyzerReplicationController ownerEngine = (OwnerAnalyzerReplicationController) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-replication-controller-owner.yaml",
                OwnerAnalyzerReplicationController.class
        );
        setupMockServerWithValidReplicationController(ownerEngine, RC_NAME);

        OwnerReference ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerReplicationController() throws Exception {
        OwnerAnalyzerReplicationController ownerEngine = (OwnerAnalyzerReplicationController) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-replication-controller-owner.yaml",
                OwnerAnalyzerReplicationController.class
        );
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICATION_CONTROLLERS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(RC_NAME, OwnerTypeEnum.REPLICATION_CONTROLLER);
        OwnerServiceReplicationControllerExt ownerService = new OwnerServiceReplicationControllerExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}