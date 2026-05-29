package kubernetes.introspection.entities.permision;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ResourcePermissionEnum {
    // Pod ресурсы
    PODS_GET("pods", "get"),
    PODS_LIST("pods", "list"),
    PODS_WATCH("pods", "watch"),

    // Owner ресурсы
    DEPLOYMENTS_GET("deployments", "get"),
    DEPLOYMENTS_LIST("deployments", "list"),
    DEPLOYMENTS_WATCH("deployments", "watch"),

    STATEFULSETS_GET("statefulsets", "get"),
    STATEFULSETS_LIST("statefulsets", "list"),
    STATEFULSETS_WATCH("statefulsets", "watch"),

    DAEMONSETS_GET("daemonsets", "get"),
    DAEMONSETS_LIST("daemonsets", "list"),
    DAEMONSETS_WATCH("daemonsets", "watch"),

    REPLICASETS_GET("replicasets", "get"),
    REPLICASETS_LIST("replicasets", "list"),
    REPLICASETS_WATCH("replicasets", "watch"),

    REPLICATION_CONTROLLERS_GET("replicationcontrollers", "get"),
    REPLICATION_CONTROLLERS_LIST("replicationcontrollers", "list"),
    REPLICATION_CONTROLLERS_WATCH("replicationcontrollers", "watch"),

    JOBS_GET("jobs", "get"),
    JOBS_LIST("jobs", "list"),
    JOBS_WATCH("jobs", "watch"),

    CRONJOBS_GET("cronjobs", "get"),
    CRONJOBS_LIST("cronjobs", "list"),
    CRONJOBS_WATCH("cronjobs", "watch"),

    // Service ресурсы
    SERVICES_GET("services", "get"),
    SERVICES_LIST("services", "list"),
    SERVICES_WATCH("services", "watch"),

    ENDPOINTS_GET("endpoints", "get"),
    ENDPOINTS_LIST("endpoints", "list"),
    ENDPOINTS_WATCH("endpoints", "watch"),

    // Config ресурсы
    CONFIGMAPS_GET("configmaps", "get"),
    CONFIGMAPS_LIST("configmaps", "list"),
    CONFIGMAPS_WATCH("configmaps", "watch"),

    SECRETS_GET("secrets", "get"),
    SECRETS_LIST("secrets", "list"),
    SECRETS_WATCH("secrets", "watch");

    private final String resource;
    private final String verb;

    ResourcePermissionEnum(String resource, String verb) {
        this.resource = resource;
        this.verb = verb;
    }


    public static List<ResourcePermissionEnum> getActualPermissions() {
        return Arrays.asList(
                PODS_GET, PODS_LIST,

                DEPLOYMENTS_GET, DEPLOYMENTS_LIST,
                STATEFULSETS_GET, STATEFULSETS_LIST,
                DAEMONSETS_GET, DAEMONSETS_LIST,
                REPLICASETS_GET, REPLICASETS_LIST,
                REPLICATION_CONTROLLERS_GET, REPLICATION_CONTROLLERS_LIST,
                JOBS_GET, JOBS_LIST,
                CRONJOBS_GET, CRONJOBS_LIST,

                SERVICES_GET, SERVICES_LIST,
                ENDPOINTS_GET, ENDPOINTS_LIST,

                CONFIGMAPS_GET, CONFIGMAPS_LIST,
                SECRETS_GET, SECRETS_LIST
        );
    }


    public String getStringValue() {
        return resource + "/" + verb;
    }

    public static ResourcePermissionEnum getFromStringValue(String value) {
        for (ResourcePermissionEnum rp : values()) {
            if (rp.getStringValue().equals(value)) {
                return rp;
            }
        }
        return null;
    }

    public static ResourcePermissionEnum findByResourceAndVerb(String resource, String verb) {
        return Arrays.stream(values())
                .filter(p -> p.getResource().equals(resource) && p.getVerb().equals(verb))
                .findFirst().get();
    }
}
