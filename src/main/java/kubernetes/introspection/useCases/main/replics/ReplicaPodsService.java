package kubernetes.introspection.useCases.main.replics;

import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.exceptions.ErrorCodeEnum.REPLICA_PODS_NOT_FOUND;

@Slf4j
public class ReplicaPodsService {
    private final KubernetesPodPort podPort;

    public ReplicaPodsService(KubernetesPodPort podPort) {
        this.podPort = podPort;
    }


    public ReplicaPodsDto getReplicaPodsWithPermission(OwnerReferenceInfo ownerRef, OwnerDto ownerDto,
                                                       PodInfo currentPod, PermissionInfo permissionInfo) {
        log.info("Start getReplicaPodsWithPermission");
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(ResourcePermissionEnum.PODS_LIST,
                    ResourcePermissionEnum.PODS_GET));
            return getReplicaPods(ownerRef, ownerDto, currentPod);
        } catch (KubernetesException e) {
            log.error("Stop getReplicaPodsWithPermission: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }

    public ReplicaPodsDto getReplicaPods(OwnerReferenceInfo ownerRef, OwnerDto ownerDto, PodInfo currentPod) {
        log.info("Start getReplicaPods for owner: {}", ownerDto.getK8sType());
        try {
            List<PodInfo> replicas = new ArrayList<>();

            if (OwnerTypeEnum.STATEFULSET.equals(ownerDto.getK8sType())) {
                replicas.addAll(findStatefulSetPods(ownerDto, currentPod));
            } else {
                replicas.addAll(findPodsByOwnerSelector(ownerDto, currentPod));
                if (replicas.isEmpty()) throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
            }

            return new ReplicaPodsDto(replicas);
        } catch (KubernetesException e) {
            log.error("Stop getReplicaPods: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }


    private List<PodInfo> findPodsByOwnerSelector(OwnerDto ownerDto, PodInfo currentPod) {
        log.info("Start findPodsByOwnerSelector");
        try {
            Map<String, String> matchLabels = ownerDto.getOwnerInfo() != null
                    ? ownerDto.getOwnerInfo().getSelector()
                    : null;

            if (matchLabels == null || matchLabels.isEmpty()) {
                log.warn("No selector found for owner");
                return Collections.emptyList();
            }

            String namespace = currentPod.getNamespace();
            log.info("Start k8s request");
            return podPort.listPodsByLabels(matchLabels, namespace)
                    .stream()
                    .filter(pod -> !isCurrentPod(pod, currentPod))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Stop findPodsByOwnerSelector: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }

    private List<PodInfo> findStatefulSetPods(OwnerDto ownerDto, PodInfo currentPod) {
        log.info("Start findStatefulSetPods");
        try {
            String namespace = currentPod.getNamespace();
            String stsName = ownerDto.getOwnerInfo().getName();
            String podNamePrefix = stsName + "-";

            log.info("Start k8s request");
            return podPort.listAllPods(namespace)
                    .stream()
                    .filter(pod -> pod.getName().startsWith(podNamePrefix))
                    .filter(pod -> !isCurrentPod(pod, currentPod))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Stop findStatefulSetPods: ", e);
            throw new KubernetesException(REPLICA_PODS_NOT_FOUND);
        }
    }

    private boolean isCurrentPod(PodInfo pod, PodInfo currentPod) {
        return currentPod.getName().equals(pod.getName()) &&
                currentPod.getNamespace().equals(pod.getNamespace());
    }


    @Getter
    public static class ReplicaPodsDto {
        private final List<PodInfo> podInfoList;

        public ReplicaPodsDto(List<PodInfo> podInfoList) {
            this.podInfoList = podInfoList;
        }
    }
}
