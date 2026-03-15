package kubernetes.introspection.entities.models.exceptions;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Код ошибки при сборе информации из Kubernetes.
 */
@Getter
public enum ErrorCode {
    /**
     * Ресурс не найден
     */
    NOT_FOUND("not found"),

    /**
     * Нет доступа к ресурсу (RBAC)
     */
    FORBIDDEN("forbidden"),

    /**
     * Таймаут при запросе к API
     */
    TIMEOUT("timeout"),

    /**
     * Внутренняя ошибка Kubernetes API
     */
    SERVER_ERROR("server error"),

    /**
     * Ошибка парсинга ответа
     */
    PARSE_ERROR("parse error"),

    /**
     * Неизвестная ошибка
     */
    UNKNOWN("unknown");

    /**
     * Имя для внутреннего использования (в нижнем регистре)
     */
    private final String name;

    ErrorCode(String name) {
        this.name = name;
    }

    /**
     * Создает ErrorCode из HTTP статуса или исключения.
     *
     * @param httpCode HTTP код ошибки (404, 403, 500 и т.д.)
     * @return соответствующий ErrorCode
     */
    public static ErrorCode fromHttpCode(int httpCode) {
        return switch (httpCode) {
            case 404 -> NOT_FOUND;
            case 403 -> FORBIDDEN;
            case 408, 504 -> TIMEOUT;
            case 500, 502, 503 -> SERVER_ERROR;
            default -> UNKNOWN;
        };
    }

    /**
     * Возвращает имя для сериализации в JSON.
     *
     * @return имя кода ошибки
     */
    @JsonValue
    public String toJson() {
        return name;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
