package entities.services.main.owner.reference;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import kubernetes.introspection.entities.services.main.owner.reference.OwnerReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;

import static entities.services.utils.KubernetesYamlUtils.loadRbacYaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnerReferenceServiceTest {

    private OwnerReferenceService ownerReferenceService;
    private Pod pod;

    @BeforeEach
    void setUp() throws Exception {
        this.ownerReferenceService = new OwnerReferenceService();
    }

    private void setPod(String fileName) throws IOException {
        String podString = loadRbacYaml(fileName);
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.pod = yaml.loadAs(podString, Pod.class);
    }

    @Test
    void getPodOwnerWithPermissionValidTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner.yaml");

        OwnerReference result = ownerReferenceService.getPodOwner(pod);

        assertNotNull(result, "OwnerReference не должен быть null");

        assertTrue(result.getController(), "Должен быть установлен controller = true");

        assertEquals("apps/v1", result.getApiVersion());
        assertEquals("ReplicaSet", result.getKind());
        assertEquals("my-replicaset", result.getName());
        assertEquals("rs-12345", result.getUid());
    }

    @Test
    void getPodOwnerWithPermissionNoPodOwnerTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner.yaml");

        pod.getMetadata().setOwnerReferences(null);

        OwnerReference result = ownerReferenceService.getPodOwner(pod);

        assertNotNull(result, "OwnerReference не должен быть null");
        assertNull(result.getKind(), "Должен быть пустым");
    }

    @Test
    void getPodOwnerWithPermissionNoControllerValidTest() throws IOException {
        setPod("owner/reference/test-short-pod-owner-no-controller.yaml");

        OwnerReference result = ownerReferenceService.getPodOwner(pod);

        assertNotNull(result, "OwnerReference не должен быть null");

        assertFalse(result.getController(), "Должен быть установлен controller = true");

        assertEquals("apps/v1", result.getApiVersion());
        assertEquals("ReplicaSet", result.getKind());
        assertEquals("my-replicaset", result.getName());
        assertEquals("rs-12345", result.getUid());
    }


}
