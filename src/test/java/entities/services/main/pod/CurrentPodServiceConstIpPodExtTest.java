package entities.services.main.pod;

import engine.PodAnalyzer;
import entities.services.main.pod.parent.CurrentPodServiceTestAbstract;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.pod.PodInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstIpPodExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
class CurrentPodServiceConstIpPodExtTest extends CurrentPodServiceTestAbstract {

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
    void getCurrentPodInfoWithCheckPermissionsValidTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstIpPodExt(client, NAMESPACE, POD_IP);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodListByIp(podAnalyzer, permission, POD_IP);

        PodInfo pod = service.getCurrentPodInfoWithCheckPermissions(permission);
        log.info("Test result: {}", pod);
        Assertions.assertNotNull(pod);
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsNoPermissionTest() {
        CurrentPodService service = new CurrentPodServiceConstIpPodExt(client, NAMESPACE, POD_IP);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, false),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, false)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodInfoWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsNoPodIpTest() {
        CurrentPodService service = new CurrentPodServiceConstIpPodExt(client, NAMESPACE, null);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodInfoWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsWrongPodIpTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstIpPodExt(client, NAMESPACE, MISTAKE_POD_IP);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodListByIp(podAnalyzer, permission, MISTAKE_POD_IP);

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodInfoWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsKubernetes500Test() {
        CurrentPodService service = new CurrentPodServiceConstIpPodExt(client, NAMESPACE, POD_IP);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)));

        setupMockServerWith500();

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodInfoWithCheckPermissions(permission);
        });
    }

}
