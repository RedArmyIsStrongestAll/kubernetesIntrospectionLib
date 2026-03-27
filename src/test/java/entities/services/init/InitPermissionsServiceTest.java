package entities.services.init;

import engine.RbacAnalyzer;
import entities.services.init.parant.InitPermissionsServiceTestAbstract;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.services.init.InitPermissionsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
class InitPermissionsServiceTest extends InitPermissionsServiceTestAbstract {

    @Test
    void testCheckPermissionsWithAllAccess() throws Exception {
        InitPermissionsService service = new InitPermissionsService(client);
        String yamlContent = loadRbacYaml("rbac/test-rbac.yaml");
        RbacAnalyzer analyzer = new RbacAnalyzer(yamlContent);
        setupMockServerWithRbacAnalyzer(analyzer);

        PermissionInfo result = service.checkPermissions(testNamespace);
        log.info("Test result: {}", result);

        assertTrue(result.isSuccess(), "Проверка должна быть успешной, если все разрешения разрешены");

        // Проверяем pods
        assertPermissionAllowed(result, "pods", "get");
        assertPermissionAllowed(result, "pods", "list");
        assertPermissionAllowed(result, "pods", "watch");

        // Проверяем services
        assertPermissionAllowed(result, "services", "get");
        assertPermissionAllowed(result, "services", "list");
        assertPermissionAllowed(result, "services", "watch");
    }

    @Test
    void testCheckPermissionsNoPodWatch() throws Exception {
        InitPermissionsService service = new InitPermissionsService(client);

        String yamlContent = loadRbacYaml("rbac/fail-test-rbac-no-pod-watch.yaml");
        RbacAnalyzer analyzer = new RbacAnalyzer(yamlContent);
        setupMockServerWithRbacAnalyzer(analyzer);

        PermissionInfo result = service.checkPermissions(testNamespace);
        log.info("Test result: {}", result);

        assertFalse(result.isSuccess(), "Проверка должна быть неуспешной когда pods/watch запрещен");

        PermissionInfo.PermissionInfoDto podWatch = findPermission(result, "pods", "watch");
        PermissionInfo.PermissionInfoDto podGet = findPermission(result, "pods", "get");
        PermissionInfo.PermissionInfoDto podList = findPermission(result, "pods", "list");
        assertNotNull(podWatch, "Permission pods/watch должен присутствовать в результате");
        assertFalse(podWatch.isAllowed(), "pods/watch должен быть запрещен RBAC");
        assertNotNull(podGet, "Permission pods/get должен присутствовать в результате");
        assertTrue(podGet.isAllowed(), "pods/get должен быть разрешен");
        assertNotNull(podList, "Permission pods/list должен присутствовать в результате");
        assertTrue(podList.isAllowed(), "pods/list должен быть разрешен");
    }

    @Test
    void testCheckPermissionsWithDefaultRbac() throws Exception {
        InitPermissionsService service = new InitPermissionsService(client);
        String yamlContent = loadRbacYaml("rbac/fail-test-rbac-default.yaml");
        RbacAnalyzer analyzer = new RbacAnalyzer(yamlContent);
        setupMockServerWithRbacAnalyzer(analyzer);

        PermissionInfo result = service.checkPermissions(testNamespace);
        log.info("Test result: {}", result);

        assertFalse(result.isSuccess(), "Проверка должна быть успешной при дефольных разрешениях");

        // Проверяем pods
        PermissionInfo.PermissionInfoDto podWatch = findPermission(result, "pods", "watch");
        PermissionInfo.PermissionInfoDto podGet = findPermission(result, "pods", "get");
        PermissionInfo.PermissionInfoDto podList = findPermission(result, "pods", "list");
        assertNotNull(podWatch, "Permission pods/watch должен присутствовать");
        assertFalse(podWatch.isAllowed(), "pods/watch должен не быть разрешен");
        assertNotNull(podGet, "Permission pods/get должен присутствовать");
        assertFalse(podGet.isAllowed(), "pods/get должен не быть разрешен");
        assertNotNull(podList, "Permission pods/list должен присутствовать");
        assertFalse(podList.isAllowed(), "pods/list должен не быть разрешен");

        // Проверяем services
        PermissionInfo.PermissionInfoDto svcWatch = findPermission(result, "services", "watch");
        PermissionInfo.PermissionInfoDto svcGet = findPermission(result, "services", "get");
        PermissionInfo.PermissionInfoDto svcList = findPermission(result, "services", "list");
        assertNotNull(svcWatch, "Permission services/watch должен присутствовать");
        assertFalse(svcWatch.isAllowed(), "services/watch должен не быть разрешен");
        assertNotNull(svcGet, "Permission services/get должен присутствовать");
        assertFalse(svcGet.isAllowed(), "services/get должен не быть разрешен");
        assertNotNull(svcList, "Permission services/list должен присутствовать");
        assertFalse(svcList.isAllowed(), "services/list должен не быть разрешен");
    }

    @Test
    void testCheckPermissionsNoAllPermissions() throws Exception {
        InitPermissionsService service = new InitPermissionsService(client);
        String yamlContent = loadRbacYaml("rbac/fail-test-rbac-no-all.yaml");
        RbacAnalyzer analyzer = new RbacAnalyzer(yamlContent);
        setupMockServerWithRbacAnalyzer(analyzer);

        PermissionInfo result = service.checkPermissions(testNamespace);
        log.info("Test result: {}", result);

        assertFalse(result.isSuccess(), "Проверка должна быть неуспешной при полном запрете");

        PermissionInfo.PermissionInfoDto podWatch = findPermission(result, "pods", "watch");
        PermissionInfo.PermissionInfoDto podGet = findPermission(result, "pods", "get");
        PermissionInfo.PermissionInfoDto podList = findPermission(result, "pods", "list");
        assertNotNull(podWatch, "Permission pods/watch должен присутствовать");
        assertFalse(podWatch.isAllowed(), "pods/watch должен быть запрещен");
        assertNotNull(podGet, "Permission pods/get должен присутствовать");
        assertFalse(podGet.isAllowed(), "pods/get должен быть запрещен");
        assertNotNull(podList, "Permission pods/list должен присутствовать");
        assertFalse(podList.isAllowed(), "pods/list должен быть запрещен");

        PermissionInfo.PermissionInfoDto svcWatch = findPermission(result, "services", "watch");
        PermissionInfo.PermissionInfoDto svcGet = findPermission(result, "services", "get");
        PermissionInfo.PermissionInfoDto svcList = findPermission(result, "services", "list");
        assertNotNull(svcWatch, "Permission services/watch должен присутствовать");
        assertFalse(svcWatch.isAllowed(), "services/watch должен быть запрещен");
        assertNotNull(svcGet, "Permission services/get должен присутствовать");
        assertFalse(svcGet.isAllowed(), "services/get должен быть запрещен");
        assertNotNull(svcList, "Permission services/list должен присутствовать");
        assertFalse(svcList.isAllowed(), "services/list должен быть запрещен");
    }

}
