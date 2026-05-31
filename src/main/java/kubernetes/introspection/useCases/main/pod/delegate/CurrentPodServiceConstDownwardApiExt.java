package kubernetes.introspection.useCases.main.pod.delegate;

import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.env.EnvironmentProvider;
import kubernetes.introspection.useCases.main.pod.CurrentPodService;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static kubernetes.introspection.entities.permision.ResourcePermissionEnum.PODS_GET;

@Slf4j
public class CurrentPodServiceConstDownwardApiExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceConstDownwardApiExt";
    protected final EnvironmentProvider environmentProviderSystemImpl;

    public CurrentPodServiceConstDownwardApiExt(KubernetesPodPort podPort, String namespace, EnvironmentProvider environmentProviderSystemImpl) {
        super(podPort, namespace);
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
            return environmentProviderSystemImpl.getPodName();
        }
        return System.getenv("POD_NAME");
    }

    @Override
    protected PodInfo getPodInfo() throws Exception {
        log.info("Start k8s request");
        return podPort.getPodByName(podName, namespace);
    }
}
