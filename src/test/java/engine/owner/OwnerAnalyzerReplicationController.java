package engine.owner;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OwnerAnalyzerReplicationController implements OwnerAnalyzer<ReplicationController> {
    private final ReplicationController replicationController;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerReplicationController(String yamlContent) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.replicationController = yaml.loadAs(yamlContent, ReplicationController.class);
        this.rbacAnalyzer = new RbacAnalyzer(yamlContent);
    }

    @Override
    public ReplicationController getOwner(String name, String namespace) {
        if (replicationController == null) return null;
        if (!rbacAnalyzer.isAllowed("replicationcontrollers", "get", namespace)) return null;
        ObjectMeta meta = replicationController.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return replicationController;
        }
        return null;
    }
}