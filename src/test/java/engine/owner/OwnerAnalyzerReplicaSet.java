package engine.owner;

import engine.RbacAnalyzer;
import entities.services.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;

import java.io.IOException;

public class OwnerAnalyzerReplicaSet implements OwnerAnalyzer<ReplicaSet> {
    private final ReplicaSet replicaSet;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerReplicaSet(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.replicaSet = TestUtils.trySetYamlObject(yamlContent, ReplicaSet.class);

        this.rbacAnalyzer = rbacAnalyzer;
    }

    @Override
    public ReplicaSet getOwner(String name, String namespace) {
        if (replicaSet == null) return null;
        if (!rbacAnalyzer.isAllowed("replicasets", "get", namespace)) return null;
        ObjectMeta meta = replicaSet.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return replicaSet;
        }
        return null;
    }
}