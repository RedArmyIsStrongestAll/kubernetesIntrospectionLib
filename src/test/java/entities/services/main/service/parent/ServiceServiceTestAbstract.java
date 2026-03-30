package entities.services.main.service.parent;

import engine.RbacAnalyzer;
import engine.ServiceAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.services.main.service.ServiceService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ServiceServiceTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String SERVICE_NAME = "test-service";

    protected KubernetesClient kubernetesClient;
    protected ServiceService serviceService;
    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;


    protected ServiceAnalyzer getServiceAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFilename);
        String serviceYaml = KubernetesYamlUtils.loadRbacYaml(podFilename);

        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacYaml);
        return new ServiceAnalyzer(serviceYaml, rbacAnalyzer);
    }


    protected void setupMockServerWithError() {
    }

    protected void setupMockServerWithPodsByLabels(ServiceAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/services")
                .andReply(200, (requet) -> {
                    log.info("Received LIST request for services in namespace: {}", NAMESPACE);
                    return analyzer.listAllServices(NAMESPACE);
                })
                .always();
    }
}
