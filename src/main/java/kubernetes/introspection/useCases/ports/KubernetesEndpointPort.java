package kubernetes.introspection.useCases.ports;

import kubernetes.introspection.entities.service.ServiceEndpointAddress;

import java.util.List;

public interface KubernetesEndpointPort {
    List<ServiceEndpointAddress> listEndpointsByServiceName(String serviceName, String namespace);
}
