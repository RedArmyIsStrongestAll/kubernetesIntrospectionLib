package entities.services.main.source;

import engine.ConfigMapAnalyzer;
import engine.PodAnalyzer;
import entities.services.main.source.parent.ConfigMapSourceServiceTestAbstract;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.source.ConfigSourceInfo;
import kubernetes.introspection.entities.models.source.ConfigUsageTypeEnum;
import kubernetes.introspection.entities.services.main.source.ConfigMapSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigMapSourceServiceTest extends ConfigMapSourceServiceTestAbstract {

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();

        configMapSourceService = new ConfigMapSourceService(mockServer.createClient(), NAMESPACE);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testGetConfigMapSourcesWithPermission_success() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "source/configmap-pod-test.yaml");
        ConfigMapAnalyzer configMapAnalyzer = getConfigMapAnalyzer("rbac/test-rbac.yaml", "source/configmap-pod-test.yaml");

        setupMockServerWithPodsByName(POD_NAME, podAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_ENV_NAME, configMapAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_ENVFROM_NAME_1, configMapAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_ENVFROM_NAME_2, configMapAnalyzer);
        setupMockServerWithConfigMapByName(CONFIGMAP_VOLUME_NAME_3, configMapAnalyzer);

        Pod pod = podAnalyzer.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(pod);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CONFIGMAPS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CONFIGMAPS_LIST, true)
        ));

        ConfigMapSourceService.ConfigMapDto dto = configMapSourceService.getConfigMapSourcesWithPermission(pod, permissionInfo);

        assertNotNull(dto);
        assertNotNull(dto.getK8sConfigMapList());
        assertNotNull(dto.getConfigSourceInfoList());

        List<ConfigMap> configMaps = dto.getK8sConfigMapList();
        List<ConfigSourceInfo> sourceInfos = dto.getConfigSourceInfoList();
        assertEquals(4, configMaps.size());
        assertEquals(4, sourceInfos.size());
        assertSourceInfoExists(sourceInfos, "env-config", ConfigUsageTypeEnum.ENV, List.of("key1"));
        assertSourceInfoExists(sourceInfos, "envfrom-config1", ConfigUsageTypeEnum.ENV_FROM, List.of("key2", "key3"));
        assertSourceInfoExists(sourceInfos, "envfrom-config2", ConfigUsageTypeEnum.ENV_FROM, List.of("key4", "key5"));
        assertSourceInfoExists(sourceInfos, "volume-config", ConfigUsageTypeEnum.VOLUME, List.of("key6"));
    }

}