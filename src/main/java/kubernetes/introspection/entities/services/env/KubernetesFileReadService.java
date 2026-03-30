package kubernetes.introspection.entities.services.env;

import java.io.IOException;

public interface KubernetesFileReadService {

    public String getKubernetesHostEnv();

    public boolean namespaceExists();

    public boolean tokenExists();

    public String getNamespace() throws IOException;

    public String getToken() throws IOException;
}
