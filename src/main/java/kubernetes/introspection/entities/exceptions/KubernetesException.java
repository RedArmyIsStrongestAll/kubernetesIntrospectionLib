package kubernetes.introspection.entities.exceptions;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class KubernetesException extends RuntimeException {
    private final ErrorCodeEnum errorCodeEnum;
}
