package kubernetes.introspection.entities.models.dto.owner;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Тип ресурса Kubernetes, управляющего подом (workload controller).
 * <p>
 * Определяет стратегию создания, обновления и масштабирования подов.
 * Каждый тип имеет свои особенности поведения, влияющие на ожидания пользователя
 * от работы приложения.
 * </p>
 *
 * <h2>Ключевые различия:</h2>
 * <ul>
 *   <li><b>Deployment</b> - обеспечивает обновление приложения (rolling update),
 *       создает ReplicaSet для каждой версии. Полезно знать: не застряло ли обновление,
 *       сколько реплик доступно.</li>
 *   <li><b>StatefulSet</b> - гарантирует уникальные имена и порядок запуска,
 *       используется для stateful приложений. Полезно знать: не завис ли под
 *       на определенном порядковом номере.</li>
 *   <li><b>DaemonSet</b> - запускает под на каждом узле кластера.
 *       Полезно знать: на всех ли узлах под запущен.</li>
 *   <li><b>Job</b> - выполняет под до успешного завершения.
 *       Полезно знать: завершился ли успешно или упал.</li>
 *   <li><b>CronJob</b> - создает Job по расписанию.
 *       Полезно знать: был ли успешен последний запуск.</li>
 *   <li><b>ReplicaSet</b> - просто поддерживает заданное количество подов.
 *       Обычно управляется Deployment'ом.</li>
 *   <li><b>Pod</b> - под создан напрямую, без контроллера.
 *       Не будет автоматически восстановлен при падении.</li>
 * </ul>
 */
@Getter
public enum WorkloadType {
    /**
     * Управляет обновлением и масштабированием через ReplicaSet
     */
    DEPLOYMENT("deployment", "Deployment"),

    /**
     * Гарантирует уникальные имена и порядок для stateful приложений
     */
    STATEFULSET("statefulset", "StatefulSet"),

    /**
     * Запускает под на каждом узле кластера
     */
    DAEMONSET("daemonset", "DaemonSet"),

    /**
     * Обеспечивает заданное количество реплик (обычно создается Deployment)
     */
    REPLICASET("replicaset", "ReplicaSet"),

    /**
     * Устаревшая версия ReplicaSet
     */
    REPLICATION_CONTROLLER("replicationcontroller", "ReplicationController"),

    /**
     * Выполняет под до завершения (успех или ошибка)
     */
    JOB("job", "Job"),

    /**
     * Создает Job по расписанию (cron)
     */
    CRON_JOB("cronjob", "CronJob"),

    /**
     * Под создан напрямую, без контроллера
     */
    POD("pod", "Pod"),

    /**
     * Тип не определен или неизвестен
     */
    UNKNOWN("unknown", "Unknown");

    /**
     * Имя для парсинга из Kubernetes API (в нижнем регистре)
     */
    private final String name;

    /**
     * Оригинальное имя, которое возвращает kubectl (как в Kubernetes API)
     */
    private final String originalName;

    WorkloadType(String name, String originalName) {
        this.name = name;
        this.originalName = originalName;
    }

    /**
     * Создает WorkloadType из строки kind, полученной из ownerReference.
     *
     * @param kind строка kind из Kubernetes (Deployment, StatefulSet, и т.д.)
     * @return соответствующий WorkloadType или UNKNOWN если тип не распознан
     */
    public static WorkloadType fromKubernetes(String kind) {
        if (kind == null) return UNKNOWN;

        return switch (kind.toLowerCase()) {
            case "deployment" -> DEPLOYMENT;
            case "statefulset" -> STATEFULSET;
            case "daemonset" -> DAEMONSET;
            case "replicaset" -> REPLICASET;
            case "replicationcontroller" -> REPLICATION_CONTROLLER;
            case "job" -> JOB;
            case "cronjob" -> CRON_JOB;
            case "pod" -> POD;
            default -> UNKNOWN;
        };
    }

    /**
     * Возвращает имя для сериализации в JSON.
     * Используется оригинальное имя из Kubernetes API.
     *
     * @return оригинальное имя типа ресурса
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