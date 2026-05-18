package kubernetes.introspection.useCases.main.source;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.entities.source.ConfigSourceTypeEnum;
import kubernetes.introspection.entities.source.ConfigUsageTypeEnum;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
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
public class SecretSourceService {

    private final KubernetesClient kubernetesClient;
    private final String namespace;

    public SecretSourceService(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
    }

    public static List<ConfigSourceInfo> mapToConfigSourceInfoList(
            List<Secret> secrets,
            Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {

        return secrets.stream()
                .map(secret -> {
                    String name = secret.getMetadata().getName();
                    Set<ConfigUsageTypeEnum> usageTypes = usageTypeMap.getOrDefault(
                            name, Set.of(ConfigUsageTypeEnum.UNKNOWN)
                    );
                    ConfigUsageTypeEnum usageType = usageTypes.size() == 1
                            ? usageTypes.iterator().next()
                            : ConfigUsageTypeEnum.MIXED;

                    return mapToConfigSourceInfo(secret, usageType);
                })
                .collect(Collectors.toList());
    }

    public static ConfigSourceInfo mapToConfigSourceInfo(Secret secret, ConfigUsageTypeEnum usageType) {
        String name = secret.getMetadata().getName();
        List<String> keys = new ArrayList<>();
        if (secret.getData() != null) {
            keys.addAll(secret.getData().keySet());
        }

        return ConfigSourceInfo.builder()
                .name(name)
                .type(ConfigSourceTypeEnum.SECRET)
                .keys(keys)
                .usageType(usageType)
                .build();
    }

    public SecretDto getSecretSourcesWithPermission(Pod currentPod, PermissionInfo permissionInfo) {
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(
                    ResourcePermissionEnum.SECRETS_GET,
                    ResourcePermissionEnum.SECRETS_LIST
            ));
            return getSecretSources(currentPod);
        } catch (Exception e) {
            log.error("Error getting Secret sources", e);
            throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
        }
    }

    public SecretDto getSecretSources(Pod currentPod) {
        try {
            Set<String> secretNames = new HashSet<>();
            Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap = new HashMap<>();

            extractFromEnv(currentPod, secretNames, usageTypeMap);
            extractFromEnvFrom(currentPod, secretNames, usageTypeMap);
            extractFromVolumes(currentPod, secretNames, usageTypeMap);

            if (secretNames.isEmpty()) {
                throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
            }

            List<Secret> secrets = secretNames.stream()
                    .map(name -> kubernetesClient.secrets().inNamespace(namespace).withName(name).get())
                    .filter(Objects::nonNull)
                    .toList();

            return new SecretDto(secrets, usageTypeMap);

        } catch (Exception e) {
            log.error("Error getting Secret sources", e);
            throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
        }
    }

    private void extractFromEnv(Pod pod, Set<String> secretNames, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        pod.getSpec().getContainers().forEach(container -> {
            if (container.getEnv() != null) {
                container.getEnv().forEach(envVar -> {
                    if (envVar.getValueFrom() != null && envVar.getValueFrom().getSecretKeyRef() != null) {
                        String name = envVar.getValueFrom().getSecretKeyRef().getName();
                        secretNames.add(name);
                        usageTypeMap.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV);
                    }
                });
            }
        });
    }

    private void extractFromEnvFrom(Pod pod, Set<String> secretNames, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        pod.getSpec().getContainers().forEach(container -> {
            if (container.getEnvFrom() != null) {
                container.getEnvFrom().forEach(envFrom -> {
                    if (envFrom.getSecretRef() != null && envFrom.getSecretRef().getName() != null) {
                        String name = envFrom.getSecretRef().getName();
                        secretNames.add(name);
                        usageTypeMap.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.ENV_FROM);
                    }
                });
            }
        });
    }

    private void extractFromVolumes(Pod pod, Set<String> secretNames, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
        pod.getSpec().getVolumes().forEach(volume -> {
            if (volume.getSecret() != null) {
                String name = volume.getSecret().getSecretName();
                secretNames.add(name);
                usageTypeMap.computeIfAbsent(name, k -> new HashSet<>()).add(ConfigUsageTypeEnum.VOLUME);
            }
        });
    }

    @Getter
    public static class SecretDto {
        private final List<Secret> k8sSecretList;
        private final List<ConfigSourceInfo> configSourceInfoList;

        public SecretDto(List<Secret> k8sSecretList, Map<String, Set<ConfigUsageTypeEnum>> usageTypeMap) {
            this.k8sSecretList = k8sSecretList;
            this.configSourceInfoList = mapToConfigSourceInfoList(k8sSecretList, usageTypeMap);
        }

        public SecretDto() {
            this.k8sSecretList = null;
            this.configSourceInfoList = null;
        }
    }
}