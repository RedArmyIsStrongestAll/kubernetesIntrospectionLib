package engine.owner;

import engine.RbacAnalyzer;
import entities.services.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.io.IOException;

public class OwnerAnalyzerDeployment implements OwnerAnalyzer<Deployment> {

    private final Deployment deployment;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerDeployment(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.deployment = (Deployment) TestUtils.changeSetYamlObject(yamlContent, Deployment.class);

        this.rbacAnalyzer = rbacAnalyzer;
    }

    @Override
    public Deployment getOwner(String name, String namespace) {
        if (deployment == null) return null;
        if (!rbacAnalyzer.isAllowed("deployments", "get", namespace)) return null;
        ObjectMeta meta = deployment.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return deployment;
        }
        return null;
    }
}
