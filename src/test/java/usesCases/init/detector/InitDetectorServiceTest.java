package usesCases.init.detector;

import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.useCases.env.KubernetesFileReadService;
import kubernetes.introspection.useCases.init.InitDetectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class InitDetectorServiceTest {

    @Mock
    private KubernetesFileReadService kubernetesFileReadService;

    @InjectMocks
    private InitDetectorService initDetectorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void runningInKubernetesNoKubernetesHostEnvTest() {
        when(kubernetesFileReadService.getKubernetesHostEnv()).thenReturn(null);
        when(kubernetesFileReadService.namespaceExists()).thenReturn(true);
        when(kubernetesFileReadService.tokenExists()).thenReturn(true);

        boolean result = initDetectorService.runningInKubernetes();
        assertFalse(result);
    }

    @Test
    void runningInKubernetesNoTokenFileTest() {
        when(kubernetesFileReadService.getKubernetesHostEnv()).thenReturn("1.2.3.4");
        when(kubernetesFileReadService.namespaceExists()).thenReturn(true);
        when(kubernetesFileReadService.tokenExists()).thenReturn(false);

        boolean result = initDetectorService.runningInKubernetes();
        assertFalse(result);
    }

    @Test
    void runningInKubernetesSuccessTest() {
        when(kubernetesFileReadService.getKubernetesHostEnv()).thenReturn("1.2.3.4");
        when(kubernetesFileReadService.namespaceExists()).thenReturn(true);
        when(kubernetesFileReadService.tokenExists()).thenReturn(true);

        boolean result = initDetectorService.runningInKubernetes();
        assertTrue(result);
    }

    @Test
    void getNamespaceNoNamespaceFileTest() {
        when(kubernetesFileReadService.getKubernetesHostEnv()).thenReturn("1.2.3.4");
        when(kubernetesFileReadService.namespaceExists()).thenReturn(false);

        KubernetesException ex = assertThrows(KubernetesException.class, () -> initDetectorService.getNamespace());
        assertEquals(ErrorCodeEnum.NOT_IN_CLUSTER, ex.getErrorCodeEnum());
    }

    @Test
    void getNamespaceReturnsValidNamespaceTest() throws IOException {
        when(kubernetesFileReadService.getKubernetesHostEnv()).thenReturn("1.2.3.4");
        when(kubernetesFileReadService.namespaceExists()).thenReturn(true);
        when(kubernetesFileReadService.tokenExists()).thenReturn(true);
        when(kubernetesFileReadService.getNamespace()).thenReturn("test-namespace");

        String result = initDetectorService.getNamespace();
        assertEquals("test-namespace", result);
    }

    @Test
    void getNamespaceReturnsEmptyNamespaceTest() throws IOException {
        when(kubernetesFileReadService.getKubernetesHostEnv()).thenReturn("1.2.3.4");
        when(kubernetesFileReadService.namespaceExists()).thenReturn(true);
        when(kubernetesFileReadService.tokenExists()).thenReturn(true);
        when(kubernetesFileReadService.getNamespace()).thenReturn("");

        KubernetesException ex = assertThrows(KubernetesException.class, () -> initDetectorService.getNamespace());
        assertEquals(ErrorCodeEnum.NOT_NAMESPACE, ex.getErrorCodeEnum());
    }
}