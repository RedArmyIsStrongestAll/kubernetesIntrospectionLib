package engine;

import io.fabric8.kubernetes.api.model.Pod;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PodAnalyzer {

    private final Pod pod;
    private final RbacAnalyzer rbacAnalyzer;


    private final ResourcePermissionEnum PODS_GET = ResourcePermissionEnum.PODS_GET;
    private final ResourcePermissionEnum PODS_LIST = ResourcePermissionEnum.PODS_LIST;

    public PodAnalyzer(String podYaml, RbacAnalyzer rbacAnalyzer) {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        this.pod = yaml.loadAs(podYaml, Pod.class);

        this.rbacAnalyzer = rbacAnalyzer;
    }

    public Pod getPodByName(PermissionInfo permissionInfo,
                            String requestedName,
                            String requestedNamespace) {

        if (!hasPermission(permissionInfo, ResourcePermissionEnum.PODS_GET)) return null;
        if (!rbacAnalyzer.isAllowed("pods", "get", requestedNamespace)) return null;

        if (pod.getMetadata() == null) return null;

        if (!pod.getMetadata().getName().equals(requestedName)) return null;
        if (!pod.getMetadata().getNamespace().equals(requestedNamespace)) return null;

        return pod;
    }

    public Pod getPodListByIp(PermissionInfo permissionInfo,
                              String requestedIp,
                              String requestedNamespace) {

        if (!hasPermission(permissionInfo, ResourcePermissionEnum.PODS_LIST)) return null;
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) return null;

        if (pod.getMetadata() == null) return null;

        if (pod.getStatus() == null || pod.getStatus().getPodIP() == null) return null;
        if (!pod.getStatus().getPodIP().equals(requestedIp)) return null;
        if (!pod.getMetadata().getNamespace().equals(requestedNamespace)) return null;

        return pod;
    }

    public List<Pod> getPodsByLabels(PermissionInfo permissionInfo,
                                     Map<String, String> requestedLabels,
                                     String requestedNamespace) {

        if (!hasPermission(permissionInfo, ResourcePermissionEnum.PODS_LIST)) return Collections.emptyList();
        if (!rbacAnalyzer.isAllowed("pods", "list", requestedNamespace)) return Collections.emptyList();

        if (pod.getMetadata() == null || pod.getMetadata().getLabels() == null) return Collections.emptyList();

        Map<String, String> podLabels = pod.getMetadata().getLabels();

        boolean containsAllLabels = requestedLabels.entrySet().stream()
                .allMatch(e -> e.getValue().equals(podLabels.get(e.getKey())));

        if (!containsAllLabels) return Collections.emptyList();
        if (!pod.getMetadata().getNamespace().equals(requestedNamespace)) return Collections.emptyList();

        return Collections.singletonList(pod);
    }


    private boolean hasPermission(PermissionInfo permissionInfo, ResourcePermissionEnum requiredPermission) {
        return permissionInfo.getPermissions().stream()
                .anyMatch(p -> p.getResource() == requiredPermission && p.isAllowed());
    }
}