package kubernetes.introspection.useCases.env;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KubernetesFileReadServiceFileImpl implements KubernetesFileReadService {

    private static final String ENV = "KUBERNETES_SERVICE_HOST";
    private static final Path SA_TOKEN = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token");
    private static final Path SA_NS = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace");

    public String getKubernetesHostEnv() {
        return System.getenv(ENV);
    }

    public boolean namespaceExists() {
        return Files.exists(SA_NS);
    }

    public boolean tokenExists() {
        return Files.exists(SA_TOKEN);
    }

    public String getNamespace() throws IOException {
        return Files.readString(SA_NS);
    }

    public String getToken() throws IOException {
        return Files.readString(SA_TOKEN);
    }
}
