package usesCases.main.owner;

import engine.owners.OwnerAnalyzerStatefulSet;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.Fabric8OwnerAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceStatefulSetExt;
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
                "rbac/test-rbac.yaml", "owner/pod-with-statefulset-owner.yaml", OwnerAnalyzerStatefulSet.class);
        setupMockServerWithValidStatefulSet(ownerEngine, STATEFULSET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.STATEFULSET, ownerDto.getOwnerInfo().getType());
        assertEquals(STATEFULSET_NAME, ownerDto.getOwnerInfo().getName());
    }

    @Test
    void getOwnerWithPermissionNoPermissionStatefulSet() throws Exception {
        OwnerAnalyzerStatefulSet ownerEngine = (OwnerAnalyzerStatefulSet) getOwnerAnalyzer(
                "rbac/test-rbac.yaml", "owner/pod-with-statefulset-owner.yaml", OwnerAnalyzerStatefulSet.class);
        setupMockServerWithValidStatefulSet(ownerEngine, STATEFULSET_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, false)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }

    @Test
    void getOwnerNoPermissionStatefulSetByRbac() throws Exception {
        OwnerAnalyzerStatefulSet ownerEngine = (OwnerAnalyzerStatefulSet) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml", "owner/pod-with-statefulset-owner.yaml", OwnerAnalyzerStatefulSet.class);
        setupMockServerWithValidStatefulSet(ownerEngine, STATEFULSET_NAME);

        OwnerReferenceInfo ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> ownerService.getOwner(ownerRef));
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerStatefulSet() throws Exception {
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(STATEFULSET_NAME, OwnerTypeEnum.STATEFULSET);
        OwnerServiceStatefulSetExt ownerService = new OwnerServiceStatefulSetExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }
}
