package kubernetes.introspection.entities.models.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
    NOT_IN_CLUSTER(400, "Not in cluster",
            "The application is not running in a Kubernetes cluster.",
            true),

    NOT_NAMESPACE(400, "Not namespace",
            "The application is running in a Kubernetes cluster, but cannot read 'namespace' from file '/var/run/secrets/kubernetes.io/serviceaccount/namespace'",
            true),

    FORBIDDEN(403, "Access denied",
            "The application (as pod) does not have access to the resource due to RBAC rules",
            false);

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

    ErrorCode(int code, String name, String message, boolean critical) {
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