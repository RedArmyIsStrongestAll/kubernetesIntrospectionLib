package kubernetes.introspection.entities.models.dto.service;

public class EndpointAddress {
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

