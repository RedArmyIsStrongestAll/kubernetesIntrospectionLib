package kubernetes.introspection.useCases.ports;

import kubernetes.introspection.entities.service.ServiceInfo;

import java.util.List;

public interface KubernetesServicePort {
    List<ServiceInfo> listServices(String namespace);
}
