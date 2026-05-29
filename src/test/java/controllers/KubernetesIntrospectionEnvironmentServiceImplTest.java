package controllers;

import engine.ConfigMapAnalyzer;
import engine.EndpointAnalyzer;
import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import engine.SecretAnalyzer;
import engine.ServiceAnalyzer;
import engine.owners.OwnerAnalyzerDeployment;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.useCases.env.EnvironmentProviderSystemImpl;
import kubernetes.introspection.useCases.env.GetVarsServicesDtoService;
import kubernetes.introspection.useCases.env.KubernetesFileReadServiceFileImpl;
import kubernetes.introspection.useCases.init.InitDetectorService;
import kubernetes.introspection.controllers.KubernetesIntrospectionEnvironmentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class KubernetesIntrospectionEnvironmentServiceImplTest extends KubernetesIntrospectionEnvironmentServiceImplTestAbstract {

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
        mockEnvProvider = mock(EnvironmentProviderSystemImpl.class);
        mockFileReadService = mock(KubernetesFileReadServiceFileImpl.class);
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    public void inCluster_Pod_ConstDownwardApi_Owner_Deployment_Service_Endpoints_ConfigMap_Secrets_Test() throws Exception {
        String yamlPath = "commons.Integration/deployment-all.yaml";
        String yamlRbacPath = "commons.Integration/deployment-all-rbac.yaml";

        //InitDetectorService
        InitDetectorService mockInitService = new InitDetectorService(mockFileReadService);
        when(mockFileReadService.getKubernetesHostEnv()).thenReturn("1.1.1.1");
        when(mockFileReadService.tokenExists()).thenReturn(true);
        when(mockFileReadService.namespaceExists()).thenReturn(true);
        when(mockFileReadService.getNamespace()).thenReturn(NAMESPACE);

        //InitPermissionService
        RbacAnalyzer rbacAnalyzer = getRbacAnalyzer(yamlRbacPath);
        setupMockServerWithRbacAnalyzer(rbacAnalyzer);

        //CurrentPodServiceConstDownward
        when(mockEnvProvider.getPodName()).thenReturn(POD_NAME);
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
        GetVarsServicesDtoService vars = new GetVarsServicesDtoService(mockEnvProvider, null, null, null);
        KubernetesEnvironmentInfo result = service.getKubernetesEnvironmentInfoWithClient(vars, client);

        System.out.println("\n\n\n\n\n" + "RESPONSE:\n" + result + "\n\n\n\n\n");

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getCurrentPod(), "currentPod должен быть найден");
        Assertions.assertEquals(POD_NAME, result.getCurrentPod().getName());

        Assertions.assertNotNull(result.getOwner(), "owner должен быть найден");
        Assertions.assertEquals(DEPLOYMENT_NAME, result.getOwner().getName());

        Assertions.assertNotNull(result.getReplicaPods(), "replicaPods не должны быть null");
        Assertions.assertFalse(result.getReplicaPods().isEmpty(), "replicaPods должны содержать хотя бы одну реплику");

        Assertions.assertNotNull(result.getServices(), "service должен быть найден");
        Assertions.assertEquals(SERVICE_NAME, result.getServices().getName());

        Assertions.assertNotNull(result.getConfigSources(), "configSources не должны быть null");
        Assertions.assertFalse(result.getConfigSources().isEmpty(), "configSources должны содержать хотя бы один источник");

        long errors = result.getErrors() == null ? 0 :
                result.getErrors().stream()
                        .filter(e -> e.getErrorCodeEnum().isCritical())
                        .count();
        Assertions.assertEquals(0, errors, "Не должно быть критических ошибок: " + result.getErrors());
    }
}
