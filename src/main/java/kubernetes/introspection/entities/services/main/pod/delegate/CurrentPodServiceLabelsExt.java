package kubernetes.introspection.entities.services.main.pod.delegate;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum.PODS_GET;
import static kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum.PODS_LIST;


@Slf4j
public class CurrentPodServiceLabelsExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceLabelsExt";
    private final List<String> labelList;

    /**
     * Принимает List<String> формата "key=value"
     * <p> Пример: List.of("app=my-app", "env=prod", "version=1.0")</p>
     */
    public CurrentPodServiceLabelsExt(KubernetesClient kubernetesClient, String namespace, List<String> labelList) {
        super(kubernetesClient, namespace);
        this.labelList = labelList;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermission() {
        return new ArrayList<>(List.of(PODS_LIST, PODS_GET));
    }

    @Override
    protected String getNameClassExt() {
        return CURRENT_POD_SERVICE_NAME;
    }

    @Override
    protected String getPodName() throws Exception {
        if (labelList != null && !labelList.isEmpty()) {
            return "The search will be by labels: " + String.join(", ", labelList);
        }
        return null;
    }

    @Override
    protected Pod getPod() throws Exception {
        Map<String, String> labels = labelList.stream()
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> parts[0].trim(),
                        parts -> parts[1].trim()
                ));

        List<Pod> pods = kubernetesClient.pods()
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems().stream()
                .toList();

        if (pods.size() != 1) {
            return null;
        }

        return pods.get(0);
    }

}