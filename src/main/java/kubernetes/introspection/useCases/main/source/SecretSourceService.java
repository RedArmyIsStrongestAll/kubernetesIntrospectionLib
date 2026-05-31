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
public class SecretSourceService {

    private final KubernetesConfigPort configPort;
    private final String namespace;

    public SecretSourceService(KubernetesConfigPort configPort, String namespace) {
        this.configPort = configPort;
        this.namespace = namespace;
    }


    public static ConfigSourceInfo mapToConfigSourceInfo(String name, List<String> keys, ConfigUsageTypeEnum usageType) {
        List<String> keyList = keys != null ? new ArrayList<>(keys) : Collections.emptyList();
        return ConfigSourceInfo.builder()
                .name(name)
                .type(ConfigSourceTypeEnum.SECRET)
                .keys(keyList)
                .usageType(usageType)
                .build();
    }


    public SecretDto getSecretSourcesWithPermission(PodInfo podInfo, PermissionInfo permissionInfo) {
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(
                    ResourcePermissionEnum.SECRETS_GET,
                    ResourcePermissionEnum.SECRETS_LIST
            ));
            return getSecretSources(podInfo);
        } catch (Exception e) {
            log.error("Error getting Secret sources", e);
            throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
        }
    }

    public SecretDto getSecretSources(PodInfo podInfo) {
        try {
            Map<String, Set<ConfigUsageTypeEnum>> secretRefs =
                    podInfo.getSecretRefs() != null ? podInfo.getSecretRefs() : Collections.emptyMap();

            if (secretRefs.isEmpty()) {
                throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
            }

            List<ConfigSourceInfo> configSourceInfoList = secretRefs.entrySet().stream()
                    .map(entry -> {
                        String name = entry.getKey();
                        List<String> keys = configPort.getSecretKeys(name, namespace);
                        if (keys == null) return null;
                        Set<ConfigUsageTypeEnum> usageTypes = entry.getValue();
                        ConfigUsageTypeEnum usageType = usageTypes.size() == 1
                                ? usageTypes.iterator().next()
                                : ConfigUsageTypeEnum.MIXED;
                        return mapToConfigSourceInfo(name, keys, usageType);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new SecretDto(configSourceInfoList);
        } catch (Exception e) {
            log.error("Error getting Secret sources", e);
            throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
        }
    }


    @Getter
    public static class SecretDto {
        private final List<ConfigSourceInfo> configSourceInfoList;

        public SecretDto(List<ConfigSourceInfo> configSourceInfoList) {
            this.configSourceInfoList = configSourceInfoList;
        }

        public SecretDto() {
            this.configSourceInfoList = null;
        }
    }
}
