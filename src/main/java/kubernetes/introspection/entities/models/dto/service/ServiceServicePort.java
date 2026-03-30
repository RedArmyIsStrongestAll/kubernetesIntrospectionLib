package kubernetes.introspection.entities.models.dto.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Информация о порте сервиса
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class ServiceServicePort {
    /**
     * Имя порта (если есть)
     */
    private String name;

    /**
     * Протокол: TCP/UDP
     */
    private String protocol;

    /**
     * Порт, который слушает сервис
     */
    private int port;

    /**
     * Порт на ноде (для NodePort)
     */
    private Integer nodePort;

    /**
     * Порт на поде (если отличается от port)
     */
    private Integer targetPort;
}
