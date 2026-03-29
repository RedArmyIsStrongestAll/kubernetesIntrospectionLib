package engine;

import entities.services.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PodAnalyzer {
    private final List<Pod> podList;
    private final RbacAnalyzer rbacAnalyzer;

    public PodAnalyzer(String podYaml, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.rbacAnalyzer = rbacAnalyzer;

        this.podList = TestUtils.trySetYamlObjectList(podYaml, Pod.class, "Pod");
    }

    public Pod getPodByName(String requestedName, String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) return null;

        return podList.stream()
                .filter(pod -> pod.getMetadata() != null)
                .filter(pod -> pod.getMetadata().getName().equals(requestedName))
                .filter(pod -> pod.getMetadata().getNamespace().equals(requestedNamespace))
                .findFirst()
                .orElse(null);
    }

    public PodList getPodListByIp(String requestedIp, String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        List<Pod> matchedPods = podList.stream()
                .filter(pod -> pod.getMetadata() != null && pod.getStatus() != null && pod.getStatus().getPodIP() != null)
                .filter(pod -> pod.getStatus().getPodIP().equals(requestedIp))
                .filter(pod -> pod.getMetadata().getNamespace().equals(requestedNamespace))
                .collect(Collectors.toList());

        return new PodList("v1", matchedPods, "PodList", new ListMeta());
    }

    public PodList getPodListByLabels(Map<String, String> requestedLabels, String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        List<Pod> matchedPods = podList.stream()
                .filter(pod -> pod.getMetadata() != null && pod.getMetadata().getLabels() != null)
                .filter(pod -> pod.getMetadata().getNamespace().equals(requestedNamespace))
                .filter(pod -> {
                    Map<String, String> labels = pod.getMetadata().getLabels();
                    return requestedLabels.entrySet().stream()
                            .allMatch(e -> labels.containsKey(e.getKey()) && e.getValue().equals(labels.get(e.getKey())));
                })
                .collect(Collectors.toList());

        return new PodList("v1", matchedPods, "PodList", new ListMeta());
    }

    public PodList listAllPods(String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        List<Pod> matchedPods = podList.stream()
                .filter(pod -> pod.getMetadata() != null && pod.getMetadata().getNamespace().equals(requestedNamespace))
                .collect(Collectors.toList());

        return new PodList("v1", matchedPods, "PodList", new ListMeta());
    }

    public PodList listPodsByPrefix(String requestedNamespace, String prefix) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        List<Pod> matchedPods = podList.stream()
                .filter(pod -> pod.getMetadata() != null && pod.getMetadata().getNamespace().equals(requestedNamespace))
                .filter(pod -> pod.getMetadata().getName().startsWith(prefix))
                .collect(Collectors.toList());

        return new PodList("v1", matchedPods, "PodList", new ListMeta());
    }
}