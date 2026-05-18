package kubernetes.introspection.entities.source;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Способ использования источника конфигурации в поде.
 * Определяет, как именно ConfigMap или Secret подключен к контейнеру.
 */
@Getter
public enum ConfigUsageTypeEnum {
    /**
     * Используется в отдельных переменных окружения (valueFrom)
     */
    ENV("env", "Env"),

    /**
     * Все ключи загружены как переменные окружения (envFrom)
     */
    ENV_FROM("envfrom", "EnvFrom"),

    /**
     * Примонтирован как том (volumeMount)
     */
    VOLUME("volume", "Volume"),

    /**
     * Смешанное использование (несколько способов)
     */
    MIXED("mixed", "Mixed"),

    /**
     * Способ использования не определен
     */
    UNKNOWN("unknown", "Unknown");

    /**
     * Имя для парсинга из Kubernetes API (в нижнем регистре)
     */
    private final String name;

    /**
     * Оригинальное имя для отображения пользователю
     */
    private final String originalName;

    ConfigUsageTypeEnum(String name, String originalName) {
        this.name = name;
        this.originalName = originalName;
    }

    /**
     * Создает ConfigUsageType на основе анализа использования ресурса в поде.
     *
     * @param hasEnvRef  есть ли ссылки через valueFrom
     * @param hasEnvFrom есть ли ссылки через envFrom
     * @param hasVolume  есть ли ссылки через volume
     * @return соответствующий ConfigUsageType
     */
    public static ConfigUsageTypeEnum fromUsage(boolean hasEnvRef, boolean hasEnvFrom, boolean hasVolume) {
        int count = 0;
        if (hasEnvRef) count++;
        if (hasEnvFrom) count++;
        if (hasVolume) count++;

        if (count == 0) return UNKNOWN;
        if (count > 1) return MIXED;

        if (hasEnvRef) return ENV;
        if (hasEnvFrom) return ENV_FROM;
        if (hasVolume) return VOLUME;

        return UNKNOWN;
    }

    /**
     * Возвращает имя для сериализации в JSON.
     *
     * @return имя типа использования
     */
    @JsonValue
    public String toJson() {
        return this.name();
    }

    @Override
    public String toString() {
        return this.originalName;
    }
}