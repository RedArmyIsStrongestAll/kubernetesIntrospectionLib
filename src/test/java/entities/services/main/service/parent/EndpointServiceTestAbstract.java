package entities.services.main.service.parent;

import engine.EndpointAnalyzer;
import engine.RbacAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.services.main.service.EndpointService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class EndpointServiceTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String SERVICE_NAME = "test-service";
    protected static final String MISTAKE_SERVICE_NAME = "no-test-service";

    protected KubernetesClient kubernetesClient;
    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;
    protected EndpointService endpointService;


    protected EndpointAnalyzer getEndpointAnalyzer(String rbacFile, String endpointsFile) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFile);
        String endpointsYaml = KubernetesYamlUtils.loadRbacYaml(endpointsFile);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacYaml);
        return new EndpointAnalyzer(endpointsYaml, rbacAnalyzer);
    }


    protected void setupMockServerWithError() {
    }

    protected void setupMockServerWithEndpoints(EndpointAnalyzer analyzer, String serviceName) {
        String fieldSelectorQuery = KubernetesYamlUtils.buildFieldSelectorQuery("metadata.name", serviceName);
        String path = "/api/v1/namespaces/" + NAMESPACE + "/endpoints?fieldSelector=" + fieldSelectorQuery;
        mockServer.expect()
                .get()
                .withPath(path)
                .andReply(200, request -> {
                    log.info("Received LIST request for endpoints with serviceName: {}", serviceName);
                    return analyzer.listEndpointsByServiceName(serviceName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithEmptyEndpoints(EndpointAnalyzer analyzer, String serviceName) {
        String fieldSelectorQuery = KubernetesYamlUtils.buildFieldSelectorQuery("metadata.name", serviceName);
        String path = "/api/v1/namespaces/" + NAMESPACE + "/endpoints?fieldSelector=" + fieldSelectorQuery;

        mockServer.expect()
                .get()
                .withPath(path)
                .andReply(200, request -> {
                    log.info("Received LIST request for endpoints with serviceName: {}", serviceName);
                    List<Endpoints> endpoints = analyzer.listEndpointsByServiceName(serviceName, NAMESPACE).getItems();
                    return new EndpointsList("v1", endpoints, "EndpointsList", new ListMeta());
                })
                .always();
    }

}
