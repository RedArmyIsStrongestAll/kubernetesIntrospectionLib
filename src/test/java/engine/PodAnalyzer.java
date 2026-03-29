package engine;

import entities.services.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class PodAnalyzer {

    private final Pod pod;
    private final RbacAnalyzer rbacAnalyzer;

    public PodAnalyzer(String podYaml, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.pod = (Pod) TestUtils.changeSetYamlObject(podYaml, Pod.class);

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

    public PodList listAllPods(String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (pod.getMetadata() == null || !pod.getMetadata().getNamespace().equals(requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        return new PodList("v1", Collections.singletonList(pod), "PodList", new ListMeta());
    }

    public PodList listPodsByPrefix(String requestedNamespace, String prefix) {
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (pod.getMetadata() == null || !pod.getMetadata().getNamespace().equals(requestedNamespace)) {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }

        if (pod.getMetadata().getName().startsWith(prefix)) {
            return new PodList("v1", Collections.singletonList(pod), "PodList", new ListMeta());
        } else {
            return new PodList("v1", Collections.emptyList(), "PodList", new ListMeta());
        }
    }
}