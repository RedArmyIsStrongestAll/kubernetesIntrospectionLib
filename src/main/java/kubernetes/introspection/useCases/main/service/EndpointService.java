package kubernetes.introspection.useCases.main.service;

import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.service.ServiceEndpointAddress;
import kubernetes.introspection.useCases.ports.KubernetesEndpointPort;
import kubernetes.introspection.useCases.utils.PermissionServiceUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class EndpointService {
    private final KubernetesEndpointPort endpointPort;

    public EndpointService(KubernetesEndpointPort endpointPort) {
        this.endpointPort = endpointPort;
    }


    public EndpointDto getEndpointsForServiceWithPermission(String serviceName, String namespace,
                                                            PermissionInfo permissionInfo) {
        log.info("Start getEndpointsForServiceWithPermission");
        try {
            PermissionServiceUtil.checkPermission(permissionInfo, () -> List.of(
                    ResourcePermissionEnum.ENDPOINTS_GET,
                    ResourcePermissionEnum.ENDPOINTS_LIST
            ));
            return getEndpointsForService(serviceName, namespace);
        } catch (Exception e) {
            log.error("Stop getEndpointsForServiceWithPermission", e);
            throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
        }
    }

    public EndpointDto getEndpointsForService(String serviceName, String namespace) {
        log.info("Start getEndpointsForService");
        try {
            if (serviceName == null || serviceName.isEmpty()) {
                log.warn("Service name is empty");
                throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
            }

            List<ServiceEndpointAddress> addresses = endpointPort.listEndpointsByServiceName(serviceName, namespace);

            if (addresses == null) {
                log.warn("Endpoints not found for service: {}", serviceName);
                throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
            }

            return new EndpointDto(addresses);
        } catch (Exception e) {
            log.error("Stop getEndpointsForService", e);
            throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
        }
    }


    @Getter
    public static class EndpointDto {
        private final List<ServiceEndpointAddress> endpointsInfo;

        public EndpointDto(List<ServiceEndpointAddress> endpointsInfo) {
            this.endpointsInfo = endpointsInfo;
        }
    }
}
