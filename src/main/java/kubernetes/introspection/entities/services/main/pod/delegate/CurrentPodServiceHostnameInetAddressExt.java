package kubernetes.introspection.entities.services.main.pod.delegate;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.env.EnvironmentProvider;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static kubernetes.introspection.entities.models.permision.ResourcePermissionEnum.PODS_GET;

@Slf4j
public class CurrentPodServiceHostnameInetAddressExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceHostnameInetAddressExt";
    protected final EnvironmentProvider environmentProviderSystemImpl;


    public CurrentPodServiceHostnameInetAddressExt(KubernetesClient kubernetesClient, String namespace, EnvironmentProvider environmentProviderSystemImpl) {
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
            return environmentProviderSystemImpl.getInetAddressLocalHost();
        }
        return InetAddress.getLocalHost().getHostName();
    }

    @Override
    protected Pod getPod() throws Exception {
        log.info("Start k8s request");
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
    }
}


