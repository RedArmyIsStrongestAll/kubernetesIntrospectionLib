package kubernetes.introspection.useCases.ports;

import kubernetes.introspection.entities.owner.OwnerInfo;

public interface KubernetesOwnerPort {
    OwnerInfo getDeployment(String name, String namespace);

    OwnerInfo getStatefulSet(String name, String namespace);

    OwnerInfo getDaemonSet(String name, String namespace);

    OwnerInfo getReplicaSet(String name, String namespace);

    OwnerInfo getReplicationController(String name, String namespace);

    OwnerInfo getJob(String name, String namespace);

    OwnerInfo getCronJob(String name, String namespace);
}
