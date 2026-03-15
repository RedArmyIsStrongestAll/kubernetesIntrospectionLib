package kubernetes.introspection.entities.models.dto.owner;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Статус выполнения Job в Kubernetes.
 * <p>
 * Отражает текущее состояние Job и ее подов.
 * Job считается завершенной только когда все поды выполнились успешно (Succeeded)
 * или достигнут лимит повторов (Failed).
 * </p>
 *
 * <h2>Переходы состояний:</h2>
 * <ul>
 *   <li><b>Active</b> → <b>Succeeded</b> - все поды завершились успешно</li>
 *   <li><b>Active</b> → <b>Failed</b> - превышен лимит повторов или время</li>
 *   <li><b>Active</b> → <b>Active</b> - поды перезапускаются при ошибках</li>
 * </ul>
 */
@Getter
public enum JobStatus {
    /**
     * Job выполняется: поды созданы и работают
     */
    ACTIVE("active", "Active"),

    /**
     * Job успешно завершена: все поды выполнились с кодом 0
     */
    SUCCEEDED("succeeded", "Succeeded"),

    /**
     * Job завершилась с ошибкой: превышены лимиты повторов
     */
    FAILED("failed", "Failed");

    /**
     * Имя для парсинга из Kubernetes API (в нижнем регистре)
     */
    private final String name;

    /**
     * Оригинальное имя, которое возвращает kubectl (как в Kubernetes API)
     */
    private final String originalName;

    JobStatus(String name, String originalName) {
        this.name = name;
        this.originalName = originalName;
    }

    /**
     * Создает JobStatus из полей статуса Job, полученных из Kubernetes API.
     * <p>
     * Job в Kubernetes может находиться в одном из трех состояний:
     * <ul>
     *   <li><b>ACTIVE</b> - Job выполняется (есть активные поды, нет времени завершения)</li>
     *   <li><b>SUCCEEDED</b> - Job успешно завершена (все поды выполнены, есть время завершения, нет failed)</li>
     *   <li><b>FAILED</b> - Job завершилась с ошибкой (есть failed поды, есть время завершения)</li>
     * </ul>
     * </p>
     *
     * <h2>Логика определения:</h2>
     * <ol>
     *   <li>Если completionTime != null - Job завершена:
     *     <ul>
     *       <li>failed > 0 -> FAILED (были ошибки)</li>
     *       <li>иначе -> SUCCEEDED (все хорошо)</li>
     *     </ul>
     *   </li>
     *   <li>Если completionTime == null - Job еще выполняется -> ACTIVE</li>
     * </ol>
     *
     * <h2>Примеры из fabric8:</h2>
     * <pre>
     * // Job в процессе
     * Job job = client.batch().v1().jobs().inNamespace("default").withName("backup-job").get();
     * JobStatus status = JobStatus.fromKubernetes(
     *     job.getStatus().getActive(),      // 2 пода работают
     *     job.getStatus().getSucceeded(),   // 0 пока не завершились
     *     job.getStatus().getFailed(),      // 0 падений нет
     *     job.getStatus().getCompletionTime() // null
     * ); // Результат: ACTIVE
     *
     * // Job успешно завершена
     * JobStatus status = JobStatus.fromKubernetes(
     *     0,  // active
     *     5,  // succeeded - все 5 подов успешны
     *     0,  // failed
     *     "2024-01-15T10:30:00Z"  // есть время
     * ); // Результат: SUCCEEDED
     *
     * // Job с ошибками
     * JobStatus status = JobStatus.fromKubernetes(
     *     0,  // active
     *     3,  // succeeded - 3 успешны
     *     2,  // failed - 2 упали
     *     "2024-01-15T10:30:00Z"
     * ); // Результат: FAILED
     * </pre>
     *
     * @param active         количество активных подов (может быть null)
     * @param succeeded      количество успешно завершенных подов (может быть null)
     * @param failed         количество упавших подов (может быть null)
     * @param completionTime время завершения Job в формате RFC3339 (null если не завершена)
     * @return соответствующий JobStatus (ACTIVE/SUCCEEDED/FAILED)
     */
    public static JobStatus fromKubernetes(Integer active, Integer succeeded, Integer failed, String completionTime) {
        if (completionTime != null) {
            // Job завершена
            return failed > 0 ? FAILED : SUCCEEDED;
        } else {
            // Job еще выполняется
            return ACTIVE;
        }
    }

    /**
     * Упрощенный вариант - из строки условия.
     */
    public static JobStatus fromKubernetes(String condition) {
        if (condition == null) return null;
        return switch (condition.toLowerCase()) {
            case "complete" -> SUCCEEDED;
            case "failed" -> FAILED;
            default -> ACTIVE;
        };
    }

    /**
     * Возвращает имя для сериализации в JSON.
     * Используется оригинальное имя из Kubernetes API.
     *
     * @return оригинальное имя статуса Job
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