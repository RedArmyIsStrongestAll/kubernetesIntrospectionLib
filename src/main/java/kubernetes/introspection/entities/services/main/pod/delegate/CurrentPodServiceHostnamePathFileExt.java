package kubernetes.introspection.entities.services.main.pod.delegate;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.env.EnvironmentProvider;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum.PODS_GET;

@Slf4j
public class CurrentPodServiceHostnamePathFileExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceHostnamePathFileExt";
    protected final EnvironmentProvider environmentProviderSystemImpl;

    public CurrentPodServiceHostnamePathFileExt(KubernetesClient kubernetesClient, String namespace, EnvironmentProvider environmentProviderSystemImpl) {
        super(kubernetesClient, namespace);
        this.environmentProviderSystemImpl = environmentProviderSystemImpl;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return new ArrayList<>(Collections.singleton(PODS_GET));
    }

    @Override
    protected String getNameClassExt() {
        return CURRENT_POD_SERVICE_NAME;
    }

    @Override
    protected String getPodName() throws Exception {
        if (environmentProviderSystemImpl != null) {
            return environmentProviderSystemImpl.readHostNameFile();
        }
        return new String(Files.readAllBytes(Paths.get("/etc/hostname"))).trim();
    }

    @Override
    protected Pod getPod() throws Exception {
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
    }

}


