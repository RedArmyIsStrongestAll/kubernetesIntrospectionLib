package usesCases.main.replics;

import engine.PodAnalyzer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.kubernetes.Fabric8PodAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.replics.ReplicaPodsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.replics.parent.ReplicaPodsServiceTestAbstract;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ReplicaPodsServiceTest extends ReplicaPodsServiceTestAbstract {

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        kubernetesClient = mockServer.createClient();
        replicaPodsService = new ReplicaPodsService(new Fabric8PodAdapter(kubernetesClient));
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testGetReplicaPodsWithPermissionSuccess() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/labels-pods.yaml");
        Map<String, String> labels = Collections.singletonMap("app", "test-app");
        setupMockServerWithPodsByLabels(podAnalyzer, labels);

        PodInfo currentPod = new Fabric8PodAdapter(kubernetesClient)
                .listPodsByLabels(labels, NAMESPACE).stream()
                .filter(p -> CURRENT_POD_NAME.equals(p.getName()))
                .findFirst()
                .orElseGet(() -> PodInfo.builder().name(CURRENT_POD_NAME).namespace(NAMESPACE).build());

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)
        ));

        OwnerReferenceInfo ownerRef = createOwnerReference(OwnerTypeEnum.DEPLOYMENT, DEPLOYMENT_NAME);
        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT, labels);

        ReplicaPodsService.ReplicaPodsDto result = replicaPodsService.getReplicaPodsWithPermission(ownerRef, ownerDto, currentPod, permission);

        assertNotNull(result);
        assertNotNull(result.getPodInfoList());
        assertEquals(2, result.getPodInfoList().size());

        List<String> podNames = result.getPodInfoList().stream()
                .map(PodInfo::getName)
                .sorted().toList();

        assertTrue(podNames.contains("test-pod-1"));
        assertTrue(podNames.contains("test-pod-2"));
        assertFalse(podNames.contains(CURRENT_POD_NAME));
    }

    @Test
    void testGetReplicaPodsWithPermissionMissingListPermission() throws IOException {
        PodInfo currentPod = PodInfo.builder().name(CURRENT_POD_NAME).namespace(NAMESPACE).build();

        PermissionInfo permission = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, false),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)
        ));

        OwnerReferenceInfo ownerRef = createOwnerReference(OwnerTypeEnum.DEPLOYMENT, DEPLOYMENT_NAME);
        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT);

        Assertions.assertThrows(KubernetesException.class, () ->
                replicaPodsService.getReplicaPodsWithPermission(ownerRef, ownerDto, currentPod, permission)
        );
    }

    @Test
    void testGetReplicaPodsStatefulSetOwner() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/sts-pods.yaml");
        String stsName = "test-sts";
        String podNamePrefix = stsName + "-";
        setupMockServerWithPodsByLabels(podAnalyzer, podNamePrefix);

        PodInfo currentPod = PodInfo.builder()
                .name(STATEFUL_CURRENT_POD_NAME)
                .namespace(NAMESPACE)
                .build();

        OwnerReferenceInfo ownerRef = createOwnerReference(OwnerTypeEnum.STATEFULSET, STATEFUL_NAME);
        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.STATEFULSET, Collections.emptyMap());

        ReplicaPodsService.ReplicaPodsDto result = replicaPodsService.getReplicaPods(ownerRef, ownerDto, currentPod);

        assertNotNull(result);
        List<PodInfo> replicas = result.getPodInfoList();

        replicas.forEach(pod -> {
            assertTrue(pod.getName().startsWith(podNamePrefix));
            assertFalse(isCurrentPod(pod, currentPod));
        });

        assertEquals(2, replicas.size());
    }

    @Test
    void testFindPodsByOwnerSelectorNoPodsFoundWithLabels() throws Exception {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "replics/no-labels-pods-rbac.yaml");
        Map<String, String> labels = Collections.singletonMap("app", "test-app");
        setupMockServerWithPodsByLabels(podAnalyzer, labels);

        PodInfo currentPod = PodInfo.builder()
                .name(NO_LABELS_CURRENT_POD_NAME)
                .namespace(NAMESPACE)
                .build();

        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT, labels);

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
        PodInfo currentPod = PodInfo.builder()
                .name(CURRENT_POD_NAME)
                .namespace(NAMESPACE)
                .build();

        OwnerService.OwnerDto ownerDto = mockOwnerDto(OwnerTypeEnum.DEPLOYMENT, Collections.emptyMap());

        Assertions.assertThrows(KubernetesException.class, () ->
                replicaPodsService.getReplicaPods(
                        createOwnerReference(OwnerTypeEnum.DEPLOYMENT, "test-deploy"),
                        ownerDto,
                        currentPod
                )
        );
    }
}
