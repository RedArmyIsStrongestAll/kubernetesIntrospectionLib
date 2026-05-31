package kubernetes.introspection.adapters;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.ReplicationControllerStatus;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.batch.v1.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.owner.JobStatusInfo;
import kubernetes.introspection.entities.owner.OwnerInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.useCases.ports.KubernetesOwnerPort;

import java.util.Collections;
import java.util.Optional;

public class Fabric8OwnerAdapter implements KubernetesOwnerPort {

    private final KubernetesClient client;

    public Fabric8OwnerAdapter(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public OwnerInfo getDeployment(String name, String namespace) {
        Deployment d = client.apps().deployments().inNamespace(namespace).withName(name).get();
        if (d == null) return null;
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.DEPLOYMENT)
                .name(Optional.ofNullable(d.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(d.getSpec()).map(DeploymentSpec::getSelector)
                        .map(s -> s.getMatchLabels()).orElse(Collections.emptyMap()))
                .desiredReplicas(Optional.ofNullable(d.getSpec()).map(DeploymentSpec::getReplicas).orElse(null))
                .availableReplicas(Optional.ofNullable(d.getStatus()).map(DeploymentStatus::getAvailableReplicas).orElse(null))
                .build();
    }

    @Override
    public OwnerInfo getStatefulSet(String name, String namespace) {
        StatefulSet s = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
        if (s == null) return null;
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.STATEFULSET)
                .name(Optional.ofNullable(s.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(s.getSpec()).map(StatefulSetSpec::getSelector)
                        .map(sel -> sel.getMatchLabels()).orElse(Collections.emptyMap()))
                .desiredReplicas(Optional.ofNullable(s.getSpec()).map(StatefulSetSpec::getReplicas).orElse(null))
                .availableReplicas(Optional.ofNullable(s.getStatus()).map(StatefulSetStatus::getAvailableReplicas).orElse(null))
                .build();
    }

    @Override
    public OwnerInfo getDaemonSet(String name, String namespace) {
        DaemonSet d = client.apps().daemonSets().inNamespace(namespace).withName(name).get();
        if (d == null) return null;
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.DAEMONSET)
                .name(Optional.ofNullable(d.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(d.getSpec()).map(s -> s.getSelector())
                        .map(sel -> sel.getMatchLabels()).orElse(Collections.emptyMap()))
                .desiredReplicas(Optional.ofNullable(d.getStatus()).map(DaemonSetStatus::getDesiredNumberScheduled).orElse(null))
                .availableReplicas(Optional.ofNullable(d.getStatus()).map(DaemonSetStatus::getNumberAvailable).orElse(null))
                .build();
    }

    @Override
    public OwnerInfo getReplicaSet(String name, String namespace) {
        ReplicaSet r = client.apps().replicaSets().inNamespace(namespace).withName(name).get();
        if (r == null) return null;
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.REPLICASET)
                .name(Optional.ofNullable(r.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(r.getSpec()).map(ReplicaSetSpec::getSelector)
                        .map(sel -> sel.getMatchLabels()).orElse(Collections.emptyMap()))
                .desiredReplicas(Optional.ofNullable(r.getSpec()).map(ReplicaSetSpec::getReplicas).orElse(null))
                .availableReplicas(Optional.ofNullable(r.getStatus()).map(ReplicaSetStatus::getAvailableReplicas).orElse(null))
                .build();
    }

    @Override
    public OwnerInfo getReplicationController(String name, String namespace) {
        ReplicationController rc = client.replicationControllers().inNamespace(namespace).withName(name).get();
        if (rc == null) return null;
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.REPLICATION_CONTROLLER)
                .name(Optional.ofNullable(rc.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(rc.getSpec()).map(ReplicationControllerSpec::getSelector)
                        .orElse(Collections.emptyMap()))
                .desiredReplicas(Optional.ofNullable(rc.getSpec()).map(ReplicationControllerSpec::getReplicas).orElse(null))
                .availableReplicas(Optional.ofNullable(rc.getStatus()).map(ReplicationControllerStatus::getAvailableReplicas).orElse(null))
                .build();
    }

    @Override
    public OwnerInfo getJob(String name, String namespace) {
        Job job = client.batch().v1().jobs().inNamespace(namespace).withName(name).get();
        if (job == null) return null;
        JobStatusInfo jobStatusInfo = Optional.ofNullable(job.getStatus())
                .map(s -> JobStatusInfo.builder()
                        .active(s.getActive())
                        .succeeded(s.getSucceeded())
                        .failed(s.getFailed())
                        .startTime(s.getStartTime())
                        .completionTime(s.getCompletionTime())
                        .build())
                .orElse(null);
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.JOB)
                .name(Optional.ofNullable(job.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(job.getSpec()).map(JobSpec::getSelector)
                        .map(sel -> sel.getMatchLabels()).orElse(Collections.emptyMap()))
                .desiredReplicas(1)
                .availableReplicas(Optional.ofNullable(job.getStatus()).map(JobStatus::getSucceeded).orElse(null))
                .jobStatus(jobStatusInfo)
                .build();
    }

    @Override
    public OwnerInfo getCronJob(String name, String namespace) {
        CronJob cj = client.batch().v1().cronjobs().inNamespace(namespace).withName(name).get();
        if (cj == null) return null;
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.CRON_JOB)
                .name(Optional.ofNullable(cj.getMetadata()).map(ObjectMeta::getName).orElse(null))
                .exists(true)
                .selector(Optional.ofNullable(cj.getSpec())
                        .map(CronJobSpec::getJobTemplate)
                        .map(JobTemplateSpec::getSpec)
                        .map(js -> js.getTemplate())
                        .map(t -> t.getMetadata())
                        .map(ObjectMeta::getLabels)
                        .orElse(Collections.emptyMap()))
                .desiredReplicas(1)
                .lastSuccessfulTime(Optional.ofNullable(cj.getStatus())
                        .map(CronJobStatus::getLastSuccessfulTime).orElse(null))
                .build();
    }
}
