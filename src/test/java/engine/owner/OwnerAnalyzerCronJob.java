package engine.owner;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OwnerAnalyzerCronJob implements OwnerAnalyzer<CronJob> {
    private final CronJob cronJob;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerCronJob(String yamlContent) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.cronJob = yaml.loadAs(yamlContent, CronJob.class);
        this.rbacAnalyzer = new RbacAnalyzer(yamlContent);
    }

    @Override
    public CronJob getOwner(String name, String namespace) {
        if (cronJob == null) return null;
        if (!rbacAnalyzer.isAllowed("cronjobs", "get", namespace)) return null;
        ObjectMeta meta = cronJob.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return cronJob;
        }
        return null;
    }
}