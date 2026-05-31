package usesCases.main.replics.parent;

import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.owner.OwnerInfo;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.replics.ReplicaPodsService;
import lombok.extern.slf4j.Slf4j;
import usesCases.utils.KubernetesYamlUtils;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class ReplicaPodsServiceTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String DEPLOYMENT_NAME = "test-deployment";
    protected static final String CURRENT_POD_NAME = "test-pod-0";

    protected static final String STATEFUL_NAME = "test-sts";
    protected static final String STATEFUL_CURRENT_POD_NAME = "test-sts-0";

    protected static final String NO_LABELS_CURRENT_POD_NAME = "pod-without-labels-0";

    protected KubernetesMockServer mockServer;
    protected KubernetesClient kubernetesClient;
    protected ReplicaPodsService replicaPodsService;

    protected PodAnalyzer getPodAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String podYaml = KubernetesYamlUtils.loadRbacYaml(podFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(KubernetesYamlUtils.loadRbacYaml(rbacFilename));
        return new PodAnalyzer(podYaml, rbacAnalyzer);
    }

    protected OwnerReferenceInfo createOwnerReference(OwnerTypeEnum type, String name) {
        return OwnerReferenceInfo.builder()
                .kind(type.getOriginalName())
                .name(name)
                .build();
    }

    protected OwnerService.OwnerDto mockOwnerDto(OwnerTypeEnum type, Map<String, String> selector) {
        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(type)
                .name(type == OwnerTypeEnum.STATEFULSET ? STATEFUL_NAME : DEPLOYMENT_NAME)
                .exists(true)
                .selector(selector)
                .build();
        return new OwnerService.OwnerDto(ownerInfo, type);
    }

    protected OwnerService.OwnerDto mockOwnerDto(OwnerTypeEnum type) {
        return mockOwnerDto(type, Map.of("app", "test-app"));
    }

    protected boolean isCurrentPod(PodInfo pod, PodInfo currentPod) {
        return currentPod.getName().equals(pod.getName()) &&
                currentPod.getNamespace().equals(pod.getNamespace());
    }

    protected void setupMockServerWithError() {
    }

    protected void setupMockServerWithPodsByLabels(PodAnalyzer analyzer, String podNamePrefix) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods")
                .andReply(200, (request) -> {
                    log.info("Received LIST request for pods with podNamePrefix: {}", podNamePrefix);
                    return analyzer.listPodsByPrefix(NAMESPACE, podNamePrefix);
                })
                .always();
    }

    protected void setupMockServerWithPodsByLabels(PodAnalyzer analyzer, Map<String, String> labels) {
        String labelSelectorQuery = KubernetesYamlUtils.buildLabelSelectorQuery(labels);
        String path = "/api/v1/namespaces/" + NAMESPACE + "/pods?labelSelector=" + labelSelectorQuery;

        mockServer.expect()
                .get()
                .withPath(path)
                .andReply(200, request -> {
                    log.info("Received LIST request for pods with labels: {}", labels);
                    return analyzer.getPodListByLabels(labels, NAMESPACE);
                })
                .always();
    }
}
