package entities.services.main.pod;

import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.pod.PodInfo;
import kubernetes.introspection.entities.services.env.EnvironmentProviderSystemImpl;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstDownwardApiExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class CurrentPodServiceConstDownwardApiExtTest {

    private static final String POD_NAME = "test-pod";
    private static final String MISTAKE_POD_NAME = "no-test-pod";
    private static final String NAMESPACE = "test-namespace";

    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;

    EnvironmentProviderSystemImpl mockProvider;

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
        mockProvider = mock(EnvironmentProviderSystemImpl.class);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void getCurrentPodInfoWithCheckPermissionsValidTest() throws Exception {
        CurrentPodService service = new CurrentPodServiceConstDownwardApiExt(client, NAMESPACE, mockProvider);
        when(mockProvider.getPodName()).thenReturn(POD_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)));

        PodAnalyzer podAnalyzer = getPodAnalyzer("rbac/test-rbac.yaml", "pod/test-pod.yaml");
        setupMockServerWithPodAnalyzer(podAnalyzer, permission);

        PodInfo pod = service.getCurrentPodInfoWithCheckPermissions(permission);
        log.info("Test result: {}", pod);

        Assertions.assertNotNull(pod);
    }

    public PodAnalyzer getPodAnalyzer(String rbacFilename, String podFilename) throws IOException {
        String yamlRbacContent = loadRbacYaml(rbacFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(yamlRbacContent);

        String yamlPodContent = loadRbacYaml(podFilename);

        return new PodAnalyzer(yamlPodContent, rbacAnalyzer);
    }

    protected void setupMockServerWithPodAnalyzer(PodAnalyzer analyzer, PermissionInfo permissionApp) {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + POD_NAME)
                .andReply(200, request -> {
                    log.info("Received GET request for pod: {}/{}", NAMESPACE, POD_NAME);
                    return analyzer.getPodByName(permissionApp, POD_NAME, NAMESPACE);
                })
                .always();
    }

    private String loadRbacYaml(String filename) throws IOException {
        try {
            URL resource = getClass().getClassLoader().getResource(filename);
            if (resource != null) {
                File file = new File(resource.getFile());
                if (file.exists()) {
                    return new String(Files.readAllBytes(file.toPath()));
                }
            }
            throw new IOException("No " + filename + " file found");
        } catch (Exception e) {
            log.error("No have yaml file, using default RBAC rules: ", e);
            throw e;
        }
    }

}
