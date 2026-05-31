package kubernetes.introspection.entities.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
public class PodInfo {
    private String name;
    private String uid;
    private String namespace;
    private Map<String, String> labels;

    private String phase;
    private String qosClass;

    private List<ContainerInfo> containers;

    private String creationTimestamp;
    private String deletionTimestamp;

    private String nodeName;
    private String podIP;

    private List<OwnerReferenceInfo> ownerReferences;

    private Map<String, Set<ConfigUsageTypeEnum>> configMapRefs;
    private Map<String, Set<ConfigUsageTypeEnum>> secretRefs;
}
