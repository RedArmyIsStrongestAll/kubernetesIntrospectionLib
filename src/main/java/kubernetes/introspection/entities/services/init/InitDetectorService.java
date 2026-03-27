package kubernetes.introspection.entities.services.init;

import kubernetes.introspection.entities.models.exceptions.ErrorCode;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class InitDetectorService {
    private static final String ENV = "KUBERNETES_SERVICE_HOST";
    private static final Path SA_TOKEN = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token");
    private static final Path SA_NS = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace");

    public boolean runningInKubernetes() {
        String kubeHost = getKubernetesHostEnv();
        boolean hasKubeEnv = kubeHost != null && !kubeHost.isBlank();
        boolean hasToken = Files.exists(SA_TOKEN);
        boolean hasNs = Files.exists(SA_NS);

        return hasKubeEnv && hasToken && hasNs;
    }

    public String getKubernetesHostEnv() {
        return System.getenv(ENV);
    }

    public String getNamespace() {
        if (!runningInKubernetes()) {
            log.error(ErrorCode.NOT_IN_CLUSTER.getMessage());
            throw new KubernetesException(ErrorCode.NOT_IN_CLUSTER);
        }

        try {
            String ns = Files.readString(SA_NS).trim();
            if (ns.isBlank()) {
                throw new Exception();
            }
            return ns;

        } catch (Exception e) {
            log.error(ErrorCode.NOT_NAMESPACE.getMessage());
            throw new KubernetesException(ErrorCode.NOT_NAMESPACE);
        }
    }

    public static Path getTokenPath() {
        return SA_TOKEN;
    }

    public static Path getNamespacePath() {
        return SA_NS;
    }

}
