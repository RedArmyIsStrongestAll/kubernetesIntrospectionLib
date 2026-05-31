package kubernetes.introspection.useCases.ports;

import kubernetes.introspection.entities.pod.PodInfo;

import java.util.List;
import java.util.Map;

public interface KubernetesPodPort {
    PodInfo getPodByName(String name, String namespace);

    List<PodInfo> listPodsByLabels(Map<String, String> labels, String namespace);

    List<PodInfo> listAllPods(String namespace);
}
