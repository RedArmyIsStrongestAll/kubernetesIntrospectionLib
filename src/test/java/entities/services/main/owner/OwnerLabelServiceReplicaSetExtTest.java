package entities.services.main.owner;

import engine.owner.OwnerAnalyzerReplicaSet;
import entities.services.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceReplicaSetExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceReplicaSetExtTest extends OwnerServiceTestAbstract {
    private static final String REPLICASET_NAME = "test-replicaset";

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
    void getOwnerWithPermissionValidReplicaSetOwner() throws Exception {
        OwnerAnalyzerReplicaSet ownerEngine = (OwnerAnalyzerReplicaSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-replicaset-owner.yaml",
                OwnerAnalyzerReplicaSet.class
        );
        setupMockServerWithValidReplicaSet(ownerEngine, REPLICASET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICASETS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.REPLICASET, ownerDto.getOwnerInfo().getType());
        assertEquals(REPLICASET_NAME, ownerDto.getOwnerInfo().getName());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionReplicaSet() throws Exception {
        OwnerAnalyzerReplicaSet ownerEngine = (OwnerAnalyzerReplicaSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-replicaset-owner.yaml",
                OwnerAnalyzerReplicaSet.class
        );
        setupMockServerWithValidReplicaSet(ownerEngine, REPLICASET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICASETS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoPermissionReplicaSetByRbac() throws Exception {
        OwnerAnalyzerReplicaSet ownerEngine = (OwnerAnalyzerReplicaSet) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-replicaset-owner.yaml",
                OwnerAnalyzerReplicaSet.class
        );
        setupMockServerWithValidReplicaSet(ownerEngine, REPLICASET_NAME);

        OwnerReference ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerReplicaSet() throws Exception {
        OwnerAnalyzerReplicaSet ownerEngine = (OwnerAnalyzerReplicaSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-replicaset-owner.yaml",
                OwnerAnalyzerReplicaSet.class
        );
        setupMockServerWithError(); // Используется в других тестах

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICASETS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}