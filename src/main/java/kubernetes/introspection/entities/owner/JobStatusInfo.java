package kubernetes.introspection.entities.owner;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class JobStatusInfo {
    private Integer active;
    private Integer succeeded;
    private Integer failed;
    private String startTime;
    private String completionTime;
}
