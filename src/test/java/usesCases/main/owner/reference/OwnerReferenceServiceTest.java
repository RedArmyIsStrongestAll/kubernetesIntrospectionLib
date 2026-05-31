package usesCases.main.owner.reference;

import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.main.owner.reference.OwnerReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OwnerReferenceServiceTest {

    private OwnerReferenceService ownerReferenceService;

    @BeforeEach
    void setUp() {
        this.ownerReferenceService = new OwnerReferenceService();
    }

    @Test
    void getPodOwnerWithPermissionValidTest() {
        PodInfo podInfo = PodInfo.builder()
                .name("test-pod")
                .namespace("default")
                .ownerReferences(List.of(
                        OwnerReferenceInfo.builder()
                                .apiVersion("apps/v1")
                                .kind("ReplicaSet")
                                .name("my-replicaset")
                                .uid("rs-12345")
                                .controller(true)
                                .build()
                ))
                .build();

        OwnerReferenceInfo result = ownerReferenceService.getPodOwner(podInfo);

        assertNotNull(result, "OwnerReferenceInfo не должен быть null");
        assertTrue(result.getController(), "Должен быть установлен controller = true");
        assertEquals("apps/v1", result.getApiVersion());
        assertEquals("ReplicaSet", result.getKind());
        assertEquals("my-replicaset", result.getName());
        assertEquals("rs-12345", result.getUid());
    }

    @Test
    void getPodOwnerWithPermissionNoPodOwnerTest() {
        PodInfo podInfo = PodInfo.builder()
                .name("test-pod")
                .namespace("default")
                .ownerReferences(null)
                .build();

        OwnerReferenceInfo result = ownerReferenceService.getPodOwner(podInfo);

        assertNotNull(result, "OwnerReferenceInfo не должен быть null");
        assertNull(result.getKind(), "Должен быть пустым");
    }

    @Test
    void getPodOwnerWithPermissionNoControllerValidTest() {
        PodInfo podInfo = PodInfo.builder()
                .name("test-pod")
                .namespace("default")
                .ownerReferences(List.of(
                        OwnerReferenceInfo.builder()
                                .apiVersion("apps/v1")
                                .kind("ReplicaSet")
                                .name("my-replicaset")
                                .uid("rs-12345")
                                .controller(false)
                                .build()
                ))
                .build();

        OwnerReferenceInfo result = ownerReferenceService.getPodOwner(podInfo);

        assertNotNull(result);
        assertFalse(result.getController());
        assertEquals("apps/v1", result.getApiVersion());
        assertEquals("ReplicaSet", result.getKind());
        assertEquals("my-replicaset", result.getName());
        assertEquals("rs-12345", result.getUid());
    }
}
