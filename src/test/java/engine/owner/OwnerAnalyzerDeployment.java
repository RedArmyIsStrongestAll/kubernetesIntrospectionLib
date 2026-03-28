package engine.owner;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OwnerAnalyzerDeployment implements OwnerAnalyzer<Deployment> {

    private final Deployment deployment;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerDeployment(String yamlContent) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.deployment = yaml.loadAs(yamlContent, Deployment.class);

        this.rbacAnalyzer = new RbacAnalyzer(yamlContent);
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
