package kubernetes.introspection.entities.services.init;

import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class InitDetectorService {

    private static final String ENV = "KUBERNETES_SERVICE_HOST";
    private static final Path SA_TOKEN = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token");
    private static final Path SA_NS = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace");

    public boolean runningInKubernetes() {
        log.info("Start runningInKubernetes");
        String kubeHost = getKubernetesHostEnv();
        boolean hasKubeEnv = kubeHost != null && !kubeHost.isBlank();
        boolean hasToken = isFileExists(SA_TOKEN);
        boolean hasNs = isFileExists(SA_NS);

        log.info("Result runningInKubernetes: hasKubeEnv {}, hasToken {}, hasNs {}",
                hasKubeEnv, hasToken, hasNs);
        return hasKubeEnv && hasToken && hasNs;
    }

    public String getNamespace() {
        log.info("Start getNamespace");

        if (!runningInKubernetes()) {
            log.error(ErrorCodeEnum.NOT_IN_CLUSTER.getMessage());
            throw new KubernetesException(ErrorCodeEnum.NOT_IN_CLUSTER);
        }

        try {
            String ns = readFileContent(SA_NS).trim();
            if (ns.isBlank()) {
                throw new IOException("Namespace content is blank");
            }
            return ns;

        } catch (Exception e) {
            log.error(ErrorCodeEnum.NOT_NAMESPACE.getMessage(), e);
            throw new KubernetesException(ErrorCodeEnum.NOT_NAMESPACE);
        }
    }

    @Deprecated(since = "Using only for test")
    public boolean isFileExists(Path path) {
        return Files.exists(path);
    }

    @Deprecated(since = "Using only for test")
    public String readFileContent(Path path) throws IOException {
        return Files.readString(path);
    }

    @Deprecated(since = "Using only for test")
    public String getKubernetesHostEnv() {
        return System.getenv(ENV);
    }

    @Deprecated(since = "Using only for test")
    public static Path getTokenPath() {
        return SA_TOKEN;
    }

    @Deprecated(since = "Using only for test")
    public static Path getNamespacePath() {
        return SA_NS;
    }
}