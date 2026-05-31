package usesCases.main.source;

import engine.ConfigMapAnalyzer;
import engine.PodAnalyzer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.Fabric8ConfigAdapter;
import kubernetes.introspection.adapters.Fabric8PodAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.entities.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.main.source.ConfigMapSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.source.parent.ConfigMapSourceServiceTestAbstract;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigMapSourceServiceTest extends ConfigMapSourceServiceTestAbstract {

    private Fabric8PodAdapter podAdapter;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();

        configMapSourceService = new ConfigMapSourceService(new Fabric8ConfigAdapter(mockServer.createClient()), NAMESPACE);
        podAdapter = new Fabric8PodAdapter(mockServer.createClient());
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testGetConfigMapSourcesWithPermissionSuccess() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "source/configmap-pod-test.yaml");
        ConfigMapAnalyzer configMapAnalyzer = getConfigMapAnalyzer("rbac/test-rbac.yaml", "source/configmap-pod-test.yaml");

        setupMockServerWithPodsByName(POD_NAME, podAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_ENV_NAME, configMapAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_ENVFROM_NAME_1, configMapAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_ENVFROM_NAME_2, configMapAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_VOLUME_NAME_3, configMapAnalyzer);

        PodInfo podInfo = podAdapter.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(podInfo);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CONFIGMAPS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CONFIGMAPS_LIST, true)
        ));

        ConfigMapSourceService.ConfigMapDto dto = configMapSourceService.getConfigMapSourcesWithPermission(podInfo, permissionInfo);

        assertNotNull(dto);
        assertNotNull(dto.getConfigSourceInfoList());

        List<ConfigSourceInfo> sourceInfos = dto.getConfigSourceInfoList();
        assertEquals(4, sourceInfos.size());
        assertSourceInfoExists(sourceInfos, "env-config", ConfigUsageTypeEnum.ENV, List.of("key1"));
        assertSourceInfoExists(sourceInfos, "envfrom-config1", ConfigUsageTypeEnum.ENV_FROM, List.of("key2", "key3"));
        assertSourceInfoExists(sourceInfos, "envfrom-config2", ConfigUsageTypeEnum.ENV_FROM, List.of("key4", "key5"));
        assertSourceInfoExists(sourceInfos, "volume-config", ConfigUsageTypeEnum.VOLUME, List.of("key6"));
    }

    @Test
    void testMapToConfigSourceInfoCorrectlyMapsSingleConfigMap() {
        ConfigSourceInfo info = ConfigMapSourceService.mapToConfigSourceInfo(
                "test-configmap", List.of("key1", "key2", "binKey"), ConfigUsageTypeEnum.ENV);

        assertNotNull(info);
        assertEquals("test-configmap", info.getName());
        assertEquals(ConfigSourceTypeEnum.CONFIG_MAP, info.getType());
        assertEquals(ConfigUsageTypeEnum.ENV, info.getUsageType());
        assertEquals(3, info.getKeys().size());
        assertTrue(info.getKeys().containsAll(List.of("key1", "key2", "binKey")));
    }

    @Test
    void testGetConfigMapSources_throwsExceptionIfNoConfigMapsUsed() {
        PodInfo podInfo = PodInfo.builder()
                .name("test-pod")
                .namespace(NAMESPACE)
                .configMapRefs(Map.of())
                .build();

        assertThrows(KubernetesException.class, () ->
                configMapSourceService.getConfigMapSources(podInfo));
    }

    @Test
    void testGetConfigMapSourcesWithPermission_deniesAccessIfMissingPermission() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "source/configmap-pod-test.yaml");
        setupMockServerWithPodsByName(POD_NAME, podAnalyzer);

        PodInfo podInfo = podAdapter.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(podInfo);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CONFIGMAPS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CONFIGMAPS_LIST, false)
        ));

        assertThrows(KubernetesException.class, () ->
                configMapSourceService.getConfigMapSourcesWithPermission(podInfo, permissionInfo));
    }
}
