package engine.owner;

import engine.RbacAnalyzer;
import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;

import java.io.IOException;

public class OwnerAnalyzerCronJob implements OwnerAnalyzer<CronJob> {
    private final CronJob cronJob;
    private final RbacAnalyzer rbacAnalyzer;

    public OwnerAnalyzerCronJob(String yamlContent, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.cronJob = KubernetesYamlUtils.trySetYamlObject(yamlContent, CronJob.class);

        this.rbacAnalyzer = rbacAnalyzer;
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