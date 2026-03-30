package entities.services.main.service;

import engine.EndpointAnalyzer;
import entities.services.main.service.parent.EndpointServiceTestAbstract;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.service.ServiceEndpointAddress;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.service.EndpointService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class EndpointServiceTest extends EndpointServiceTestAbstract {

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        kubernetesClient = mockServer.createClient();
        endpointService = new EndpointService(kubernetesClient);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testGetEndpointsForServiceWithPermissionSuccess() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/test-rbac.yaml", "endpoints/valid-endpoints.yaml");
        setupMockServerWithEndpoints(analyzer, SERVICE_NAME);

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.ENDPOINTS_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.ENDPOINTS_GET, true)
        ));

        EndpointService.EndpointDto result = endpointService.getEndpointsForServiceWithPermission(SERVICE_NAME, NAMESPACE, permission);

        assertNotNull(result);
        assertNotNull(result.getK8sEndpoints());
        assertNotNull(result.getEndpointsInfo());
        assertFalse(result.getEndpointsInfo().isEmpty());

        ServiceEndpointAddress endpoint = result.getEndpointsInfo().get(0);
        assertNotNull(endpoint);
        assertEquals("10.1.0.1", endpoint.getIp());
        assertEquals("pod-1", endpoint.getPodName());
        assertEquals("Pod", endpoint.getTargetKind());
        assertEquals("pod-1", endpoint.getTargetName());
        assertEquals(Integer.valueOf(80), endpoint.getPort());
        assertTrue(endpoint.isReady());

        List<ServiceEndpointAddress> endpoints = result.getEndpointsInfo();
        assertEquals(3, endpoints.size());
        assertTrue(endpoints.stream().anyMatch(e -> "10.1.0.1".equals(e.getIp()) && e.isReady()));
        assertTrue(endpoints.stream().anyMatch(e -> "10.1.0.2".equals(e.getIp()) && e.isReady()));
        assertTrue(endpoints.stream().anyMatch(e -> "10.1.0.3".equals(e.getIp()) && !e.isReady()));
    }

    @Test
    void testGetEndpointsForServiceWithPermissionNoGetPermission() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/no-get-permission.yaml", "endpoints/valid-endpoints.yaml");
        setupMockServerWithEndpoints(analyzer, SERVICE_NAME);

        KubernetesException exception = assertThrows(KubernetesException.class, () ->
                endpointService.getEndpointsForService(SERVICE_NAME, NAMESPACE)
        );
    }

    @Test
    void testGetEndpointsForServiceEndpointsNotFound() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/test-rbac.yaml", "endpoints/valid-endpoints.yaml");
        setupMockServerWithEndpoints(analyzer, MISTAKE_SERVICE_NAME);

        KubernetesException exception = assertThrows(KubernetesException.class, () ->
                endpointService.getEndpointsForService(SERVICE_NAME, NAMESPACE)
        );
    }

    @Test
    void testGetEndpointsForServiceMultipleEndpointsFound() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/test-rbac.yaml", "endpoints/multi-endpoints.yaml");
        setupMockServerWithEndpoints(analyzer, SERVICE_NAME);

        KubernetesException exception = assertThrows(KubernetesException.class, () ->
                endpointService.getEndpointsForService(SERVICE_NAME, NAMESPACE)
        );
    }

    @Test
    void testMapToEndpointsInfoWithEmptySubsets() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/test-rbac.yaml", "endpoints/invalid-empty-subsets.yaml");
        setupMockServerWithEndpoints(analyzer, SERVICE_NAME);

        EndpointService.EndpointDto result = endpointService.getEndpointsForService(SERVICE_NAME, NAMESPACE);

        assertNotNull(result.getEndpointsInfo());
        assertTrue(result.getEndpointsInfo().isEmpty());
    }

    @Test
    void testMapToEndpointsInfoWithNullSubsets() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/test-rbac.yaml", "endpoints/invalid-null-subsets.yaml");
        setupMockServerWithEndpoints(analyzer, SERVICE_NAME);

        EndpointService.EndpointDto result = endpointService.getEndpointsForService(SERVICE_NAME, NAMESPACE);

        assertNotNull(result.getEndpointsInfo());
        assertTrue(result.getEndpointsInfo().isEmpty());
    }

    @Test
    void testMapToEndpointsInfoWithMixedReadyAndNotReady() throws IOException {
        EndpointAnalyzer analyzer = getEndpointAnalyzer("rbac/test-rbac.yaml", "endpoints/mixed-ready.yaml");
        setupMockServerWithEndpoints(analyzer, SERVICE_NAME);

        EndpointService.EndpointDto result = endpointService.getEndpointsForService(SERVICE_NAME, NAMESPACE);

        assertNotNull(result.getEndpointsInfo());
        assertEquals(2, result.getEndpointsInfo().size());
        assertTrue(result.getEndpointsInfo().stream().anyMatch(e -> "10.1.0.1".equals(e.getIp()) && e.isReady()));
        assertTrue(result.getEndpointsInfo().stream().anyMatch(e -> "10.1.0.3".equals(e.getIp()) && !e.isReady()));
    }

}
