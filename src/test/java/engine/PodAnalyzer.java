package engine;

import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.Collections;
import java.util.Map;

public class PodAnalyzer {

    private final Pod pod;
    private final RbacAnalyzer rbacAnalyzer;


    public PodAnalyzer(String podYaml, RbacAnalyzer rbacAnalyzer) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.pod = yaml.loadAs(podYaml, Pod.class);

        this.rbacAnalyzer = rbacAnalyzer;
    }

    public Pod getPodByName(String requestedName,
                            String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) return null;

        if (pod.getMetadata() == null) return null;

        if (!pod.getMetadata().getName().equals(requestedName)) return null;
        if (!pod.getMetadata().getNamespace().equals(requestedNamespace)) return null;

        return pod;
    }

    public PodList getPodListByIp(String requestedIp,
                                  String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (pod.getMetadata() == null || pod.getStatus() == null || pod.getStatus().getPodIP() == null) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (!pod.getStatus().getPodIP().equals(requestedIp)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
        if (!pod.getMetadata().getNamespace().equals(requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        return new PodList(
                "v1",
                Collections.singletonList(pod),
                "PodList",
                new ListMeta()
        );
    }

    public PodList getPodListByLabels(Map<String, String> requestedLabels,
                                      String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (pod.getMetadata() == null || pod.getMetadata().getLabels() == null) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (!pod.getMetadata().getNamespace().equals(requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
        boolean allLabelsMatch = requestedLabels.entrySet().stream()
                .allMatch(e -> e.getValue().equals(pod.getMetadata().getLabels().get(e.getKey())));
        if (!allLabelsMatch) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        return new PodList("v1", Collections.singletonList(pod), "PodList", new ListMeta());
    }
}