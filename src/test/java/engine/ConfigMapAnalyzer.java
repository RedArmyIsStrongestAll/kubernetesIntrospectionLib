package engine;

import usesCases.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ListMeta;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigMapAnalyzer {

    private final List<ConfigMap> configMapList;
    private final RbacAnalyzer rbacAnalyzer;

    public ConfigMapAnalyzer(String configMapYaml, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.rbacAnalyzer = rbacAnalyzer;
        this.configMapList = KubernetesYamlUtils.trySetYamlObjectList(configMapYaml, ConfigMap.class, "ConfigMap");
    }

    public ConfigMap getConfigMapByName(String requestedName, String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("configmaps", "get", requestedNamespace)) return null;
        return configMapList.stream()
                .filter(cm -> cm.getMetadata() != null)
                .filter(cm -> requestedName.equals(cm.getMetadata().getName()))
                .filter(cm -> requestedNamespace.equals(cm.getMetadata().getNamespace()))
                .findFirst()
                .orElse(null);
    }

    public ConfigMapList listAllConfigMaps(String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("configmaps", "list", requestedNamespace)) {
            return new ConfigMapList("v1", Collections.emptyList(), "ConfigMapList", new ListMeta());
        }
        List<ConfigMap> matchedConfigMaps = configMapList.stream()
                .filter(cm -> cm.getMetadata() != null && cm.getMetadata().getNamespace().equals(requestedNamespace))
                .collect(Collectors.toList());
        return new ConfigMapList("v1", matchedConfigMaps, "ConfigMapList", new ListMeta());
    }
}