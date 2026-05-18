package usesCases.main.owner;

import engine.owners.OwnerAnalyzerStatefulSet;
import usesCases.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceStatefulSetExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceStatefulSetExtTest extends OwnerServiceTestAbstract {
    private static final String STATEFULSET_NAME = "test-statefulset";

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
    void getOwnerWithPermissionValidStatefulSetOwner() throws Exception {
        OwnerAnalyzerStatefulSet ownerEngine = (OwnerAnalyzerStatefulSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-statefulset-owner.yaml",
                OwnerAnalyzerStatefulSet.class
        );
        setupMockServerWithValidStatefulSet(ownerEngine, STATEFULSET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.STATEFULSET, ownerDto.getOwnerInfo().getType());
        assertEquals(STATEFULSET_NAME, ownerDto.getOwnerInfo().getName());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionStatefulSet() throws Exception {
        OwnerAnalyzerStatefulSet ownerEngine = (OwnerAnalyzerStatefulSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-statefulset-owner.yaml",
                OwnerAnalyzerStatefulSet.class
        );
        setupMockServerWithValidStatefulSet(ownerEngine, STATEFULSET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoPermissionStatefulSetByRbac() throws Exception {
        OwnerAnalyzerStatefulSet ownerEngine = (OwnerAnalyzerStatefulSet) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-statefulset-owner.yaml",
                OwnerAnalyzerStatefulSet.class
        );
        setupMockServerWithValidStatefulSet(ownerEngine, STATEFULSET_NAME);

        OwnerReference ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerStatefulSet() throws Exception {
        OwnerAnalyzerStatefulSet ownerEngine = (OwnerAnalyzerStatefulSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-statefulset-owner.yaml",
                OwnerAnalyzerStatefulSet.class
        );
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}