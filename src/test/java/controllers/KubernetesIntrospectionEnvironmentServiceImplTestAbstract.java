package controllers;

import engine.ConfigMapAnalyzer;
import engine.EndpointAnalyzer;
import engine.OwnerAnalyzer;
import engine.PodAnalyzer;
import engine.RbacAnalyzer;
import engine.SecretAnalyzer;
import engine.ServiceAnalyzer;
import engine.owners.OwnerAnalyzerDeployment;
import usesCases.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.useCases.env.EnvironmentProviderSystemImpl;
import kubernetes.introspection.useCases.env.KubernetesFileReadService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static usesCases.utils.KubernetesYamlUtils.loadRbacYaml;

@Slf4j
public class KubernetesIntrospectionEnvironmentServiceImplTestAbstract {

    protected static final String NAMESPACE = "test-namespace";
    protected static final String POD_NAME = "test-pod";
    protected static final String DEPLOYMENT_NAME = "test-deployment";
    protected static final String SERVICE_NAME = "test-service";
    protected static final String CONFIGMAP_NAME = "test-configmap";
    protected static final String SECRET_NAME = "test-secret";


    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;

    protected EnvironmentProviderSystemImpl mockEnvProvider;
    protected KubernetesFileReadService mockFileReadService;


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
