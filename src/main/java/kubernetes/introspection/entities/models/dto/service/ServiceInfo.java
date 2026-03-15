package kubernetes.introspection.entities.models.dto.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Информация о Service в Kubernetes.
 * <p>
 * Сервис обеспечивает сетевой доступ к подам через механизм селекторов.
 * Позволяет понять, как трафик попадает к приложению.
 * </p>
 *
 * <h2>Типы сервисов:</h2>
 * <ul>
 *   <li><b>ClusterIP</b> - доступ только внутри кластера</li>
 *   <li><b>NodePort</b> - открывает порт на каждой ноде</li>
 *   <li><b>LoadBalancer</b> - создает внешний балансировщик</li>
 *   <li><b>ExternalName</b> - CNAME запись</li>
 * </ul>
 *
 * <h2>Связь с подами:</h2>
 * <p>
 * Сервис находит поды по селектору (selector). Реальные IP адреса готовых подов,
 * которые получают трафик, отображаются в списке endpoints.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class ServiceInfo {
    /**
     * Имя сервиса
     */
    private String name;

    /**
     * Тип сервиса: ClusterIP, NodePort, LoadBalancer, ExternalName
     */
    private String type;

    /**
     * Внутренний IP сервиса в кластере (для ClusterIP/NodePort/LoadBalancer)
     */
    private String clusterIP;

    /**
     * Внешний IP (для LoadBalancer, если назначен)
     */
    private String externalIP;

    /**
     * Порты, на которых слушает сервис
     */
    private List<ServicePort> ports;

    /**
     * Селекторы, связывающие сервис с подами
     * Позволяют понять, какие поды получают трафик
     */
    private Map<String, String> selector;

    /**
     * Реальные эндпоинты (поды), которые получают трафик от сервиса.
     * Содержат IP, имя пода и порт для каждого готового пода.
     */
    private List<EndpointAddress> endpoints;

    /**
     * Количество готовых эндпоинтов.
     * Показывает, сколько подов реально обслуживают трафик.
     */
    private int readyEndpoints;

    /**
     * Признак полной готовности сервиса.
     * true если все поды, соответствующие селектору, находятся в endpoints.
     */
    private boolean fullyReady;
}