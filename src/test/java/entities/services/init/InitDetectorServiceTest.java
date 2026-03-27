package entities.services.init;

import kubernetes.introspection.entities.models.exceptions.ErrorCode;
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
    void testRunningInKubernetesNoKubernetesHostEnv() {
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
    void testRunningInKubernetesNoTokenFile() {
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
    void testRunningInKubernetesSuccess() {
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
    void testGetNamespaceNoNamespaceFile() {
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
            assertEquals(ErrorCode.NOT_IN_CLUSTER, ex.getErrorCode());
        }
    }

    @Test
    void testGetNamespaceReturnsValidNamespace() {
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
}