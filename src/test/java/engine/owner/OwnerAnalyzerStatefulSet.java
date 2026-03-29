package engine.owner;

import engine.RbacAnalyzer;
import entities.services.utils.TestUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import java.io.IOException;

public class OwnerAnalyzerStatefulSet implements OwnerAnalyzer<StatefulSet> {
    private final StatefulSet statefulSet;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerStatefulSet(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.statefulSet = TestUtils.trySetYamlObject(yamlContent, StatefulSet.class);

        this.rbacAnalyzer = rbacAnalyzer;
    }

    @Override
    public StatefulSet getOwner(String name, String namespace) {
        if (statefulSet == null) return null;
        if (!rbacAnalyzer.isAllowed("statefulsets", "get", namespace)) return null;
        ObjectMeta meta = statefulSet.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return statefulSet;
        }
        return null;
    }
}