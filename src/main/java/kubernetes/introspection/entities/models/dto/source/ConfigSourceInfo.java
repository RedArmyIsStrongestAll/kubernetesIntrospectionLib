package kubernetes.introspection.entities.models.dto.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Информация об источнике конфигурации, используемом подом.
 * <p>
 * Представляет ссылку на ConfigMap или Secret, которые предоставляют
 * переменные окружения или файлы конфигурации для контейнеров.
 * Безопасная DTO - содержит только имена ключей, без значений.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigSourceInfo {
    /**
     * Имя ресурса (ConfigMap или Secret) в Kubernetes.
     */
    private String name;

    /**
     * Тип источника: CONFIG_MAP (несекретные данные) или SECRET (пароли, токены).
     */
    private ConfigSourceType type;

    /**
     * Список имен ключей (переменных), определенных в этом источнике.
     * Без значений - только имена для интроспекции.
     */
    private List<String> keys;

    /**
     * Способ использования источника контейнерами пода.
     *
     * @see ConfigUsageType
     */
    private ConfigUsageType usageType;
}
