package entities.services.main.pod.parent;

import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static entities.services.utils.TestUtils.loadRbacYaml;

@Slf4j
public class CurrentPodServiceTestAbstract {

    protected static final String POD_NAME = "test-pod";
    protected static final String MISTAKE_POD_NAME = "no-test-pod";

    protected static final String POD_IP = "10.244.0.1";
    protected static final String MISTAKE_POD_IP = "0.0.0.0";

    protected static final List<String> VALID_LABELS = List.of("app=test-app", "env=prod");
    protected static final List<String> INVALID_LABELS = List.of("app=wrong", "env=wrong");

    protected static final String NAMESPACE = "test-namespace";

    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;


    protected PodAnalyzer getPodAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String yamlRbacContent = loadRbacYaml(rbacFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(yamlRbacContent);

        String yamlPodContent = loadRbacYaml(podFilename);

        return new PodAnalyzer(yamlPodContent, rbacAnalyzer);
    }


    protected Map<String, String> parseLabels(List<String> labels) {
        return labels.stream()
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> parts[0].trim(),
                        parts -> parts[1].trim()
                ));
    }


    protected void setupMockServerWithValidPodByName(PodAnalyzer analyzer, PermissionInfo permissionApp, String podName) {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + podName)
                .andReply(200, request -> {
                    log.info("Received GET request for pod: {}/{}", NAMESPACE, podName);
                    return analyzer.getPodByName(permissionApp, podName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWith500() {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + POD_NAME)
                .andReply(500, request -> "Internal Server Error")
                .always();
    }

    protected void setupMockServerWithValidPodListByIp(PodAnalyzer analyzer, PermissionInfo permissionApp, String podIp) {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods")
                .andReply(200, request -> {
                    log.info("Received LIST request for pods on ip {} in namespace: {}", podIp, NAMESPACE);
                    return analyzer.getPodListByIp(permissionApp, podIp, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithPodsByLabels(PodAnalyzer analyzer, PermissionInfo permissionApp, Map<String, String> labels) {
        String labelSelector = buildLabelSelectorQuery(labels);
        String query = "/api/v1/namespaces/" + NAMESPACE + "/pods" + "?labelSelector=" + labelSelector;

        mockServer.expect()
                .get()
                .withPath(query)
                .andReply(200, request -> {
                    log.info("Received LIST request for pods in namespace: {}", NAMESPACE);
                    return analyzer.getPodListByLabels(permissionApp, labels, NAMESPACE);
                })
                .always();
    }

    private String buildLabelSelectorQuery(Map<String, String> labels) {
        return labels.entrySet().stream()
                .map(entry ->
                        URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "%3D" +
                                URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("%2C"));
    }
}
