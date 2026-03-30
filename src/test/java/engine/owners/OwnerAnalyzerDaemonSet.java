package engine.owners;

import engine.OwnerAnalyzer;
import engine.RbacAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;

import java.io.IOException;

public class OwnerAnalyzerDaemonSet implements OwnerAnalyzer<DaemonSet> {
    private final DaemonSet daemonSet;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerDaemonSet(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.daemonSet = KubernetesYamlUtils.trySetYamlObject(yamlContent, DaemonSet.class);

        this.rbacAnalyzer = rbacAnalyzer;
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