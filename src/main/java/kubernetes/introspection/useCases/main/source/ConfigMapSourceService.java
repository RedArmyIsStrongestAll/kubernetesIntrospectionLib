package kubernetes.introspection.useCases.main.source;

import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.entities.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.ports.KubernetesConfigPort;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ConfigMapSourceService {

    private final KubernetesConfigPort configPort;
    private final String namespace;

    public ConfigMapSourceService(KubernetesConfigPort configPort, String namespace) {
        this.configPort = configPort;
        this.namespace = namespace;
    }


    public static ConfigSourceInfo mapToConfigSourceInfo(String name, List<String> keys, ConfigUsageTypeEnum usageType) {
        List<String> keyList = keys != null ? new ArrayList<>(keys) : Collections.emptyList();
        return ConfigSourceInfo.builder()
                .name(name)
                .type(ConfigSourceTypeEnum.CONFIG_MAP)
                .keys(keyList)
                .usageType(usageType)
                .build();
    }


    public ConfigMapDto getConfigMapSourcesWithPermission(PodInfo podInfo, PermissionInfo permissionInfo) {
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(
                    ResourcePermissionEnum.CONFIGMAPS_GET,
                    ResourcePermissionEnum.CONFIGMAPS_LIST
            ));
            return getConfigMapSources(podInfo);
        } catch (Exception e) {
            log.error("Error getting ConfigMap sources", e);
            throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
        }
    }

    public ConfigMapDto getConfigMapSources(PodInfo podInfo) {
        try {
            Map<String, Set<ConfigUsageTypeEnum>> configMapRefs =
                    podInfo.getConfigMapRefs() != null ? podInfo.getConfigMapRefs() : Collections.emptyMap();

            if (configMapRefs.isEmpty()) {
                throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
            }

            List<ConfigSourceInfo> configSourceInfoList = configMapRefs.entrySet().stream()
                    .map(entry -> {
                        String name = entry.getKey();
                        List<String> keys = configPort.getConfigMapKeys(name, namespace);
                        if (keys == null) return null;
                        Set<ConfigUsageTypeEnum> usageTypes = entry.getValue();
                        ConfigUsageTypeEnum usageType = usageTypes.size() == 1
                                ? usageTypes.iterator().next()
                                : ConfigUsageTypeEnum.MIXED;
                        return mapToConfigSourceInfo(name, keys, usageType);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new ConfigMapDto(configSourceInfoList);
        } catch (Exception e) {
            log.error("Error getting ConfigMap sources", e);
            throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
        }
    }


    @Getter
    public static class ConfigMapDto {
        private final List<ConfigSourceInfo> configSourceInfoList;

        public ConfigMapDto(List<ConfigSourceInfo> configSourceInfoList) {
            this.configSourceInfoList = configSourceInfoList;
        }

        public ConfigMapDto() {
            this.configSourceInfoList = null;
        }
    }
}
