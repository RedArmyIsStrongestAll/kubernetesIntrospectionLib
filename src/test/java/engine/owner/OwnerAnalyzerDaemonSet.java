package engine.owner;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OwnerAnalyzerDaemonSet implements OwnerAnalyzer<DaemonSet> {
    private final DaemonSet daemonSet;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerDaemonSet(String yamlContent) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.daemonSet = yaml.loadAs(yamlContent, DaemonSet.class);
        this.rbacAnalyzer = new RbacAnalyzer(yamlContent);
    }

    @Override
    public DaemonSet getOwner(String name, String namespace) {
        if (daemonSet == null) return null;
        if (!rbacAnalyzer.isAllowed("daemonsets", "get", namespace)) return null;
        ObjectMeta meta = daemonSet.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return daemonSet;
        }
        return null;
    }
}