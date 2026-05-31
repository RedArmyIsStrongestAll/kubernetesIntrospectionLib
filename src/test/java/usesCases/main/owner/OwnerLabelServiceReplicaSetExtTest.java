package usesCases.main.owner;

import engine.owners.OwnerAnalyzerReplicaSet;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.kubernetes.Fabric8OwnerAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceReplicaSetExt;
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
                "rbac/test-rbac.yaml", "owner/pod-with-replicaset-owner.yaml", OwnerAnalyzerReplicaSet.class);
        setupMockServerWithValidReplicaSet(ownerEngine, REPLICASET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICASETS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.REPLICASET, ownerDto.getOwnerInfo().getType());
        assertEquals(REPLICASET_NAME, ownerDto.getOwnerInfo().getName());
    }

    @Test
    void getOwnerWithPermissionNoPermissionReplicaSet() throws Exception {
        OwnerAnalyzerReplicaSet ownerEngine = (OwnerAnalyzerReplicaSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml", "owner/pod-with-replicaset-owner.yaml", OwnerAnalyzerReplicaSet.class);
        setupMockServerWithValidReplicaSet(ownerEngine, REPLICASET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICASETS_GET, false)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }

    @Test
    void getOwnerNoPermissionReplicaSetByRbac() throws Exception {
        OwnerAnalyzerReplicaSet ownerEngine = (OwnerAnalyzerReplicaSet) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml", "owner/pod-with-replicaset-owner.yaml", OwnerAnalyzerReplicaSet.class);
        setupMockServerWithValidReplicaSet(ownerEngine, REPLICASET_NAME);

        OwnerReferenceInfo ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> ownerService.getOwner(ownerRef));
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerReplicaSet() throws Exception {
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.REPLICASETS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(REPLICASET_NAME, OwnerTypeEnum.REPLICASET);
        OwnerServiceReplicaSetExt ownerService = new OwnerServiceReplicaSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }
}
