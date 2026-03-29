package entities.services.main.owner.reference;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.reference.OwnerReferenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.util.List;

import static entities.services.utils.TestUtils.loadRbacYaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnerReferenceServiceTest {
    private final String NAMESPACE = "test-namespace";

    private OwnerReferenceService ownerReferenceService;
    private Pod pod;

    @BeforeEach
    void setUp() throws Exception {
        this.ownerReferenceService = new OwnerReferenceService(NAMESPACE);
    }

    private void setPod(String fileName) throws IOException {
        String podString = loadRbacYaml(fileName);
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.pod = yaml.loadAs(podString, Pod.class);
    }

    @Test
    void getPodOwnerWithPermissionValidTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner.yaml");

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)
        ));

        OwnerReference result = ownerReferenceService.getPodOwnerWithPermission(pod, permissionInfo);

        assertNotNull(result, "OwnerReference не должен быть null");

        assertTrue(result.getController(), "Должен быть установлен controller = true");

        assertEquals("apps/v1", result.getApiVersion());
        assertEquals("ReplicaSet", result.getKind());
        assertEquals("my-replicaset", result.getName());
        assertEquals("rs-12345", result.getUid());
    }

    @Test
    void getPodOwnerWithPermissionNoListPermissionTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner.yaml");

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true)
        ));

        Assertions.assertThrows(KubernetesException.class, () -> ownerReferenceService.getPodOwnerWithPermission(pod, permissionInfo));
    }

    @Test
    void getPodOwnerWithPermissionNoPodOwnerTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner.yaml");

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)
        ));

        pod.getMetadata().setOwnerReferences(null);

        OwnerReference result = ownerReferenceService.getPodOwnerWithPermission(pod, permissionInfo);

        assertNotNull(result, "OwnerReference не должен быть null");
        assertNull(result.getKind(), "Должен быть пустым");
    }

    @Test
    void getPodOwnerWithPermissionNoControllerValidTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner-no-controller.yaml");

        PermissionInfo permissionInfo = new PermissionInfo(true, List.of(
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_LIST, true)
        ));

        OwnerReference result = ownerReferenceService.getPodOwnerWithPermission(pod, permissionInfo);

        assertNotNull(result, "OwnerReference не должен быть null");

        assertFalse(result.getController(), "Должен быть установлен controller = true");

        assertEquals("apps/v1", result.getApiVersion());
        assertEquals("ReplicaSet", result.getKind());
        assertEquals("my-replicaset", result.getName());
        assertEquals("rs-12345", result.getUid());
    }


}
