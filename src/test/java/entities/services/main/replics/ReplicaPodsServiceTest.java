package entities.services.main.replics;

import engine.PodAnalyzer;
import entities.services.main.replics.parent.ReplicaPodsServiceTestAbstract;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import kubernetes.introspection.entities.services.main.replics.ReplicaPodsService;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelCallChainService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@Slf4j
public class ReplicaPodsServiceTest extends ReplicaPodsServiceTestAbstract {

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        kubernetesClient = mockServer.createClient();

        ownerLabelCallChainService = Mockito.mock(OwnerLabelCallChainService.class);
        replicaPodsService = new ReplicaPodsService(kubernetesClient, ownerLabelCallChainService);

    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testGetReplicaPodsWithPermissionSuccess() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/labels-pods.yaml");
        Pod currentPod = podAnalyzer.getPodByName(CURRENT_POD_NAME, NAMESPACE);

        Map<String, String> labels = Collections.singletonMap("app", "test-app");

        LabelSelector labelSelector = new LabelSelector();
        labelSelector.setMatchLabels(labels);
        when(ownerLabelCallChainService.getSelector(eq(OwnerTypeEnum.DEPLOYMENT), any()))
                .thenReturn(labelSelector);

        setupMockServerWithPodsByLabels(podAnalyzer, labels);

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)
        ));

        OwnerReference ownerRef = createOwnerReference(OwnerTypeEnum.DEPLOYMENT, DEPLOYMENT_NAME);
        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT);

        ReplicaPodsService.ReplicaPodsDto result = replicaPodsService.getReplicaPodsWithPermission(ownerRef, ownerDto, currentPod, permission);

        assertNotNull(result);
        assertNotNull(result.getPodInfoList());
        assertNotNull(result.getK8sPodList());

        assertEquals(2, result.getK8sPodList().size());
        assertEquals(2, result.getPodInfoList().size());

        List<String> podNames = result.getK8sPodList().stream()
                .map(pod -> pod.getMetadata().getName())
                .sorted().toList();

        assertTrue(podNames.contains("test-pod-1"));
        assertTrue(podNames.contains("test-pod-2"));
        assertFalse(podNames.contains(CURRENT_POD_NAME));
    }

    @Test
    void testGetReplicaPodsWithPermissionMissingListPermission() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/labels-pods.yaml");
        Pod currentPod = podAnalyzer.getPodByName(CURRENT_POD_NAME, NAMESPACE);

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, false),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)
        ));

        OwnerReference ownerRef = createOwnerReference(OwnerTypeEnum.DEPLOYMENT, DEPLOYMENT_NAME);
        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT);

        Assertions.assertThrows(KubernetesException.class, () ->
                replicaPodsService.getReplicaPodsWithPermission(ownerRef, ownerDto, currentPod, permission)
        );
    }

    @Test
    void testGetReplicaPodsStatefulSetOwner() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/sts-pods.yaml");
        Pod currentPod = podAnalyzer.getPodByName(STATEFUL_CURRENT_POD_NAME, NAMESPACE);

        String stsName = "test-sts";
        OwnerReference ownerRef = createOwnerReference(OwnerTypeEnum.STATEFULSET, STATEFUL_NAME);
        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.STATEFULSET);

        String podNamePrefix = stsName + "-";
        setupMockServerWithPodsByLabels(podAnalyzer, podNamePrefix);

        ReplicaPodsService.ReplicaPodsDto result = replicaPodsService.getReplicaPods(ownerRef, ownerDto, currentPod);

        assertNotNull(result);
        List<Pod> replicas = result.getK8sPodList();

        replicas.forEach(pod -> {
            assertTrue(pod.getMetadata().getName().startsWith(podNamePrefix));
            assertFalse(isCurrentPod(pod, currentPod));
        });

        assertEquals(2, replicas.size());
    }

    @Test
    void testFindPodsByOwnerSelectorNoPodsFoundWithLabels() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/no-labels-pods-rbac.yaml");
        Pod currentPod = podAnalyzer.getPodByName(NO_LABELS_CURRENT_POD_NAME, NAMESPACE);

        Map<String, String> labels = Collections.singletonMap("app", "test-app");
        LabelSelector labelSelector = new LabelSelector();
        labelSelector.setMatchLabels(labels);
        when(ownerLabelCallChainService.getSelector(eq(OwnerTypeEnum.DEPLOYMENT), any()))
                .thenReturn(labelSelector);

        setupMockServerWithPodsByLabels(podAnalyzer, labels);

        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT);

        Assertions.assertThrows(KubernetesException.class, () ->
                replicaPodsService.getReplicaPods(
                        createOwnerReference(OwnerTypeEnum.DEPLOYMENT, "test-deploy"),
                        ownerDto,
                        currentPod
                )
        );
    }

    @Test
    void testFindPodsByOwnerSelector_SelectorIsNull() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/labels-pods.yaml");
        Pod currentPod = podAnalyzer.getPodByName(CURRENT_POD_NAME, NAMESPACE);

        when(ownerLabelCallChainService.getSelector(eq(OwnerTypeEnum.DEPLOYMENT), any()))
                .thenReturn(null);

        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT);

        Assertions.assertThrows(KubernetesException.class, () ->
                replicaPodsService.getReplicaPods(
                        createOwnerReference(OwnerTypeEnum.DEPLOYMENT, "test-deploy"),
                        ownerDto,
                        currentPod
                )
        );
    }
}