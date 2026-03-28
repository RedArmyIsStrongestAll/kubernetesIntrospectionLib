package entities.services.main.pod;

import engine.PodAnalyzer;
import entities.services.main.pod.parent.CurrentPodServiceTestAbstract;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.pod.PodInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstNamePodExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
class CurrentPodServiceConstNamePodExtTest extends CurrentPodServiceTestAbstract {

    @BeforeEach
    void setUp() {
        this.mockServer = new KubernetesMockServer();
        this.mockServer.init();
        this.client = mockServer.createClient();
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void getCurrentPodWithCheckPermissionsValidTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstNamePodExt(client, NAMESPACE, POD_NAME);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodByName(podAnalyzer, permission, POD_NAME);

        CurrentPodService.CurrentPodDto pod = service.getCurrentPodWithCheckPermissions(permission);
        log.info("Test result: {}", pod);
        Assertions.assertNotNull(pod);
    }

    @Test
    void getCurrentPodWithCheckPermissionsNoPermissionTest() {
        CurrentPodService service = new CurrentPodServiceConstNamePodExt(client, NAMESPACE, POD_NAME);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, false)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsNoPodNameTest() {
        CurrentPodService service = new CurrentPodServiceConstNamePodExt(client, NAMESPACE, null);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsWrongPodNameTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstNamePodExt(client, NAMESPACE, MISTAKE_POD_NAME);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodByName(podAnalyzer, permission, MISTAKE_POD_NAME);

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodWithCheckPermissionsKubernetes500Test() {
        CurrentPodService service = new CurrentPodServiceConstNamePodExt(client, NAMESPACE, POD_NAME);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        setupMockServerWith500();

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }
}
