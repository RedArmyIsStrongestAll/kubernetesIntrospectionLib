package kubernetes.introspection.entities.models.dto.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Реальные эндпоинты (поды), которые получают трафик от сервиса.
 * Содержат IP, имя пода и порт для каждого готового пода.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class ServiceEndpointAddress {
    /**
     * IP адрес пода
     */
    private String ip;

    /**
     * Имя пода
     */
    private String podName;

    /**
     * Порт, на который приходит трафик
     */
    private Integer port;

    /**
     * Готов ли под принимать трафик
     * (соответствует readinessProbe)
     */
    private boolean ready;

    /**
     * Целевой объект (обычно Pod)
     */
    private String targetKind;

    /**
     * Имя целевого объекта
     */
    private String targetName;
}

