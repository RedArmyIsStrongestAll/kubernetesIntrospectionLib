package kubernetes.introspection.entities.models.dto.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Информация о контейнере в поде.
 * Содержит идентификацию, состояние, причины ошибок и историю рестартов.
 *
 * @see ContainerState возможные состояния контейнера
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class ContainerInfo {
    /**
     * Имя контейнера (из spec.containers[].name)
     */
    private String name;

    /**
     * Образ контейнера (из spec.containers[].image)
     */
    private String image;

    /**
     * ID образа (sha256 хеш)
     */
    private String imageID;

    /**
     * ID контейнера в runtime (docker/containerd)
     */
    private String containerID;


    /**
     * Текущее состояние контейнера
     */
    private ContainerState state;

    /**
     * Причина текущего состояния (CrashLoopBackOff, ImagePullBackOff и т.д.)
     */
    private String stateReason;


    /**
     * Количество рестартов контейнера
     */
    private int restartCount;

    /**
     * Причина последнего завершения (если было)
     */
    private String lastTerminationReason;

    /**
     * Детальное сообщение для WAITING состояния
     */
    private String waitingMessage;
}
