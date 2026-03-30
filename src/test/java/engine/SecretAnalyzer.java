package engine;

import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SecretAnalyzer {

    private final List<Secret> secretList;
    private final RbacAnalyzer rbacAnalyzer;

    public SecretAnalyzer(String secretYaml, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.rbacAnalyzer = rbacAnalyzer;
        this.secretList = KubernetesYamlUtils.trySetYamlObjectList(secretYaml, Secret.class, "Secret");
    }

    public Secret getSecretByName(String requestedName, String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("secrets", "get", requestedNamespace)) return null;
        return secretList.stream()
                .filter(secret -> secret.getMetadata() != null)
                .filter(secret -> requestedName.equals(secret.getMetadata().getName()))
                .filter(secret -> requestedNamespace.equals(secret.getMetadata().getNamespace()))
                .findFirst()
                .orElse(null);
    }

    public SecretList listAllSecrets(String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("secrets", "list", requestedNamespace)) {
            return new SecretList("v1", Collections.emptyList(), "SecretList", new ListMeta());
        }
        List<Secret> matchedSecrets = secretList.stream()
                .filter(secret -> secret.getMetadata() != null && secret.getMetadata().getNamespace().equals(requestedNamespace))
                .collect(Collectors.toList());
        return new SecretList("v1", matchedSecrets, "SecretList", new ListMeta());
    }
}