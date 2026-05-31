package usesCases.main.source;

import engine.PodAnalyzer;
import engine.SecretAnalyzer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.kubernetes.Fabric8ConfigAdapter;
import kubernetes.introspection.adapters.kubernetes.Fabric8PodAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.entities.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.main.source.SecretSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.source.parent.SecretSourceServiceTestAbstract;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SecretSourceServiceTest extends SecretSourceServiceTestAbstract {

    private Fabric8PodAdapter podAdapter;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        secretSourceService = new SecretSourceService(new Fabric8ConfigAdapter(mockServer.createClient()), NAMESPACE);
        podAdapter = new Fabric8PodAdapter(mockServer.createClient());
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void testGetSecretSourcesWithPermission_success() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "source/secret-pod-test.yaml");
        SecretAnalyzer secretAnalyzer = getSecretAnalyzer("rbac/test-rbac.yaml", "source/secret-pod-test.yaml");

        setupMockServerWithPodsByName(POD_NAME, podAnalyzer);
        setupMockServerWithSecretByName(SECRET_ENV_NAME, secretAnalyzer);
        setupMockServerWithSecretByName(SECRET_ENVFROM_NAME_1, secretAnalyzer);
        setupMockServerWithSecretByName(SECRET_ENVFROM_NAME_2, secretAnalyzer);
        setupMockServerWithSecretByName(SECRET_VOLUME_NAME_3, secretAnalyzer);

        PodInfo podInfo = podAdapter.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(podInfo);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_LIST, true)
        ));

        SecretSourceService.SecretDto dto = secretSourceService.getSecretSourcesWithPermission(podInfo, permissionInfo);
        assertNotNull(dto);
        assertNotNull(dto.getConfigSourceInfoList());

        List<ConfigSourceInfo> sourceInfos = dto.getConfigSourceInfoList();
        assertEquals(4, sourceInfos.size());

        assertSourceInfoExists(sourceInfos, "secret-env", ConfigUsageTypeEnum.ENV, List.of("key1"));
        assertSourceInfoExists(sourceInfos, "secret-envfrom1", ConfigUsageTypeEnum.ENV_FROM, List.of("key2", "key3"));
        assertSourceInfoExists(sourceInfos, "secret-envfrom2", ConfigUsageTypeEnum.ENV_FROM, List.of("key4", "key5"));
        assertSourceInfoExists(sourceInfos, "secret-volume", ConfigUsageTypeEnum.VOLUME, List.of("key6"));
    }

    @Test
    void testMapToConfigSourceInfo_correctlyMapsSingleSecret() {
        ConfigSourceInfo info = SecretSourceService.mapToConfigSourceInfo(
                "test-secret", List.of("key1", "key2"), ConfigUsageTypeEnum.ENV);

        assertNotNull(info);
        assertEquals("test-secret", info.getName());
        assertEquals(ConfigSourceTypeEnum.SECRET, info.getType());
        assertEquals(ConfigUsageTypeEnum.ENV, info.getUsageType());
        assertEquals(2, info.getKeys().size());
        assertTrue(info.getKeys().containsAll(List.of("key1", "key2")));
    }

    @Test
    void testGetSecretSources_throwsExceptionIfNoSecretsUsed() {
        PodInfo podInfo = PodInfo.builder()
                .name("test-pod")
                .namespace(NAMESPACE)
                .secretRefs(Map.of())
                .build();

        assertThrows(KubernetesException.class, () ->
                secretSourceService.getSecretSources(podInfo));
    }

    @Test
    void testGetSecretSourcesWithPermission_deniesAccessIfMissingPermission() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "source/secret-pod-test.yaml");
        setupMockServerWithPodsByName(POD_NAME, podAnalyzer);

        PodInfo podInfo = podAdapter.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(podInfo);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_LIST, false)
        ));

        assertThrows(KubernetesException.class, () ->
                secretSourceService.getSecretSourcesWithPermission(podInfo, permissionInfo));
    }
}
