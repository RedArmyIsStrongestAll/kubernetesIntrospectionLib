package kubernetes.introspection.entities.models.dto.permision;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionInfo {
    private boolean success;
    private List<PermissionInfoDto> permissions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PermissionInfoDto {
        private ResourcePermissionEnum resource;
        private boolean allowed;
    }
}
