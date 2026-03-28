package engine.owner;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OwnerAnalyzerStatefulSet implements OwnerAnalyzer<StatefulSet> {
    private final StatefulSet statefulSet;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerStatefulSet(String yamlContent) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.statefulSet = yaml.loadAs(yamlContent, StatefulSet.class);
        this.rbacAnalyzer = new RbacAnalyzer(yamlContent);
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