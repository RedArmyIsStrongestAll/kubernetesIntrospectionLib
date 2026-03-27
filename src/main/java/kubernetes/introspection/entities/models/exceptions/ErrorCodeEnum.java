package kubernetes.introspection.entities.models.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    NOT_IN_CLUSTER(500, "Not in cluster",
            "The application is not running in a Kubernetes cluster.",
            true),

    NOT_NAMESPACE(500, "Not namespace",
            "The application is running in a Kubernetes cluster, but cannot read 'namespace' from file '/var/run/secrets/kubernetes.io/serviceaccount/namespace'",
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

    BROKEN_NAME_IN_POD(500, "Broken name in pod",
            "The application does not have a pod name set for the pod.",
            true),

    POD_NOT_FOUND(404, "Resource not found", "Current pod not found",true);

    /**
     * Ресурс не найден
     */
//    NOT_FOUND(404, "Resource not found", false),

    /**
     * Нет доступа к ресурсу (RBAC)
     */
//    FORBIDDEN(403, "Access denied", true),

    /**
     * Таймаут при запросе к API
     */
//    TIMEOUT(408, "Request timeout", true),

    /**
     * Внутренняя ошибка Kubernetes API
     */
//    SERVER_ERROR(500, "Internal server error", true),

    /**
     * Ошибка парсинга ответа
     */
//    PARSE_ERROR(422, "Failed to parse response", false),

    /**
     * Неизвестная ошибка
     */
//    UNKNOWN(520, "Unknown error", true);

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

    /**
     * Создает ErrorCode из HTTP статуса.
     */
//    public static ErrorCode fromHttpCode(int httpCode) {
//        return switch (httpCode) {
//            case 404 -> NOT_FOUND;
//            case 403 -> FORBIDDEN;
//            case 408, 504 -> TIMEOUT;
//            case 500, 502, 503 -> SERVER_ERROR;
//            case 422 -> PARSE_ERROR;
//            default -> UNKNOWN;
//        };
//    }
}