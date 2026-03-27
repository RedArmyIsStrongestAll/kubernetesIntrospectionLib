package kubernetes.introspection.entities.services.main.pod.delegate;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermission;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static kubernetes.introspection.entities.models.dto.permision.ResourcePermission.PODS_GET;

@Slf4j
public class CurrentPodServiceRegexDownwardApiExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceRegexDownwardApiExt";

    public CurrentPodServiceRegexDownwardApiExt(KubernetesClient kubernetesClient, String namespace) {
        super(kubernetesClient, namespace);
    }

    @Override
    protected List<ResourcePermission> getPermission() {
        return new ArrayList<>(Collections.singleton(PODS_GET));
    }

    @Override
    protected String getNameClassExt() {
        return CURRENT_POD_SERVICE_NAME;
    }

    @Override
    protected String getPodName() throws Exception {
        return System.getenv().entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey().toLowerCase();
                    return key.contains("pod") && key.contains("name");
                })
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    @Override
    protected Pod getPod() throws Exception {
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
    }

}