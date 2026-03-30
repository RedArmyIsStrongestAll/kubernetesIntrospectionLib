package entities.services.main.source.parent;

import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import engine.SecretAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.source.ConfigSourceInfo;
import kubernetes.introspection.entities.models.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.models.source.ConfigUsageTypeEnum;
import kubernetes.introspection.entities.services.main.source.SecretSourceService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public abstract class SecretSourceServiceTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String POD_NAME = "test-pod";
    protected static final String SECRET_ENV_NAME = "secret-env";
    protected static final String SECRET_ENVFROM_NAME_1 = "secret-envfrom1";
    protected static final String SECRET_ENVFROM_NAME_2 = "secret-envfrom2";
    protected static final String SECRET_VOLUME_NAME_3 = "secret-volume";

    protected KubernetesMockServer mockServer;
    protected SecretSourceService secretSourceService;

    protected PodAnalyzer getPodAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFilename);
        String podYaml = KubernetesYamlUtils.loadRbacYaml(podFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacYaml);
        return new PodAnalyzer(podYaml, rbacAnalyzer);
    }

    protected SecretAnalyzer getSecretAnalyzer(String rbacFilename, String secretFilename) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFilename);
        String secretYaml = KubernetesYamlUtils.loadRbacYaml(secretFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacYaml);
        return new SecretAnalyzer(secretYaml, rbacAnalyzer);
    }

    protected void setupMockServerWithPodsByName(String podName, PodAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + podName)
                .andReply(200, (req) -> {
                    log.info("Received Pod request in namespace: {}", NAMESPACE);
                    return analyzer.getPodByName(podName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithSecretByName(String secretName, SecretAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/secrets/" + secretName)
                .andReply(200, (req) -> {
                    log.info("Received Secret request in namespace: {}", NAMESPACE);
                    return analyzer.getSecretByName(secretName, NAMESPACE);
                })
                .always();
    }

    protected void assertSourceInfoExists(List<ConfigSourceInfo> list,
                                          String name, ConfigUsageTypeEnum usageType, List<String> keys) {
        assertTrue(list.stream().anyMatch(info ->
                info.getName().equals(name) &&
                        info.getUsageType() == usageType &&
                        info.getType() == ConfigSourceTypeEnum.SECRET &&
                        info.getKeys().containsAll(keys) &&
                        info.getKeys().size() == keys.size()
        ), "Expected ConfigSourceInfo for " + name);
    }
}