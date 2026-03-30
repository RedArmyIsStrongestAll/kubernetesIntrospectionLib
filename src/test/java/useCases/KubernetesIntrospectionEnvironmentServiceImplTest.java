package useCases;

import engine.ConfigMapAnalyzer;
import engine.EndpointAnalyzer;
import engine.OwnerAnalyzer;
import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import engine.SecretAnalyzer;
import engine.ServiceAnalyzer;
import engine.owners.OwnerAnalyzerDeployment;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.entities.services.env.EnvironmentProviderSystemImpl;
import kubernetes.introspection.entities.services.env.GetVarsServicesDtoService;
import kubernetes.introspection.entities.services.init.InitDetectorService;
import kubernetes.introspection.useCases.KubernetesIntrospectionEnvironmentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static entities.services.utils.KubernetesYamlUtils.loadRbacYaml;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class KubernetesIntrospectionEnvironmentServiceImplTest {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String POD_NAME = "test-pod";
    private static final String DEPLOYMENT_NAME = "test-deployment";
    protected static final String SERVICE_NAME = "test-service";
    protected static final String CONFIGMAP_NAME = "test-configmap";
    protected static final String SECRET_NAME = "test-secret";


    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;

    protected EnvironmentProviderSystemImpl mockProvider;

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
    public void inCluster_Pod_ConstDownwardApi_Owner_Deployment_Service_Endpoints_ConfigMap_Secrets_Test() throws Exception {
        String yamlPath = "commonsIntegration/deployment-all.yaml";
        String yamlRbacPath = "commonsIntegration/deployment-all-rbac.yaml";

        //InitDetectorService
        InitDetectorService mockInitService = getInitDetectorService();

        //InitPermissionService
        RbacAnalyzer rbacAnalyzer = getRbacAnalyzer(yamlRbacPath);
        setupMockServerWithRbacAnalyzer(rbacAnalyzer);

        //CurrentPodServiceConstDownward
        when(mockProvider.getPodName()).thenReturn(POD_NAME);
        PodAnalyzer podAnalyzer = getPodAnalyzer(rbacAnalyzer, yamlPath);
        setupMockServerWithPodByName(podAnalyzer, POD_NAME);

        //OwnerReferenceService - yaml настройка
        //должен быть
        //metadata.getOwnerReferences().stream()
        //                    .filter(ref -> Boolean.TRUE.equals(ref.getController()))
        //                    .findFirst()
        //                    .orElse(metadata.getOwnerReferences().get(0));
        //и он должен быть Deployment

        //OwnerService
        OwnerAnalyzerDeployment ownerEngine = (OwnerAnalyzerDeployment) getOwnerAnalyzer(rbacAnalyzer, yamlPath, OwnerAnalyzerDeployment.class);
        setupMockServerWithOwnerDeploymentByName(ownerEngine, DEPLOYMENT_NAME);

        //OwnerLabelService - yaml настройка
        //deployment.getSpec().getSelector();
        //должен вернуть что у пода были labels {app=test-app}

        //ReplicaPodsService
        Map<String, String> podLabels = Collections.singletonMap("app", "test-app");
        setupMockServerWithPodsByLabels(podAnalyzer, podLabels);

        //ServiceService
        ServiceAnalyzer serviceAnalyzer = getServiceAnalyzer(rbacAnalyzer, yamlPath);
        setupMockServerWithServiceList(serviceAnalyzer);
        //у сервиса должен быть селектор который закрывает лейблы пода labels {app=test-app}

        //ServiceEndpoint
        EndpointAnalyzer endpointAnalyzer = getEndpointAnalyzer(rbacAnalyzer, yamlPath);
        setupMockServerWithEndpoints(endpointAnalyzer, SERVICE_NAME);
        //должно совпать с именем Service

        //ConfigMapService
        ConfigMapAnalyzer configMapAnalyzer = getConfigMapAnalyzer(rbacAnalyzer, yamlPath);
        setupMockServerWithConfigMapByName(CONFIGMAP_NAME, configMapAnalyzer);

        //SecretService
        SecretAnalyzer secretAnalyzer = getSecretAnalyzer(rbacAnalyzer, yamlPath);
        setupMockServerWithSecretByName(SECRET_NAME, secretAnalyzer);

        KubernetesIntrospectionEnvironmentServiceImpl service = new KubernetesIntrospectionEnvironmentServiceImpl(mockInitService);
        GetVarsServicesDtoService vars = new GetVarsServicesDtoService(mockProvider, null, null, null);
        KubernetesEnvironmentInfo result = service.getKubernetesEnvironmentInfo(vars);

        Assertions.assertNotNull(result);
    }

    protected InitDetectorService getInitDetectorService() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return "1.2.3.4";
            }

            @Override
            public boolean isFileExists(Path path) {
                return true;
            }

            @Override
            public String readFileContent(Path path) throws IOException {
                return "test-namespace";
            }

        };

        return testService;
    }


    protected RbacAnalyzer getRbacAnalyzer(String rbacFilename) throws IOException {
        String rbacYaml = KubernetesYamlUtils.loadRbacYaml(rbacFilename);
        return new RbacAnalyzer(rbacYaml);
    }

    protected void setupMockServerWithRbacAnalyzer(RbacAnalyzer analyzer) {
        mockServer.expect().post()
                .withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
                .andReply(200, request ->
                {
                    String body = request.getBody().readUtf8();
                    log.info("Received SSAR request: {}", body);

                    String resource = extractJsonValue(body, "resource");
                    String verb = extractJsonValue(body, "verb");
                    String namespace = extractJsonValue(body, "namespace");
                    log.info("SSAR request for: \nResource: {}, \nVerb: {}, \nNamespace: {}", resource, verb, namespace);

                    boolean allowed = analyzer.isAllowed(resource, verb, namespace);
                    String reason = allowed ? "" : "RBAC: access denied";
                    log.info("RbacAnalyzer result: {}", allowed);

                    return createSsarResponse(allowed, reason);
                })
                .always();
    }

    protected String extractJsonValue(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\":\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    protected String createSsarResponse(boolean allowed, String reason) {
        return String.format("""
                {
                  "apiVersion": "authorization.k8s.io/v1",
                  "kind": "SelfSubjectAccessReview",
                  "status": {
                    "allowed": %s,
                    "reason": "%s"
                  }
                }
                """, allowed, reason.replace("\"", "\\\""));
    }


    protected PodAnalyzer getPodAnalyzer(RbacAnalyzer rbacAnalyzer, String podFilename) throws IOException {
        String yamlPodContent = loadRbacYaml(podFilename);
        return new PodAnalyzer(yamlPodContent, rbacAnalyzer);
    }

    protected void setupMockServerWithPodByName(PodAnalyzer analyzer, String podName) {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + podName)
                .andReply(200, request -> {
                    log.info("Received GET request for pod: {}/{}", NAMESPACE, podName);
                    return analyzer.getPodByName(podName, NAMESPACE);
                })
                .always();
    }


    protected OwnerAnalyzer getOwnerAnalyzer(RbacAnalyzer rbacAnalyzer, String ownerFilename, Class<? extends OwnerAnalyzer> analyzerClass) throws Exception {
        String yamlContent = loadRbacYaml(ownerFilename);

        Constructor<? extends OwnerAnalyzer> constructor = analyzerClass.getConstructor(String.class, RbacAnalyzer.class);
        return constructor.newInstance(yamlContent, rbacAnalyzer);
    }

    protected void setupMockServerWithOwnerDeploymentByName(OwnerAnalyzerDeployment analyzer, String ownerName) {
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/deployments/" + ownerName)
                .andReply(200, request -> {
                            log.info("Received GET request for owner: {}/{}", NAMESPACE, ownerName);
                            return analyzer.getOwner(ownerName, NAMESPACE);
                        }
                )
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


    protected ServiceAnalyzer getServiceAnalyzer(RbacAnalyzer rbacAnalyzer, String serviceFilename) throws IOException {
        String serviceYaml = KubernetesYamlUtils.loadRbacYaml(serviceFilename);

        return new ServiceAnalyzer(serviceYaml, rbacAnalyzer);
    }

    protected void setupMockServerWithServiceList(ServiceAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/services")
                .andReply(200, (requet) -> {
                    log.info("Received LIST request for services in namespace: {}", NAMESPACE);
                    return analyzer.listAllServices(NAMESPACE);
                })
                .always();
    }


    protected EndpointAnalyzer getEndpointAnalyzer(RbacAnalyzer rbacAnalyzer, String endpointsFile) throws IOException {
        String endpointsYaml = KubernetesYamlUtils.loadRbacYaml(endpointsFile);
        return new EndpointAnalyzer(endpointsYaml, rbacAnalyzer);
    }

    protected void setupMockServerWithEndpoints(EndpointAnalyzer analyzer, String serviceName) {
        String fieldSelectorQuery = KubernetesYamlUtils.buildFieldSelectorQuery("metadata.name", serviceName);
        String path = "/api/v1/namespaces/" + NAMESPACE + "/endpoints?fieldSelector=" + fieldSelectorQuery;
        mockServer.expect()
                .get()
                .withPath(path)
                .andReply(200, request -> {
                    log.info("Received LIST request for endpoints with serviceName: {}", serviceName);
                    return analyzer.listEndpointsByServiceName(serviceName, NAMESPACE);
                })
                .always();
    }

    protected ConfigMapAnalyzer getConfigMapAnalyzer(RbacAnalyzer rbacAnalyzer, String cmFilename) throws IOException {
        String cmYaml = KubernetesYamlUtils.loadRbacYaml(cmFilename);
        return new ConfigMapAnalyzer(cmYaml, rbacAnalyzer);
    }

    protected void setupMockServerWithConfigMapByName(String confiMapName, ConfigMapAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/configmaps/" + confiMapName)
                .andReply(200, (req) -> {
                    log.info("Received ConfigMap request in namespace: {}", NAMESPACE);
                    return analyzer.getConfigMapByName(confiMapName, NAMESPACE);
                })
                .always();
    }


    protected SecretAnalyzer getSecretAnalyzer(RbacAnalyzer rbacAnalyzer, String secretFilename) throws IOException {
        String secretYaml = KubernetesYamlUtils.loadRbacYaml(secretFilename);
        return new SecretAnalyzer(secretYaml, rbacAnalyzer);
    }

    protected void setupMockServerWithSecretByName(String secretName, SecretAnalyzer analyzer) {
        mockServer.expect()
                .get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/secrets/" + secretName)
                .andReply(200, (req) -> {
                    log.info("Received Secret request in namespace: {}", NAMESPACE);
                    return analyzer.getSecretByName(secretName, NAMESPACE);
                })
                .always();
    }
}
