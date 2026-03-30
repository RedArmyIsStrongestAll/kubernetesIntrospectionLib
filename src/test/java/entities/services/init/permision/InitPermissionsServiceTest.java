package entities.services.init.permision;

import entities.services.init.permision.parent.InitPermissionsServiceTestAbstract;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.enviroment.CollectionError;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
class InitPermissionsServiceTest extends InitPermissionsServiceTestAbstract {

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void checkPermissionsWithAllAccessTest() throws Exception {
        PermissionInfo result = getPermissionInfoForRbacFile("rbac/test-rbac.yaml", testNamespace);
        log.info("Test result: {}", result);

        assertTrue(result.isSuccess(), "Проверка должна быть успешной, если все разрешения разрешены");

        // Проверяем pods
        assertPermissionAllowed(result, "pods", "get");
        assertPermissionAllowed(result, "pods", "list");

        // Проверяем services
        assertPermissionAllowed(result, "services", "get");
        assertPermissionAllowed(result, "services", "list");
    }

    @Test
    void convertToCollectionErrorsWithAllAccessTest() throws IOException {
        PermissionInfo info = getPermissionInfoForRbacFile("rbac/test-rbac.yaml", testNamespace);
        log.info("Call checkPermissions result: {}", info);

        List<CollectionError> errors = convert(info, testNamespace);
        log.info("Test result: {}", errors);

        assertTrue(errors.isEmpty(), "Должно быть 0 ошибок, если все разрешения разрешены");
    }

    @Test
    void checkPermissionsNoPodGetTest() throws Exception {
        PermissionInfo result = getPermissionInfoForRbacFile("rbac/fail-test-rbac-no-pod-get.yaml", testNamespace);
        log.info("Test result: {}", result);

        assertFalse(result.isSuccess(), "Проверка должна быть неуспешной когда pods/get запрещен");

        PermissionInfo.PermissionInfoDto podGet = findPermission(result, "pods", "get");
        PermissionInfo.PermissionInfoDto podList = findPermission(result, "pods", "list");
        assertNotNull(podGet, "Permission pods/get должен присутствовать в результате");
        assertFalse(podGet.isAllowed(), "pods/get НЕ должен быть разрешен");
        assertNotNull(podList, "Permission pods/list должен присутствовать в результате");
        assertTrue(podList.isAllowed(), "pods/list должен быть разрешен");
    }

    @Test
    void convertToCollectionErrorsNoPodGetTest() throws IOException {
        PermissionInfo info = getPermissionInfoForRbacFile("rbac/fail-test-rbac-no-pod-get.yaml", testNamespace);
        log.info("Call checkPermissions result: {}", info);

        List<CollectionError> errors = convert(info, testNamespace);
        log.info("Test result: {}", errors);

        assertEquals(1, errors.size(), "Должна быть одна ошибка");
        CollectionError error = errors.get(0);

        assertEquals("unknown", error.getResourceName());
        assertEquals(testNamespace, error.getNamespace());
        assertEquals("FORBIDDEN", error.getErrorCodeEnum().name());
    }

    @Test
    void checkPermissionsWithDefaultRbacTest() throws Exception {
        PermissionInfo result = getPermissionInfoForRbacFile("rbac/fail-test-rbac-default.yaml", testNamespace);
        log.info("Test result: {}", result);

        assertFalse(result.isSuccess(), "Проверка должна быть успешной при дефольных разрешениях");

        // Проверяем pods
        PermissionInfo.PermissionInfoDto podGet = findPermission(result, "pods", "get");
        PermissionInfo.PermissionInfoDto podList = findPermission(result, "pods", "list");
        assertNotNull(podGet, "Permission pods/get должен присутствовать");
        assertFalse(podGet.isAllowed(), "pods/get должен не быть разрешен");
        assertNotNull(podList, "Permission pods/list должен присутствовать");
        assertFalse(podList.isAllowed(), "pods/list должен не быть разрешен");

        // Проверяем services
        PermissionInfo.PermissionInfoDto svcGet = findPermission(result, "services", "get");
        PermissionInfo.PermissionInfoDto svcList = findPermission(result, "services", "list");
        assertNotNull(svcGet, "Permission services/get должен присутствовать");
        assertFalse(svcGet.isAllowed(), "services/get должен не быть разрешен");
        assertNotNull(svcList, "Permission services/list должен присутствовать");
        assertFalse(svcList.isAllowed(), "services/list должен не быть разрешен");
    }

    @Test
    void convertToCollectionErrorsWithDefaultRbacTest() throws IOException {
        PermissionInfo info = getPermissionInfoForRbacFile("rbac/fail-test-rbac-default.yaml", testNamespace);
        log.info("Call checkPermissions result: {}", info);

        List<CollectionError> errors = convert(info, testNamespace);
        log.info("Test result: {}", errors);

        assertEquals(ResourcePermissionEnum.getActualPermissions().size(), errors.size(), "Должна быть одна ошибка");
    }

    @Test
    void checkPermissionsNoAllPermissionsTest() throws Exception {
        PermissionInfo result = getPermissionInfoForRbacFile("rbac/fail-test-rbac-no-all.yaml", testNamespace);
        log.info("Test result: {}", result);

        assertFalse(result.isSuccess(), "Проверка должна быть неуспешной при полном запрете");

        PermissionInfo.PermissionInfoDto podGet = findPermission(result, "pods", "get");
        PermissionInfo.PermissionInfoDto podList = findPermission(result, "pods", "list");
        assertNotNull(podGet, "Permission pods/get должен присутствовать");
        assertFalse(podGet.isAllowed(), "pods/get должен быть запрещен");
        assertNotNull(podList, "Permission pods/list должен присутствовать");
        assertFalse(podList.isAllowed(), "pods/list должен быть запрещен");

        PermissionInfo.PermissionInfoDto svcGet = findPermission(result, "services", "get");
        PermissionInfo.PermissionInfoDto svcList = findPermission(result, "services", "list");
        assertNotNull(svcGet, "Permission services/get должен присутствовать");
        assertFalse(svcGet.isAllowed(), "services/get должен быть запрещен");
        assertNotNull(svcList, "Permission services/list должен присутствовать");
        assertFalse(svcList.isAllowed(), "services/list должен быть запрещен");
    }

    @Test
    void convertToCollectionErrorsNoAllPermissionsTest() throws IOException {
        PermissionInfo info = getPermissionInfoForRbacFile("rbac/fail-test-rbac-no-all.yaml", testNamespace);
        log.info("Call checkPermissions result: {}", info);

        List<CollectionError> errors = convert(info, testNamespace);
        log.info("Test result: {}", info);

        assertEquals(ResourcePermissionEnum.getActualPermissions().size(), errors.size());
    }

}