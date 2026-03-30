package engine.owners;

import engine.OwnerAnalyzer;
import engine.RbacAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;

import java.io.IOException;

public class OwnerAnalyzerJob implements OwnerAnalyzer<Job> {
    private final Job job;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerJob(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.job = KubernetesYamlUtils.trySetYamlObject(yamlContent, Job.class);

        this.rbacAnalyzer = rbacAnalyzer;
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