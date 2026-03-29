package engine.owner;

import engine.RbacAnalyzer;
import entities.services.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationController;

import java.io.IOException;

public class OwnerAnalyzerReplicationController implements OwnerAnalyzer<ReplicationController> {
    private final ReplicationController replicationController;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerReplicationController(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.replicationController = TestUtils.trySetYamlObject(yamlContent, ReplicationController.class);

        this.rbacAnalyzer = rbacAnalyzer;
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