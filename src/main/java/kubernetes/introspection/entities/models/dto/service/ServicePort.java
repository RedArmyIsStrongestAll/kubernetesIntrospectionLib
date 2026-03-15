package kubernetes.introspection.entities.models.dto.service;

/**
 * Информация о порте сервиса
 */
public class ServicePort {
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
