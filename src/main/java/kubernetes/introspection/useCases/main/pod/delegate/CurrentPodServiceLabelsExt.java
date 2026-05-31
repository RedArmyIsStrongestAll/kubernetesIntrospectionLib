package kubernetes.introspection.useCases.main.pod.delegate;

import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.main.pod.CurrentPodService;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.permision.ResourcePermissionEnum.PODS_GET;
import static kubernetes.introspection.entities.permision.ResourcePermissionEnum.PODS_LIST;

@Slf4j
public class CurrentPodServiceLabelsExt extends CurrentPodService {

    private static final String CURRENT_POD_SERVICE_NAME = "CurrentPodServiceLabelsExt";
    private final List<String> labelList;

    public CurrentPodServiceLabelsExt(KubernetesPodPort podPort, String namespace, List<String> labelList) {
        super(podPort, namespace);
        this.labelList = labelList;
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
        if (labelList != null && !labelList.isEmpty()) {
            return "The search will be by labels: " + String.join(", ", labelList);
        }
        return null;
    }

    @Override
    protected PodInfo getPodInfo() throws Exception {
        Map<String, String> labels = labelList.stream()
                .filter(s -> s.contains("="))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));

        log.info("Start k8s request");
        List<PodInfo> pods = podPort.listPodsByLabels(labels, namespace);

        if (pods.size() != 1) {
            return null;
        }
        return pods.get(0);
    }
}
