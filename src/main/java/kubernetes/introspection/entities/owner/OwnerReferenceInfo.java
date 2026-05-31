package kubernetes.introspection.entities.owner;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OwnerReferenceInfo {
    private String kind;
    private String name;
    private String uid;
    private String apiVersion;
    private Boolean controller;
}
