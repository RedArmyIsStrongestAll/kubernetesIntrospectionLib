package kubernetes.introspection.entities.enviroment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * Информация об ошибке при сборе данных Kubernetes окружения.
 * <p>
 * Позволяет понять, какие данные не удалось получить и почему.
 * Не фатально - остальные данные могут быть доступны.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class CollectionError {
    /**
     * Тип ресурса, при сборе которого произошла ошибка.
     * Например: Pod, Service, ConfigMap, Secret, Owner
     */
    private String resourceType;

    /**
     * Имя ресурса (если известно).
     * Например: "myapp-pod", "database-secret"
     */
    private String resourceName;

    /**
     * Пространство имен ресурса.
     */
    private String namespace;

    /**
     * Код ошибки для программной обработки.
     * Например: NOT_FOUND, FORBIDDEN, TIMEOUT, SERVER_ERROR
     */
    private ErrorCodeEnum errorCodeEnum;

    /**
     * Человеко-читаемое описание ошибки.
     * Например: "ReplicaSet 'myapp-7d9f5d' not found"
     */
    private String message;

    /**
     * Детали ошибки (stack trace, причину) - только если безопасно.
     * Может быть null в production.
     */
    private String details;

    /**
     * Время возникновения ошибки.
     */
    private String timestamp;
}
