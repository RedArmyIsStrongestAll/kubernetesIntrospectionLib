package kubernetes.introspection.entities.source;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Тип источника конфигурации, используемый подом.
 * <p>
 * Определяет характер данных, хранящихся в источнике:
 * конфигурационные параметры (ConfigMap) или чувствительные данные (Secret).
 * </p>
 */
@Getter
public enum ConfigSourceTypeEnum {
    /**
     * Несекретные данные конфигурации
     */
    CONFIG_MAP("configmap", "ConfigMap"),

    /**
     * Секретные данные (пароли, токены, ключи)
     */
    SECRET("secret", "Secret");

    /**
     * Имя для парсинга из Kubernetes API (в нижнем регистре)
     */
    private final String name;

    /**
     * Оригинальное имя, которое возвращает kubectl (как в Kubernetes API)
     */
    private final String originalName;

    ConfigSourceTypeEnum(String name, String originalName) {
        this.name = name;
        this.originalName = originalName;
    }

    /**
     * Создает ConfigSourceType из строки kind, полученной из Kubernetes API.
     *
     * @param kind строка kind из Kubernetes (ConfigMap, Secret)
     * @return соответствующий ConfigSourceType или null если тип неизвестен
     */
    public static ConfigSourceTypeEnum fromKubernetes(String kind) {
        if (kind == null) return null;
        return switch (kind.toLowerCase()) {
            case "configmap" -> CONFIG_MAP;
            case "secret" -> SECRET;
            default -> null;
        };
    }

    /**
     * Возвращает имя для сериализации в JSON.
     * Используется оригинальное имя из Kubernetes API.
     *
     * @return оригинальное имя типа источника
     */
    @JsonValue
    public String toJson() {
        return this.originalName;
    }

    @Override
    public String toString() {
        return this.originalName;
    }
}
