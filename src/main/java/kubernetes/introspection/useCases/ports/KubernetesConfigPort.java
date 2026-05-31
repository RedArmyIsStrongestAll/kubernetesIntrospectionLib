package kubernetes.introspection.useCases.ports;

import java.util.List;

public interface KubernetesConfigPort {
    List<String> getConfigMapKeys(String name, String namespace);

    List<String> getSecretKeys(String name, String namespace);
}
