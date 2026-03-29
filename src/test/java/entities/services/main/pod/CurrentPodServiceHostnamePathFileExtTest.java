package entities.services.main.pod;

import engine.PodAnalyzer;
import entities.services.main.pod.parent.CurrentPodServiceTestAbstract;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.env.EnvironmentProvider;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceHostnamePathFileExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class CurrentPodServiceHostnamePathFileExtTest extends CurrentPodServiceTestAbstract {

    private EnvironmentProvider mockProvider;

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
        mockProvider = mock(EnvironmentProvider.class);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void getCurrentPodWithCheckPermissionsValidTest() throws Exception {
        when(mockProvider.readHostNameFile()).thenReturn(POD_NAME);

        CurrentPodService service = new CurrentPodServiceHostnamePathFileExt(client, NAMESPACE, mockProvider);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodByName(podAnalyzer, POD_NAME);
        CurrentPodService.CurrentPodDto pod = service.getCurrentPodWithCheckPermissions(permission);
        log.info("Test result: {}", pod);
        Assertions.assertNotNull(pod);
    }

    @Test
    void getCurrentPodWithCheckPermissionsNoPermissionTest() throws Exception {
        when(mockProvider.readHostNameFile()).thenReturn(POD_NAME);

        CurrentPodService service = new CurrentPodServiceHostnamePathFileExt(client, NAMESPACE, mockProvider);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, false)));
        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsNoPodNameTest() throws Exception {
        when(mockProvider.readHostNameFile()).thenThrow(new RuntimeException("File not found"));

        CurrentPodService service = new CurrentPodServiceHostnamePathFileExt(client, NAMESPACE, mockProvider);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));
        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsWrongPodNameTest() throws Exception {
        when(mockProvider.readHostNameFile()).thenReturn(MISTAKE_POD_NAME);

        CurrentPodService service = new CurrentPodServiceHostnamePathFileExt(client, NAMESPACE, mockProvider);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodByName(podAnalyzer, MISTAKE_POD_NAME);
        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodWithCheckPermissionsKubernetes500Test() throws Exception {
        when(mockProvider.readHostNameFile()).thenReturn(POD_NAME);

        CurrentPodService service = new CurrentPodServiceHostnamePathFileExt(client, NAMESPACE, mockProvider);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));
        setupMockServerWithError();
        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }
}