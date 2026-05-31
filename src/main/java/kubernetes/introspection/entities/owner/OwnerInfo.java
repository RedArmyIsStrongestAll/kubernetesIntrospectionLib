package kubernetes.introspection.entities.owner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class OwnerInfo {
    private OwnerTypeEnum type;
    private String name;
    private boolean exists;
    private Map<String, String> selector;
    private Integer desiredReplicas;
    private Integer availableReplicas;
    private JobStatusInfo jobStatus;
    private String lastSuccessfulTime;
}
