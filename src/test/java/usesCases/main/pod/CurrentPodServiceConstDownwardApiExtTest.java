package usesCases.main.pod;

import engine.PodAnalyzer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.kubernetes.Fabric8PodAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.env.EnvironmentProviderSystemImpl;
import kubernetes.introspection.useCases.main.pod.CurrentPodService;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceConstDownwardApiExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.pod.parent.CurrentPodServiceTestAbstract;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class CurrentPodServiceConstDownwardApiExtTest extends CurrentPodServiceTestAbstract {

    EnvironmentProviderSystemImpl mockProvider;

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
        mockProvider = mock(EnvironmentProviderSystemImpl.class);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void getCurrentPodWithCheckPermissionsValidTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstDownwardApiExt(new Fabric8PodAdapter(client), NAMESPACE, mockProvider);
        when(mockProvider.getPodName()).thenReturn(POD_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodByName(podAnalyzer, POD_NAME);

        CurrentPodService.CurrentPodDto pod = service.getCurrentPodWithCheckPermissions(permission);
        log.info("Test result: {}", pod);

        Assertions.assertNotNull(pod);
    }

    @Test
    void getCurrentPodWithCheckPermissionsNoPermissionTest() {
        CurrentPodService service = new CurrentPodServiceConstDownwardApiExt(new Fabric8PodAdapter(client), NAMESPACE, mockProvider);
        when(mockProvider.getPodName()).thenReturn(POD_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, false)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsNoPodNameTest() {
        CurrentPodService service = new CurrentPodServiceConstDownwardApiExt(new Fabric8PodAdapter(client), NAMESPACE, mockProvider);
        when(mockProvider.getPodName()).thenReturn(null);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsWrongPodNameTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstDownwardApiExt(new Fabric8PodAdapter(client), NAMESPACE, mockProvider);
        when(mockProvider.getPodName()).thenReturn(MISTAKE_POD_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        setupMockServerWithValidPodByName(podAnalyzer, MISTAKE_POD_NAME);

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodWithCheckPermissionsKubernetes500Test() {
        CurrentPodService service = new CurrentPodServiceConstDownwardApiExt(new Fabric8PodAdapter(client), NAMESPACE, mockProvider);
        when(mockProvider.getPodName()).thenReturn(POD_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        setupMockServerWithError();

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

}
