package kubernetes.introspection.entities.services.main.source;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.source.ConfigSourceInfo;
import kubernetes.introspection.entities.models.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.models.source.ConfigUsageTypeEnum;
import kubernetes.introspection.entities.services.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ConfigMapSourceService {

    private final KubernetesClient kubernetesClient;
    private final String namespace;

    public ConfigMapSourceService(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
    }


    public static List<ConfigSourceInfo> mapToConfigSourceInfoList(List<ConfigMap> configMaps,
                                                                   Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        return configMaps.stream()
                .map(cm -> {
                    String name = cm.getMetadata().getName();
                    Set<ConfigUsageTypeEnum> usageTypes = usageTypeMap.getOrDefault(name, Set.of(ConfigUsageTypeEnum.UNKNOWN));
                    ConfigUsageTypeEnum usageType = usageTypes.size() == 1
                            ? usageTypes.iterator().next()
                            : ConfigUsageTypeEnum.MIXED;

                    List<String> keys = new ArrayList<>();
                    if (cm.getData() != null) keys.addAll(cm.getData().keySet());
                    if (cm.getBinaryData() != null) keys.addAll(cm.getBinaryData().keySet());

                    return ConfigSourceInfo.builder()
                            .name(name)
                            .type(ConfigSourceTypeEnum.CONFIG_MAP)
                            .keys(keys)
                            .usageType(usageType)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public static ConfigSourceInfo mapToConfigSourceInfo(ConfigMap configMap, ConfigUsageTypeEnum usageType) {
        String name = configMap.getMetadata().getName();
        List<String> keys = new ArrayList<>();
        if (configMap.getData() != null) keys.addAll(configMap.getData().keySet());
        if (configMap.getBinaryData() != null) keys.addAll(configMap.getBinaryData().keySet());

        return ConfigSourceInfo.builder()
                .name(name)
                .type(ConfigSourceTypeEnum.CONFIG_MAP)
                .keys(keys)
                .usageType(usageType)
                .build();
    }


    public ConfigMapDto getConfigMapSourcesWithPermission(Pod currentPod, PermissionInfo permissionInfo) {
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(
                    ResourcePermissionEnum.CONFIGMAPS_GET,
                    ResourcePermissionEnum.CONFIGMAPS_LIST
            ));
            return getConfigMapSources(currentPod);
        } catch (Exception e) {
            log.error("Error getting ConfigMap sources", e);
            throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
        }
    }

    public ConfigMapDto getConfigMapSources(Pod currentPod) {
        try {
            Set<String> configMapNames = new HashSet<>();
            Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap = new HashMap<>();

            extractFromEnv(currentPod, configMapNames, usageTypeMap);
            extractFromEnvFrom(currentPod, configMapNames, usageTypeMap);
            extractFromVolumes(currentPod, configMapNames, usageTypeMap);

            if (configMapNames.isEmpty()) {
                throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
            }

            List<ConfigMap> configMaps = configMapNames.stream()
                    .map(name -> kubernetesClient.configMaps().inNamespace(namespace).withName(name).get())
                    .filter(Objects::nonNull)
                    .toList();

            return new ConfigMapDto(configMaps, usageTypeMap);

        } catch (Exception e) {
            log.error("Error getting ConfigMap sources", e);
            throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
        }
    }


    private void extractFromEnv(Pod pod, Set<String> configMapNames, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        pod.getSpec().getContainers().forEach(container -> {
            if (container.getEnv() != null) {
                container.getEnv().forEach(envVar -> {
                    if (envVar.getValueFrom() != null && envVar.getValueFrom().getConfigMapKeyRef() != null) {
                        String name = envVar.getValueFrom().getConfigMapKeyRef().getName();
                        configMapNames.add(name);
                        usageTypeMap.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV);
                    }
                });
            }
        });
    }

    private void extractFromEnvFrom(Pod pod, Set<String> configMapNames, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        pod.getSpec().getContainers().forEach(container -> {
            if (container.getEnvFrom() != null) {
                container.getEnvFrom().forEach(envFrom -> {
                    if (envFrom.getConfigMapRef() != null) {
                        String name = envFrom.getConfigMapRef().getName();
                        configMapNames.add(name);
                        usageTypeMap.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV_FROM);
                    }
                });
            }
        });
    }

    private void extractFromVolumes(Pod pod, Set<String> configMapNames, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        pod.getSpec().getVolumes().forEach(volume -> {
            if (volume.getConfigMap() != null) {
                String name = volume.getConfigMap().getName();
                configMapNames.add(name);
                usageTypeMap.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.VOLUME);
            }
        });
    }


    @Getter
    public static class ConfigMapDto {
        private final List<ConfigMap> k8sConfigMapList;
        private final List<ConfigSourceInfo> configSourceInfoList;

        public ConfigMapDto(List<ConfigMap> k8sConfigMapList, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
            this.k8sConfigMapList = k8sConfigMapList;
            this.configSourceInfoList = mapToConfigSourceInfoList(k8sConfigMapList, usageTypeMap);
        }

        public ConfigMapDto() {
            this.k8sConfigMapList = null;
            this.configSourceInfoList = null;
        }
    }
}