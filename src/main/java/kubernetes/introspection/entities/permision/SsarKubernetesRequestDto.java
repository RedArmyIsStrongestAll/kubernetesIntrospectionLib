package kubernetes.introspection.entities.permision;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SsarKubernetesRequestDto {
    private ResourcePermissionEnum resourcePermissionEnum;
    private String namespace;
}
