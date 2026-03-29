package kubernetes.introspection.entities.services.main.service;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.service.ServiceEndpointAddress;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.permision.PermissionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class EndpointService {
    private final KubernetesClient kubernetesClient;

    public EndpointService(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }


    public static List<ServiceEndpointAddress> mapToEndpointsInfo(Endpoints endpoints) {
        log.info("Start mapToEndpointsInfo");
        try {
            if (endpoints == null) {
                log.warn("Endpoints is null in mapToEndpointsInfo");
                return Collections.emptyList();
            }

            return endpoints.getSubsets().stream()
                    .flatMap(subset -> {
                        List<ServiceEndpointAddress> readyList = Optional.ofNullable(subset.getAddresses())
                                .orElse(Collections.emptyList()).stream()
                                .map(addr -> toServiceEndpointAddress(addr, subset.getPorts(), true))
                                .collect(Collectors.toList());

                        List<ServiceEndpointAddress> notReadyList = Optional.ofNullable(subset.getNotReadyAddresses())
                                .orElse(Collections.emptyList()).stream()
                                .map(addr -> toServiceEndpointAddress(addr, subset.getPorts(), false))
                                .collect(Collectors.toList());

                        List<ServiceEndpointAddress> all = new ArrayList<>();
                        all.addAll(readyList);
                        all.addAll(notReadyList);
                        return all.stream();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error on mapToEndpointsInfo", e);
            throw e;
        }
    }

    private static ServiceEndpointAddress toServiceEndpointAddress(EndpointAddress k8sAddress,
                                                                   List<EndpointPort> ports, boolean ready) {
        Integer port = null;
        if (ports != null && !ports.isEmpty()) {
            port = ports.get(0).getPort(); // Берём первый порт
        }

        return ServiceEndpointAddress.builder()
                .ip(k8sAddress.getIp())
                .podName(k8sAddress.getTargetRef() != null ? k8sAddress.getTargetRef().getName() : null)
                .port(port)
                .ready(ready)
                .targetKind(k8sAddress.getTargetRef() != null ? k8sAddress.getTargetRef().getKind() : null)
                .targetName(k8sAddress.getTargetRef() != null ? k8sAddress.getTargetRef().getName() : null)
                .build();
    }


    public Endpoints getEndpointsForServiceWithPermission(String serviceName, String namespace,
                                                          PermissionInfo permissionInfo) {
        log.info("Start getEndpointsForServiceWithPermission");
        try {
            PermissionService.checkPermission(permissionInfo, () -> List.of(
                    ResourcePermissionEnum.ENDPOINTS_GET,
                    ResourcePermissionEnum.ENDPOINTS_LIST
            ));
            return getEndpointsForService(serviceName, namespace);
        } catch (Exception e) {
            log.error("Stop getEndpointsForServiceWithPermission", e);
            throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
        }
    }

    public Endpoints getEndpointsForService(String serviceName, String namespace) {
        log.info("Start getEndpointsForService");
        try {
            if (serviceName == null || serviceName.isEmpty()) {
                log.warn("Service name is empty");
                throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
            }

            Endpoints endpoints = kubernetesClient.endpoints()
                    .inNamespace(namespace)
                    .withName(serviceName)
                    .get();

            if (endpoints == null) {
                log.warn("Endpoints not found for service: {}", serviceName);
                throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
            }

            return endpoints;
        } catch (Exception e) {
            log.error("Stop getEndpointsForService", e);
            throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
        }
    }


    @Getter
    public static class EndpointDto {
        private final Endpoints k8sEndpoints;
        private final List<ServiceEndpointAddress> endpointsInfo;

        public EndpointDto(Endpoints endpoints) {
            this.k8sEndpoints = endpoints;
            this.endpointsInfo = mapToEndpointsInfo(endpoints);
        }
    }
}