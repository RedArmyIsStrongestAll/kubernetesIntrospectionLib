package engine.owner;

import engine.RbacAnalyzer;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class OwnerAnalyzerJob implements OwnerAnalyzer<Job> {
    private final Job job;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerJob(String yamlContent) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.job = yaml.loadAs(yamlContent, Job.class);
        this.rbacAnalyzer = new RbacAnalyzer(yamlContent);
    }

    @Override
    public Job getOwner(String name, String namespace) {
        if (job == null) return null;
        if (!rbacAnalyzer.isAllowed("jobs", "get", namespace)) return null;
        ObjectMeta meta = job.getMetadata();
        if (meta == null) return null;
        if (name.equals(meta.getName()) && namespace.equals(meta.getNamespace())) {
            return job;
        }
        return null;
    }
}