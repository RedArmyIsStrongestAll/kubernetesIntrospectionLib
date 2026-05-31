package usesCases.main.service;

import engine.ServiceAnalyzer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.Fabric8ServiceAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.service.ServiceInfo;
import kubernetes.introspection.useCases.main.service.ServiceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.service.parent.ServiceServiceTestAbstract;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ServiceServiceTest extends ServiceServiceTestAbstract {

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        kubernetesClient = mockServer.createClient();

        serviceService = new ServiceService(new Fabric8ServiceAdapter(kubernetesClient));
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testFindServicesForPodWithPermissionSuccessPodHasMoreLabelsThanSelector() throws IOException {
        ServiceAnalyzer serviceAnalyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/many-services.yaml");
        setupMockServerWithServiceList(serviceAnalyzer);

        Map<String, String> podLabels = new HashMap<>();
        podLabels.put("app", "test-app");
        podLabels.put("env", "prod");

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(podLabels);

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        ServiceService.ServiceDto result = serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission);

        assertNotNull(result);
        assertNotNull(result.getServiceInfo());

        ServiceInfo serviceInfo = result.getServiceInfo();
        assertEquals(SERVICE_NAME, serviceInfo.getName());
        assertEquals("ClusterIP", serviceInfo.getType());
        assertEquals("test-app", serviceInfo.getSelector().get("app"));
        assertNotNull(serviceInfo.getPorts());
    }

    @Test
    void testFindServicesForPodWithPermissionNoServicesGetPermission() {
        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, false)
        ));

        assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission));
    }

    @Test
    void testFindServicesForPodPodHasNoLabels() {
        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(null);

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission));
    }

    @Test
    void testFindServicesForPodNoServiceMatchesSelector() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/no-match-service.yaml");
        setupMockServerWithServiceList(analyzer);

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission));
    }

    @Test
    void testFindServicesForPodMultipleServicesFound() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/two-services.yaml");
        setupMockServerWithServiceList(analyzer);

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission));
    }

    @Test
    void testFindServicesForPodServiceWithoutSelector() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/no-selector-service.yaml");
        setupMockServerWithServiceList(analyzer);

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission));
    }

    @Test
    void testFindServicesForErrorKubernetes() {
        setupMockServerWithError();

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission));
    }
}
