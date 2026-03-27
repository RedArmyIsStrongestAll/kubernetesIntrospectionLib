package kubernetes.introspection.entities.services.main.pod.delegate;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermission;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static kubernetes.introspection.entities.models.dto.permision.ResourcePermission.PODS_GET;

@Slf4j
public class CurrentPodServiceConstNamePodExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceConstNamePodExt";

    public CurrentPodServiceConstNamePodExt(KubernetesClient kubernetesClient, String namespace, String podName) {
        super(kubernetesClient, namespace);
        this.podName = podName;
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
        return podName;
    }

    @Override
    protected Pod getPod() throws Exception {
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
    }

}