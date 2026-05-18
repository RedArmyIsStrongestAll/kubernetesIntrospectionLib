package usesCases.main.source;

import engine.PodAnalyzer;
import engine.SecretAnalyzer;
import usesCases.main.source.parent.SecretSourceServiceTestAbstract;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.entities.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.main.source.SecretSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecretSourceServiceTest extends SecretSourceServiceTestAbstract {

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        secretSourceService = new SecretSourceService(mockServer.createClient(), NAMESPACE);
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

        Pod pod = podAnalyzer.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(pod);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_LIST, true)
        ));

        SecretSourceService.SecretDto dto = secretSourceService.getSecretSourcesWithPermission(pod, permissionInfo);
        assertNotNull(dto);
        assertNotNull(dto.getK8sSecretList());
        assertNotNull(dto.getConfigSourceInfoList());

        List<Secret> secrets = dto.getK8sSecretList();
        List<ConfigSourceInfo> sourceInfos = dto.getConfigSourceInfoList();

        assertEquals(4, secrets.size());
        assertEquals(4, sourceInfos.size());

        assertSourceInfoExists(sourceInfos, "secret-env", ConfigUsageTypeEnum.ENV, List.of("key1"));
        assertSourceInfoExists(sourceInfos, "secret-envfrom1", ConfigUsageTypeEnum.ENV_FROM, List.of("key2", "key3"));
        assertSourceInfoExists(sourceInfos, "secret-envfrom2", ConfigUsageTypeEnum.ENV_FROM, List.of("key4", "key5"));
        assertSourceInfoExists(sourceInfos, "secret-volume", ConfigUsageTypeEnum.VOLUME, List.of("key6"));
    }

    @Test
    void testMapToConfigSourceInfo_correctlyMapsSingleSecret() {
        Secret secret = new Secret();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("test-secret");
        metadata.setNamespace(NAMESPACE);
        secret.setMetadata(metadata);

        Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        secret.setData(data);

        ConfigSourceInfo info = SecretSourceService.mapToConfigSourceInfo(secret, ConfigUsageTypeEnum.ENV);

        assertNotNull(info);
        assertEquals("test-secret", info.getName());
        assertEquals(ConfigSourceTypeEnum.SECRET, info.getType());
        assertEquals(ConfigUsageTypeEnum.ENV, info.getUsageType());
        assertEquals(2, info.getKeys().size());
        assertTrue(info.getKeys().containsAll(List.of("key1", "key2")));
    }

    @Test
    void testGetSecretSources_throwsExceptionIfNoSecretsUsed() {
        Pod pod = new Pod();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("test-pod");
        metadata.setNamespace(NAMESPACE);
        pod.setMetadata(metadata);

        KubernetesException exception = assertThrows(KubernetesException.class, () ->
                secretSourceService.getSecretSources(pod)
        );
    }

    @Test
    void testGetSecretSourcesWithPermission_deniesAccessIfMissingPermission() throws IOException {
        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "source/secret-pod-test.yaml");
        SecretAnalyzer secretAnalyzer = getSecretAnalyzer("rbac/test-rbac.yaml", "source/secret-pod-test.yaml");

        setupMockServerWithPodsByName(POD_NAME, podAnalyzer);
        setupMockServerWithSecretByName(SECRET_ENV_NAME, secretAnalyzer);

        Pod pod = podAnalyzer.getPodByName(POD_NAME, NAMESPACE);
        assertNotNull(pod);

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.SECRETS_LIST, false)
        ));

        KubernetesException exception = assertThrows(KubernetesException.class, () ->
                secretSourceService.getSecretSourcesWithPermission(pod, permissionInfo)
        );
    }
}