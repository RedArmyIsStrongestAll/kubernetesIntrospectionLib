package kubernetes.introspection.entities.models.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    NOT_IN_CLUSTER(400, "Not in cluster",
            "The application is not running in a Kubernetes cluster.",
            true),

    NOT_NAMESPACE(400, "Not namespace",
            "The application is running in a Kubernetes cluster, but cannot read 'namespace' from file '/var/run/secrets/kubernetes.io/serviceaccount/namespace'",
            true),

    NOT_CREATE_K8S_CLIENT(500, "Failed to create kubernetes client: {}",
            "Failed to create a client to access the Kubernetes cluster",
            true),

    RBAC_NOT_FOUND(400, "Resource not found",
            "Current RBAC rules not found",
            true),

    NO_STATIC_FILE_RBAC(400, "No static file RBAC",
            "The application cannot return the RBAC file template, the file was not found.",
            false),

    ERROR_READ_FILE_RBAC(400, "Error read file RBAC",
            "The application could not read the RBAC template file.",
            false),

    FORBIDDEN(403, "Access denied",
            "The application (as pod) does not have access to the resource due to RBAC rules",
            false),

    BROKEN_NAME_IN_POD(400, "Broken name in pod",
            "The application does not have a pod name set for the pod.",
            true),

    POD_NOT_FOUND(400, "Resource not found",
            "Current pod not found",
            true),

    OWNER_REFERENCE_NOT_FOUND(404, "Resource not found",
            "Current owner reference (link on parent) not found",
            false),

    OWNER_NOT_FOUND(400, "Resource not found",
            "Current owner not found",
            false),

    OWNER_REALIZED_NOT_FOUND(400, "Resource not found",
            "Owners resource not found: {}",
            false),

    REPLICA_PODS_NOT_FOUND(400, "Resource not found",
            "Replica pods not found",
            false),

    SERVICE_NOT_FOUND(400, "Resource not found",
            "Service not found",
            false),

    SERVICE_MANY_FOUND(400, "Resource not found",
            "Service more than one found",
            false),

    ENDPOINTS_NOT_FOUND(400, "Resource not found",
            "Endpoint not found",
            false),

    ENDPOINTS_MANY_FOUND(400, "Resource not found",
            "Endpoint more than one found",
            false),

    CONFIG_MAP_NOT_FOUND(400, "Resource not found",
            "ConfigMap not found",
            false),

    SECRET_NOT_FOUND(400, "Resource not found",
            "Secrets not found",
            false);


    private final int code;
    private final String name;
    private final String message;
    private final boolean critical;

    ErrorCodeEnum(int code, String name, String message, boolean critical) {
        this.code = code;
        this.name = name;
        this.message = message;
        this.critical = critical;
    }
}