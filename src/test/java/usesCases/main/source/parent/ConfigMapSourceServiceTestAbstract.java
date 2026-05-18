package usesCases.main.source.parent;

import engine.ConfigMapAnalyzer;
import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import usesCases.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.entities.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.main.source.ConfigMapSourceService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class ConfigMapSourceServiceTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String POD_NAME = "test-pod";
    protected static final String CONFIGMAP_ENV_NAME = "env-config";
    protected static final String CONFIGMAP_ENVFROM_NAME_1 = "envfrom-config1";
    protected static final String CONFIGMAP_ENVFROM_NAME_2 = "envfrom-config2";
    protected static final String CONFIGMAP_VOLUME_NAME_3 = "volume-config";

    protected KubernetesMockServer mockServer;
    protected ConfigMapSourceService configMapSourceService;


    protected PodAnalyzer getPodAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFilename);
        String podYaml = KubernetesYamlUtils.loadRbacYaml(podFilename);

        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacYaml);
        return new PodAnalyzer(podYaml, rbacAnalyzer);
    }

    protected ConfigMapAnalyzer getConfigMapAnalyzer(String rbacFilename, String cmFilename) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFilename);
        String cmYaml = KubernetesYamlUtils.loadRbacYaml(cmFilename);

        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacYaml);
        return new ConfigMapAnalyzer(cmYaml, rbacAnalyzer);
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

    protected void setupMockServerWithConfigMapByName(String confiMapName, ConfigMapAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/configmaps/" + confiMapName)
                .andReply(200, (req) -> {
                    log.info("Received ConfigMap request in namespace: {}", NAMESPACE);
                    return analyzer.getConfigMapByName(confiMapName, NAMESPACE);
                })
                .always();
    }

    protected void assertSourceInfoExists(List<ConfigSourceInfo> list,
                                          String name, ConfigUsageTypeEnum usageType, List<String> keys) {
        assertTrue(list.stream().anyMatch(info ->
                info.getName().equals(name) &&
                        info.getUsageType() == usageType &&
                        info.getType() == ConfigSourceTypeEnum.CONFIG_MAP &&
                        info.getKeys().containsAll(keys) &&
                        info.getKeys().size() == keys.size()
        ), "Expected ConfigSourceInfo for " + name);
    }

    protected void setupMockServerWithError() {
    }


}
