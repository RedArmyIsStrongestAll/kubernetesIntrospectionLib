package entities.services.main.replics.parent;

import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import kubernetes.introspection.entities.services.main.replics.ReplicaPodsService;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelCallChainService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    protected OwnerLabelCallChainService ownerLabelCallChainService;

    protected PodAnalyzer getPodAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String podYaml = KubernetesYamlUtils.loadRbacYaml(podFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(KubernetesYamlUtils.loadRbacYaml(rbacFilename));
        return new PodAnalyzer(podYaml, rbacAnalyzer);
    }


    protected OwnerReference createOwnerReference(OwnerTypeEnum type, String name) {
        OwnerReference ref = new OwnerReference();
        ref.setKind(type.getOriginalName());
        ref.setName(name);
        return ref;
    }

    protected OwnerService.OwnerDto mockOwnerDto(OwnerTypeEnum type) {
        OwnerService.OwnerDto ownerDto = mock(OwnerService.OwnerDto.class);
        when(ownerDto.getK8sType()).thenReturn(type);
        when(ownerDto.getK8sObject()).thenReturn(mock(HasMetadata.class));
        return ownerDto;
    }

    protected boolean isCurrentPod(Pod pod, Pod currentPod) {
        return currentPod.getMetadata().getName().equals(pod.getMetadata().getName()) &&
                currentPod.getMetadata().getNamespace().equals(pod.getMetadata().getNamespace());
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
