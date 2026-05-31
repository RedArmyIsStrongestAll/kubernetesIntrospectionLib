package kubernetes.introspection.useCases.main.pod.delegate;

import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.main.pod.CurrentPodService;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static kubernetes.introspection.entities.permision.ResourcePermissionEnum.PODS_GET;
import static kubernetes.introspection.entities.permision.ResourcePermissionEnum.PODS_LIST;

@Slf4j
public class CurrentPodServiceConstIpPodExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceConstIpPodExt";

    public CurrentPodServiceConstIpPodExt(KubernetesPodPort podPort, String namespace, String podId) {
        super(podPort, namespace);
        this.podName = podId;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return new ArrayList<>(List.of(PODS_LIST, PODS_GET));
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
    protected PodInfo getPodInfo() throws Exception {
        log.info("Start k8s request");
        return podPort.listAllPods(namespace)
                .stream()
                .filter(p -> podName.equals(p.getPodIP()))
                .findFirst()
                .orElse(null);
    }
}
