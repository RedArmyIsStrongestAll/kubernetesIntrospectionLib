package entities.services.main.service.parent;

import engine.EndpointAnalyzer;
import engine.RbacAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.services.main.service.EndpointService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class EndpointServiceTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String SERVICE_NAME = "test-service";

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

}
