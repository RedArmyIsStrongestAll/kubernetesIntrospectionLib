package kubernetes.introspection.entities.pod;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Состояние контейнера в Kubernetes.
 * Соответствует статусам контейнера из ContainerStatus в K8s API.
 *
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.24/#containerstatus-v1-core">
 * Kubernetes ContainerStatus API</a>
 */
@Getter
public enum ContainerStateEnum {
    /**
     * Контейнер работает
     */
    RUNNING("running", "Running"),

    /**
     * Контейнер ожидает запуска (загрузка образа, создание, CrashLoopBackOff)
     */
    WAITING("waiting", "Waiting"),

    /**
     * Контейнер завершил работу
     */
    TERMINATED("terminated", "Terminated");

    /**
     * Имя для парсинга из Kubernetes API (в нижнем регистре)
     */
    private final String name;

    /**
     * Оригинальное имя, которое возвращает kubectl (как в Kubernetes API)
     */
    private final String originalName;

    ContainerStateEnum(String name, String originalName) {
        this.name = name;
        this.originalName = originalName;
    }

    /**
     * Преобразует строковое значение статуса из Kubernetes API в enum.
     *
     * @param state статус из ContainerStatus (running/waiting/terminated)
     * @return соответствующий элемент enum или null если статус неизвестен
     */
    public static ContainerStateEnum parserFromKubernetes(String state) {
        if (state == null) return null;
        return switch (state.toLowerCase()) {
            case "running" -> RUNNING;
            case "waiting" -> WAITING;
            case "terminated" -> TERMINATED;
            default -> null;
        };
    }

    /**
     * Возвращает имя для сериализации в JSON.
     * Используется оригинальное имя из Kubernetes API.
     *
     * @return оригинальное имя состояния контейнера
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