package kubernetes.introspection.entities.models.dto.service;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Тип сервиса в Kubernetes.
 * <p>
 * Определяет способ доступа к подам извне и внутри кластера.
 * </p>
 *
 * <h2>Особенности типов:</h2>
 * <ul>
 *   <li><b>CLUSTER_IP</b> - доступ только внутри кластера (по умолчанию)</li>
 *   <li><b>NODE_PORT</b> - открывает порт на каждой ноде (30000-32767)</li>
 *   <li><b>LOAD_BALANCER</b> - создает внешний балансировщик (облако)</li>
 *   <li><b>EXTERNAL_NAME</b> - CNAME запись на внешнее DNS имя</li>
 * </ul>
 */
@Getter
public enum ServiceType {
    /**
     * Доступ только внутри кластера по внутреннему IP
     */
    CLUSTER_IP("clusterip", "ClusterIP"),

    /**
     * Открывает порт на каждой ноде для внешнего доступа
     */
    NODE_PORT("nodeport", "NodePort"),

    /**
     * Создает внешний балансировщик (поддерживается облачными провайдерами)
     */
    LOAD_BALANCER("loadbalancer", "LoadBalancer"),

    /**
     * CNAME запись на внешнее DNS имя (без прокси)
     */
    EXTERNAL_NAME("externalname", "ExternalName");

    /**
     * Имя для парсинга из Kubernetes API (в нижнем регистре)
     */
    private final String name;

    /**
     * Оригинальное имя, которое возвращает kubectl (как в Kubernetes API)
     */
    private final String originalName;

    ServiceType(String name, String originalName) {
        this.name = name;
        this.originalName = originalName;
    }

    /**
     * Создает ServiceType из строки, полученной из Kubernetes API.
     *
     * @param type строка типа сервиса из spec.type (ClusterIP, NodePort, LoadBalancer, ExternalName)
     * @return соответствующий ServiceType или null если тип неизвестен
     */
    public static ServiceType fromKubernetes(String type) {
        if (type == null) return null;
        return switch (type.toLowerCase()) {
            case "clusterip" -> CLUSTER_IP;
            case "nodeport" -> NODE_PORT;
            case "loadbalancer" -> LOAD_BALANCER;
            case "externalname" -> EXTERNAL_NAME;
            default -> null;
        };
    }

    /**
     * Возвращает имя для сериализации в JSON.
     * Используется оригинальное имя из Kubernetes API.
     *
     * @return оригинальное имя типа сервиса
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