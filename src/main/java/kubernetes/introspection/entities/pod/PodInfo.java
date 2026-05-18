package kubernetes.introspection.entities.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Информация о Pod в Kubernetes.
 * Содержит идентификацию, статус и список контейнеров пода.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class PodInfo {
    /**
     * Имя пода
     */
    private String name;

    /**
     * Уникальный идентификатор пода
     */
    private String uid;

    /**
     * Пространство имен
     */
    private String namespace;

    /**
     * Метки пода
     */
    private Map<String, String> labels;


    /**
     * Фаза пода: Pending/Running/Succeeded/Failed/Unknown
     */
    private String phase;

    /**
     * QoS класс приоритета: Guaranteed/Burstable/BestEffort
     */
    private String qosClass;


    /**
     * Список контейнеров пода с их статусами
     */
    private List<ContainerInfo> containers;


    /**
     * Время создания пода
     */
    private String creationTimestamp;

    /**
     * Время удаления (если под помечен на удаление)
     */
    private String deletionTimestamp;


    /**
     * Узел, на котором запущен под
     */
    private String nodeName;

    /**
     * IP адрес пода
     */
    private String podIP;


}