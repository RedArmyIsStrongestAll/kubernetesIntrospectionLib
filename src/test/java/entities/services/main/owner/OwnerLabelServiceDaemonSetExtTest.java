package entities.services.main.owner;

import engine.owners.OwnerAnalyzerDaemonSet;
import entities.services.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceDaemonSetExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceDaemonSetExtTest extends OwnerServiceTestAbstract {
    private static final String DAEMONSET_NAME = "test-daemonset";

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
    void getOwnerWithPermissionValidDaemonSetOwner() throws Exception {
        OwnerAnalyzerDaemonSet ownerEngine = (OwnerAnalyzerDaemonSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-daemonset-owner.yaml",
                OwnerAnalyzerDaemonSet.class
        );
        setupMockServerWithValidDaemonSet(ownerEngine, DAEMONSET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DAEMONSETS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(DAEMONSET_NAME, OwnerTypeEnum.DAEMONSET);
        OwnerService ownerService = new OwnerServiceDaemonSetExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.DAEMONSET, ownerDto.getOwnerInfo().getType());
        assertEquals(DAEMONSET_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(3, ownerDto.getOwnerInfo().getDesiredReplicas());
        assertEquals(3, ownerDto.getOwnerInfo().getAvailableReplicas());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionDaemonSet() throws Exception {
        OwnerAnalyzerDaemonSet ownerEngine = (OwnerAnalyzerDaemonSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-daemonset-owner.yaml",
                OwnerAnalyzerDaemonSet.class
        );
        setupMockServerWithValidDaemonSet(ownerEngine, DAEMONSET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DAEMONSETS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(DAEMONSET_NAME, OwnerTypeEnum.DAEMONSET);
        OwnerService ownerService = new OwnerServiceDaemonSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoPermissionDaemonSetByRbac() throws Exception {
        OwnerAnalyzerDaemonSet ownerEngine = (OwnerAnalyzerDaemonSet) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-daemonset-owner.yaml",
                OwnerAnalyzerDaemonSet.class
        );

        setupMockServerWithValidDaemonSet(ownerEngine, DAEMONSET_NAME);

        OwnerReference ownerRef = getOwnerReference(DAEMONSET_NAME, OwnerTypeEnum.DAEMONSET);
        OwnerService ownerService = new OwnerServiceDaemonSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerDaemonSet() throws Exception {
        OwnerAnalyzerDaemonSet ownerEngine = (OwnerAnalyzerDaemonSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-daemonset-owner.yaml",
                OwnerAnalyzerDaemonSet.class
        );

        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.DAEMONSETS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(DAEMONSET_NAME, OwnerTypeEnum.DAEMONSET);
        OwnerService ownerService = new OwnerServiceDaemonSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}