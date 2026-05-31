package kubernetes.introspection.useCases.main.service;

import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.service.ServiceInfo;
import kubernetes.introspection.useCases.ports.KubernetesServicePort;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ServiceService {

    private final KubernetesServicePort servicePort;

    public ServiceService(KubernetesServicePort servicePort) {
        this.servicePort = servicePort;
    }


    public ServiceDto findServicesForPodWithPermission(PodInfo podInfo, String namespace, PermissionInfo permissionInfo) {
        log.info("Start findServicesForPodWithPermission");
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(ResourcePermissionEnum.SERVICES_LIST,
                    ResourcePermissionEnum.SERVICES_GET));
            return findServicesForPod(podInfo, namespace);
        } catch (Exception e) {
            log.info("Stop findServicesForPodWithPermission: ", e);
            throw new KubernetesException(ErrorCodeEnum.SERVICE_NOT_FOUND);
        }
    }

    public ServiceDto findServicesForPod(PodInfo podInfo, String namespace) {
        log.info("Start findServicesForPod");
        try {
            Map<String, String> podLabels = podInfo.getLabels();
            if (podLabels == null || podLabels.isEmpty()) {
                log.warn("Pod has no labels");
                throw new KubernetesException(ErrorCodeEnum.SERVICE_NOT_FOUND);
            }

            log.info("Start k8s request");
            List<ServiceInfo> matched = servicePort.listServices(namespace).stream()
                    .filter(service -> matchesSelector(podLabels, service.getSelector()))
                    .toList();

            if (matched.isEmpty()) throw new KubernetesException(ErrorCodeEnum.SERVICE_NOT_FOUND);
            if (matched.size() != 1) throw new KubernetesException(ErrorCodeEnum.SERVICE_MANY_FOUND);

            return new ServiceDto(matched.get(0));
        } catch (Exception e) {
            log.info("Stop findServicesForPod: ", e);
            throw new KubernetesException(ErrorCodeEnum.SERVICE_NOT_FOUND);
        }
    }


    private boolean matchesSelector(Map<String, String> podLabels, Map<String, String> selector) {
        if (selector == null || selector.isEmpty()) return false;
        return selector.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(podLabels.get(entry.getKey())));
    }


    @Getter
    public static class ServiceDto {
        private final ServiceInfo serviceInfo;

        public ServiceDto(ServiceInfo serviceInfo) {
            this.serviceInfo = serviceInfo;
        }
    }
}
