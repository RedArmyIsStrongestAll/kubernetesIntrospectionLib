package entities.services.main.pod;

import engine.PodAnalyzer;
import entities.services.main.pod.parent.CurrentPodServiceTestAbstract;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceLabelsExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

@Slf4j
class CurrentPodServiceLabelsExtTest extends CurrentPodServiceTestAbstract {

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
    void getCurrentPodWithCheckPermissionsValidTest() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-short-pod.yaml");
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)));

        setupMockServerWithPodsByLabels(podAnalyzer, parseLabels(VALID_LABELS));

        CurrentPodService service = new CurrentPodServiceLabelsExt(client, NAMESPACE, VALID_LABELS);
        CurrentPodService.CurrentPodDto pod = service.getCurrentPodWithCheckPermissions(permission);
        log.info("Test result: {}", pod);
        Assertions.assertNotNull(pod);
    }

    @Test
    void getCurrentPodWithCheckPermissionsNoPermissionTest() {
        CurrentPodService service = new CurrentPodServiceLabelsExt(client, NAMESPACE, VALID_LABELS);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, false),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, false)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsNoPodNameTest() {
        CurrentPodService service = new CurrentPodServiceLabelsExt(client, NAMESPACE, Collections.emptyList());
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsWrongPodNameTest() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-pod.yaml");
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)));

        setupMockServerWithPodsByLabels(podAnalyzer, parseLabels(INVALID_LABELS));

        CurrentPodService service = new CurrentPodServiceLabelsExt(client, NAMESPACE, INVALID_LABELS);
        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }

    @Test
    void getCurrentPodWithCheckPermissionsKubernetes500Test() {
        CurrentPodService service = new CurrentPodServiceLabelsExt(client, NAMESPACE, VALID_LABELS);
        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        setupMockServerWithError();

        Assertions.assertThrows(KubernetesException.class, () -> {
            service.getCurrentPodWithCheckPermissions(permission);
        });
    }
}