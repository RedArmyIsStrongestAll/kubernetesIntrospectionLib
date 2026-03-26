package kubernetes.introspection.entities.models.dto.permision;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SsarRequestDto {
    private String resource;
    private String verb;
    private String namespace;
}
