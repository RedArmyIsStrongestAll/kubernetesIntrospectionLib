package entities.services.main.service;

import engine.ServiceAnalyzer;
import entities.services.main.service.parent.ServiceServiceTestAbstract;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.pod.PodInfo;
import kubernetes.introspection.entities.models.service.ServiceInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.service.ServiceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ServiceServiceTest extends ServiceServiceTestAbstract {

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        kubernetesClient = mockServer.createClient();

        serviceService = new ServiceService(kubernetesClient);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testFindServicesForPodWithPermissionSuccessPodHasMoreLabelsThanSelector() throws IOException {
        ServiceAnalyzer serviceAnalyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/many-services.yaml");
        setupMockServerWithPodsByLabels(serviceAnalyzer);

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
        assertNotNull(result.getK8sService());
        assertNotNull(result.getServiceInfo());

        Service service = result.getK8sService();
        ServiceInfo serviceInfo = result.getServiceInfo();

        assertEquals(SERVICE_NAME, service.getMetadata().getName());
        assertEquals("ClusterIP", service.getSpec().getType());
        assertEquals("test-app", service.getSpec().getSelector().get("app"));

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

        KubernetesException ex = assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission)
        );
    }

    @Test
    void testFindServicesForPodPodHasNoLabels() {
        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(null);

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        KubernetesException ex = assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission)
        );
    }

    @Test
    void testFindServicesForPodNoServiceMatchesSelector() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/no-match-service.yaml");
        setupMockServerWithPodsByLabels(analyzer);

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        KubernetesException ex = assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission)
        );
    }

    @Test
    void testFindServicesForPodMultipleServicesFound() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/two-services.yaml");
        setupMockServerWithPodsByLabels(analyzer);

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        KubernetesException ex = assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission)
        );
    }

    @Test
    void testMapToServiceInfoServiceIsNull() {
        ServiceInfo info = ServiceService.mapToServiceInfo(null);
        Assertions.assertNull(info);
    }

    @Test
    void testFindServicesForPodServiceWithoutSelector() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/no-selector-service.yaml");
        setupMockServerWithPodsByLabels(analyzer);

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        KubernetesException ex = assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission)
        );
    }

    @Test
    void testMapToServiceInfo_ExternalIPsEmpty() throws IOException {
        ServiceAnalyzer analyzer = getServiceAnalyzer("rbac/test-rbac.yaml", "services/empty-external-ips.yaml");
        Service service = analyzer.getServiceByName("test-service", "test-namespace");

        ServiceInfo info = ServiceService.mapToServiceInfo(service);

        assertNotNull(info);
        Assertions.assertNull(info.getExternalIP());
    }

    @Test
    void testFindServicesForErrorKubernetes() throws IOException {
        setupMockServerWithError();

        PodInfo podInfo = mock(PodInfo.class);
        when(podInfo.getLabels()).thenReturn(Collections.singletonMap("app", "test-app"));

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SERVICES_GET, true)
        ));

        KubernetesException ex = assertThrows(KubernetesException.class, () ->
                serviceService.findServicesForPodWithPermission(podInfo, NAMESPACE, permission)
        );
    }

}
