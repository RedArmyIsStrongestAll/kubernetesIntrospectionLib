package kubernetes.introspection.entities.services.main.owner;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.permision.PermissionService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REFERENCE_NOT_FOUND;

@Slf4j
public abstract class OwnerService {

    protected final KubernetesClient kubernetesClient;
    protected final String namespace;

    public OwnerService(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
    }

    public Object getOwnerWithPermission(OwnerReference ownerRef, PermissionInfo permissionInfo) {
        log.info("Start getOwnerWithPermission");
        try {
            PermissionService.checkPermission(permissionInfo, this::getPermissionResource);

            return getOwner(ownerRef);
        } catch (Exception e) {
            log.error("Error getOwnerWithPermission: ", e);
            throw new KubernetesException(OWNER_REFERENCE_NOT_FOUND);
        }
    }

    public OwnerService.OwnerDto getOwner(OwnerReference ownerRef) {
        log.info("Start getOwner");

        try {

           //todo  это какие реадизации классов надо сделать

//            Deployment v1 = kubernetesClient.apps().deployments().inNamespace(namespace).withName(ownerRef.getName()).get();
//
//            StatefulSet v2 = kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(ownerRef.getName()).get();
//
//            DaemonSet v3 = kubernetesClient.apps().daemonSets().inNamespace(namespace).withName(ownerRef.getName()).get();
//
//            ReplicaSet v4 = kubernetesClient.apps().replicaSets().inNamespace(namespace).withName(ownerRef.getName()).get();
//
//            ReplicationController v5 = kubernetesClient.replicationControllers().inNamespace(namespace).withName(ownerRef.getName()).get();
//
//            Job v6 = kubernetesClient.batch().v1().jobs().inNamespace(namespace).withName(ownerRef.getName()).get();
//
//            Pod v7 = kubernetesClient.pods().inNamespace(namespace).withName(ownerRef.getName()).get();

            return null;
        } catch (Exception e) {
            log.error("Error getOwner: ", e);
            throw new KubernetesException(OWNER_REFERENCE_NOT_FOUND);
        }
    }

    protected abstract List<ResourcePermissionEnum> getPermissionResource();

    protected abstract OwnerDto getOwnerDto();

    @AllArgsConstructor
    @Getter
    public static class OwnerDto {
        private OwnerInfo ownerInfo;
        private OwnerTypeEnum k8sType;
        private HasMetadata k8sObject;
    }

}

