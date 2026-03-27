package entities.services.init.parent;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.enviroment.CollectionError;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.services.init.InitPermissionsService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class InitPermissionsServiceTestAbstract {
    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;
    protected final String testNamespace = "test-namespace";


    public PermissionInfo getPermissionInfoForRbacFile(String filename, String namespace) throws IOException {
        InitPermissionsService service = new InitPermissionsService(client);
        String yamlContent = loadRbacYaml(filename);
        RbacAnalyzer analyzer = new RbacAnalyzer(yamlContent);
        setupMockServerWithRbacAnalyzer(analyzer);

        return service.checkPermissions(namespace);
    }

    public List<CollectionError> convert(PermissionInfo info, String namespace) {
        return new InitPermissionsService(client).convertToCollectionErrors(info, namespace);
    }


    protected String loadRbacYaml(String filename) throws IOException {
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

    protected void setupMockServerWithRbacAnalyzer(RbacAnalyzer analyzer) {
        mockServer.expect().post()
                .withPath("/apis/authorization.k8s.io/v1/selfsubjectaccessreviews")
                .andReply(200, request ->
                {
                    String body = request.getBody().readUtf8();
                    log.info("Received SSAR request: {}", body);


                    // Извлекаем параметры из SSAR запроса
                    String resource = extractJsonValue(body, "resource");
                    String verb = extractJsonValue(body, "verb");
                    String namespace = extractJsonValue(body, "namespace");
                    log.info("SSAR request for: \nResource: {}, \nVerb: {}, \nNamespace: {}", resource, verb, namespace);

                    // Анализируем через наш RBAC анализатор
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

    protected PermissionInfo.PermissionInfoDto findPermission(PermissionInfo result, String resource, String verb) {
        return result.getPermissions().stream()
                .filter(p -> p.getResource().getResource().equals(resource))
                .filter(p -> p.getResource().getVerb().equals(verb))
                .findFirst()
                .orElse(null);
    }

    protected void assertPermissionAllowed(PermissionInfo result, String resource, String verb) {
        PermissionInfo.PermissionInfoDto permission = findPermission(result, resource, verb);
        assertNotNull(permission, String.format("%s/%s должен присутствовать в результате", resource, verb));
        assertTrue(permission.isAllowed(), String.format("%s/%s должно быть разрешено", resource, verb));
    }
}
