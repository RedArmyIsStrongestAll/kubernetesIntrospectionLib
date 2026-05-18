package kubernetes.introspection.useCases.main.service;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.service.ServiceInfo;
import kubernetes.introspection.entities.service.ServiceServicePort;
import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ServiceService {

    private final KubernetesClient kubernetesClient;

    public ServiceService(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }


    public static ServiceInfo mapToServiceInfo(Service service) {
        log.info("Start mapToServiceInfo");
        try {
            if (service == null) {
                log.warn("Service is null in mapToServiceInfo");
                return null;
            }

            io.fabric8.kubernetes.api.model.ServiceSpec spec = service.getSpec();
            io.fabric8.kubernetes.api.model.ServiceStatus status = service.getStatus();
            io.fabric8.kubernetes.api.model.ObjectMeta metadata = service.getMetadata();

            return ServiceInfo.builder()
                    .name(metadata != null ? metadata.getName() : null)
                    .type(spec != null ? spec.getType() : null)
                    .clusterIP(spec != null ? spec.getClusterIP() : null)
                    .externalIP(
                            spec != null && spec.getExternalIPs() != null && !spec.getExternalIPs().isEmpty()
                                    ? spec.getExternalIPs().get(0)
                                    : null
                    )
                    .ports(spec != null ? mapPorts(spec.getPorts()) : Collections.emptyList())
                    .selector(spec != null && spec.getSelector() != null ? spec.getSelector() : Collections.emptyMap())
                    .endpoints(null) // Требуется отдельный запрос к Endpoints или EndpointSlice //todo написать
                    .readyEndpoints(0)
                    .fullyReady(false)
                    .build();
        } catch (Exception e) {
            log.error("Error on mapToServiceInfo", e);
            throw e;
        }
    }

    private static List<ServiceServicePort> mapPorts(List<ServicePort> portList) {
        log.info("Start mapPorts");
        try {
            if (portList == null || portList.isEmpty()) {
                return Collections.emptyList();
            }

            return portList.stream()
                    .map(port -> ServiceServicePort.builder()
                            .name(port.getName())
                            .protocol(port.getProtocol())
                            .port(port.getPort())
                            .nodePort(port.getNodePort())
                            .targetPort(port.getTargetPort() != null ? port.getTargetPort().getIntVal() : null)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error on mapPorts", e);
            throw e;
        }
    }


    public ServiceDto findServicesForPodWithPermission(PodInfo podInfo, String namespace, PermissionInfo permissionInfo) {
        log.info("Stop findServicesForPodWithPermission");
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
            log.info("Pod has labels: {}", podLabels);

            log.info("Start k8s request");
            ServiceList serviceList = kubernetesClient.services()
                    .inNamespace(namespace)
                    .list();

            List<Service> serviceListForLabel = serviceList.getItems().stream()
                    .filter(service -> matchesSelector(podLabels, service.getSpec().getSelector()))
                    .toList();
            log.info("Find services: {}", serviceListForLabel);

            if (serviceListForLabel.isEmpty()) {
                log.warn(ErrorCodeEnum.SERVICE_MANY_FOUND.getMessage());
                throw new KubernetesException(ErrorCodeEnum.SERVICE_NOT_FOUND);
            }
            if (serviceListForLabel.size() != 1) {
                log.warn(ErrorCodeEnum.SERVICE_MANY_FOUND.getMessage());
                throw new KubernetesException(ErrorCodeEnum.SERVICE_MANY_FOUND);
            }
            Service service = serviceListForLabel.get(0);
            log.info("Find services: {}", service);

            return new ServiceDto(service);

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
        private final Service k8sService;
        private final ServiceInfo serviceInfo;

        public ServiceDto(Service k8sService) {
            this.k8sService = k8sService;
            this.serviceInfo = mapToServiceInfo(k8sService);
        }

        public ServiceDto(Service k8sService, ServiceInfo serviceInfo) {
            this.k8sService = k8sService;
            this.serviceInfo = serviceInfo;
        }
    }
}
