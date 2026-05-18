package kubernetes.introspection.entities.owner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Информация о ресурсе, владеющем подом.
 * Позволяет понять, кто создал под и в каком состоянии находится контроллер.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class OwnerInfo {
    /**
     * Тип владельца (Deployment, StatefulSet, и т.д.)
     */
    private OwnerTypeEnum type;

    /**
     * Имя ресурса-владельца
     */
    private String name;

    /**
     * Существует ли ресурс в кластере
     */
    private boolean exists;

    /**
     * Селекторы, связывающие сервис с подами
     * Позволяют понять, какие поды получают трафик
     */
    private Map<String, String> selector;


    /**
     * Желаемое количество подов (для Deployment/ReplicaSet/StatefulSet)
     */
    private Integer desiredReplicas;

    /**
     * Текущее количество готовых подов
     */
    private Integer availableReplicas;


    /**
     * Статус для Job: Active/Succeeded/Failed
     */
    private JobStatus jobStatus;

    /**
     * Для CronJob: время последнего успешного запуска
     */
    private String lastSuccessfulTime;
}
