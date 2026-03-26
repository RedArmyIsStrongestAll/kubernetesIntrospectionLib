package kubernetes.introspection.entities.models.dto.permision;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ResourcePermission {
    // Pod ресурсы
    POD_GET("pods", "get"),
    POD_LIST("pods", "list"),
    POD_WATCH("pods", "watch"),

    // Service ресурсы
    SERVICE_GET("services", "get"),
    SERVICE_LIST("services", "list"),
    SERVICE_WATCH("services", "watch"),

    // Owner ресурсы
    DEPLOYMENT_GET("deployments", "get"),
    DEPLOYMENT_LIST("deployments", "list"),
    DEPLOYMENT_WATCH("deployments", "watch"),

    STATEFULSET_GET("statefulsets", "get"),
    STATEFULSET_LIST("statefulsets", "list"),
    STATEFULSET_WATCH("statefulsets", "watch"),

    DAEMONSET_GET("daemonsets", "get"),
    DAEMONSET_LIST("daemonsets", "list"),
    DAEMONSET_WATCH("daemonsets", "watch"),

    REPLICASET_GET("replicasets", "get"),
    REPLICASET_LIST("replicasets", "list"),
    REPLICASET_WATCH("replicasets", "watch"),

    REPLICATION_CONTROLLER_GET("replicationcontrollers", "get"),
    REPLICATION_CONTROLLER_LIST("replicationcontrollers", "list"),
    REPLICATION_CONTROLLER_WATCH("replicationcontrollers", "watch"),

    JOB_GET("jobs", "get"),
    JOB_LIST("jobs", "list"),
    JOB_WATCH("jobs", "watch"),

    CRONJOB_GET("cronjobs", "get"),
    CRONJOB_LIST("cronjobs", "list"),
    CRONJOB_WATCH("cronjobs", "watch"),

    // Config ресурсы
    CONFIGMAP_GET("configmaps", "get"),
    CONFIGMAP_LIST("configmaps", "list"),
    CONFIGMAP_WATCH("configmaps", "watch"),

    SECRET_GET("secrets", "get"),
    SECRET_LIST("secrets", "list"),
    SECRET_WATCH("secrets", "watch");

    private final String resource;
    private final String verb;

    ResourcePermission(String resource, String verb) {
        this.resource = resource;
        this.verb = verb;
    }

    public SsarRequestDto toSsarRequest(String namespace) {
        return new SsarRequestDto(resource, verb, namespace);
    }

    public static List<ResourcePermission> getPodPermissions() {
        return Arrays.asList(POD_GET, POD_LIST, POD_WATCH);
    }

    public static List<ResourcePermission> getServicePermissions() {
        return Arrays.asList(SERVICE_GET, SERVICE_LIST, SERVICE_WATCH);
    }

    public static List<ResourcePermission> getOwnerPermissions() {
        return Arrays.asList(
                DEPLOYMENT_GET, DEPLOYMENT_LIST, DEPLOYMENT_WATCH,
                STATEFULSET_GET, STATEFULSET_LIST, STATEFULSET_WATCH,
                DAEMONSET_GET, DAEMONSET_LIST, DAEMONSET_WATCH,
                REPLICASET_GET, REPLICASET_LIST, REPLICASET_WATCH,
                REPLICATION_CONTROLLER_GET, REPLICATION_CONTROLLER_LIST, REPLICATION_CONTROLLER_WATCH,
                JOB_GET, JOB_LIST, JOB_WATCH,
                CRONJOB_GET, CRONJOB_LIST, CRONJOB_WATCH
        );
    }

    public static List<ResourcePermission> getConfigPermissions() {
        return Arrays.asList(
                CONFIGMAP_GET, CONFIGMAP_LIST, CONFIGMAP_WATCH,
                SECRET_GET, SECRET_LIST, SECRET_WATCH
        );
    }
}
