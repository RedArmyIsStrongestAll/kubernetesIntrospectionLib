package kubernetes.introspection.entities.models.dto.enviroment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.pod.PodInfo;
import kubernetes.introspection.entities.models.dto.service.ServiceInfo;
import kubernetes.introspection.entities.models.dto.source.ConfigSourceInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Полная информация о Kubernetes окружении приложения.
 * <p>
 * Содержит все данные, необходимые для интроспекции:
 * текущий под, его владельца, родственные поды,
 * сервисы и источники конфигурации.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class KubernetesEnvironmentInfo {
    /**
     * Информация о текущем поде, в котором запущено приложение.
     * Содержит статус, контейнеры, метки и основную идентификацию.
     */
    private PodInfo currentPod;

    /**
     * Список родственных подов (другие реплики того же владельца).
     * Содержит базовую информацию о состоянии других экземпляров приложения.
     */
    private List<PodInfo> siblingPods;

    /**
     * Информация о ресурсе, управляющем подом (Deployment/StatefulSet/Job/etc).
     * Позволяет понять, кто создал под и следит за его жизненным циклом.
     */
    private OwnerInfo owner;


    /**
     * Список сервисов, через которые можно достучаться до приложения.
     * Включает информацию о типах сервисов, портах и реальных эндпоинтах.
     */
    private List<ServiceInfo> services;

    /**
     * Список источников конфигурации, используемых подом.
     * ConfigMap и Secret с их ключами (только имена, без значений).
     */
    private List<ConfigSourceInfo> configSources;


    /**
     * Версия Kubernetes API кластера.
     * Полезно для диагностики совместимости.
     */
    private String kubernetesVersion;

    /**
     * Время сбора информации (timestamp).
     */
    private String collectionTimestamp;


    /**
     * Ошибки, которые помещали собрать информацию
     */
    private List<CollectionError> errors;
}

