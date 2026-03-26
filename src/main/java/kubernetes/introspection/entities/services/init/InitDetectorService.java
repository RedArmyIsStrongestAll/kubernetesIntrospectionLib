package kubernetes.introspection.entities.services.init;

import java.nio.file.Files;
import java.nio.file.Path;

public class InitDetectorService {
    private static final Path SA_TOKEN = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token");
    private static final Path SA_NS = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace");

    public boolean runningInKubernetes() {
        String kubeHost = System.getenv("KUBERNETES_SERVICE_HOST");
        boolean hasKubeEnv = kubeHost != null && !kubeHost.isBlank();
        boolean hasToken = Files.exists(SA_TOKEN);
        boolean hasNs = Files.exists(SA_NS);

        return hasKubeEnv && hasToken && hasNs;
    }

}
