package entities.services.init.detector;

import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.init.InitDetectorService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class InitDetectorServiceTest {

    @Test
    void runningInKubernetesNoKubernetesHostEnvTest() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return null;
            }
        };

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(InitDetectorService.getNamespacePath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.exists(InitDetectorService.getTokenPath()))
                    .thenReturn(true);

            boolean result = testService.runningInKubernetes();
            assertFalse(result);
        }
    }

    @Test
    void runningInKubernetesNoTokenFileTest() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return "1.2.3.4";
            }
        };

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(InitDetectorService.getNamespacePath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.exists(InitDetectorService.getTokenPath()))
                    .thenReturn(false);

            boolean result = testService.runningInKubernetes();
            assertFalse(result);
        }
    }


    @Test
    void runningInKubernetesSuccessTest() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return "1.2.3.4";
            }
        };

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(InitDetectorService.getNamespacePath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.exists(InitDetectorService.getTokenPath()))
                    .thenReturn(true);

            boolean result = testService.runningInKubernetes();
            assertTrue(result);
        }
    }

    @Test
    void getNamespaceNoNamespaceFileTest() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return "1.2.3.4";
            }
        };

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(InitDetectorService.getNamespacePath()))
                    .thenReturn(false);

            KubernetesException ex = assertThrows(KubernetesException.class, testService::getNamespace);
            assertEquals(ErrorCodeEnum.NOT_IN_CLUSTER, ex.getErrorCodeEnum());
        }
    }

    @Test
    void getNamespaceReturnsValidNamespaceTest() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return "1.2.3.4";
            }
        };

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(InitDetectorService.getTokenPath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.exists(InitDetectorService.getNamespacePath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.readString(InitDetectorService.getNamespacePath()))
                    .thenReturn("test-namespace");

            String result = testService.getNamespace();
            assertEquals("test-namespace", result);
        }
    }

    @Test
    void getNamespaceReturnsEmptyNamespaceTest() {
        InitDetectorService testService = new InitDetectorService() {
            @Override
            public String getKubernetesHostEnv() {
                return "1.2.3.4";
            }
        };

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(InitDetectorService.getTokenPath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.exists(InitDetectorService.getNamespacePath()))
                    .thenReturn(true);
            mockedFiles.when(() -> Files.readString(InitDetectorService.getNamespacePath()))
                    .thenReturn("");

            KubernetesException ex = assertThrows(KubernetesException.class, testService::getNamespace);
            assertEquals(ErrorCodeEnum.NOT_NAMESPACE, ex.getErrorCodeEnum());
        }
    }
}