package kubernetes.introspection.adapters;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.useCases.ports.KubernetesConfigPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Fabric8ConfigAdapter implements KubernetesConfigPort {

    private final KubernetesClient client;

    public Fabric8ConfigAdapter(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public List<String> getConfigMapKeys(String name, String namespace) {
        ConfigMap cm = client.configMaps().inNamespace(namespace).withName(name).get();
        if (cm == null) return null;
        List<String> keys = new ArrayList<>();
        if (cm.getData() != null) keys.addAll(cm.getData().keySet());
        if (cm.getBinaryData() != null) keys.addAll(cm.getBinaryData().keySet());
        return keys;
    }

    @Override
    public List<String> getSecretKeys(String name, String namespace) {
        Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
        if (secret == null) return null;
        return secret.getData() != null ? new ArrayList<>(secret.getData().keySet()) : Collections.emptyList();
    }
}
