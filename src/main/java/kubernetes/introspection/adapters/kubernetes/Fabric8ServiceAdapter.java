package kubernetes.introspection.adapters.kubernetes;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.service.ServiceInfo;
import kubernetes.introspection.entities.service.ServiceServicePort;
import kubernetes.introspection.useCases.ports.KubernetesServicePort;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Fabric8ServiceAdapter implements KubernetesServicePort {

    private final KubernetesClient client;

    public Fabric8ServiceAdapter(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public List<ServiceInfo> listServices(String namespace) {
        return client.services().inNamespace(namespace).list().getItems()
                .stream().map(this::mapToServiceInfo).collect(Collectors.toList());
    }

    private ServiceInfo mapToServiceInfo(io.fabric8.kubernetes.api.model.Service service) {
        if (service == null) return null;
        var spec = service.getSpec();
        var metadata = service.getMetadata();
        return ServiceInfo.builder()
                .name(metadata != null ? metadata.getName() : null)
                .type(spec != null ? spec.getType() : null)
                .clusterIP(spec != null ? spec.getClusterIP() : null)
                .externalIP(spec != null && spec.getExternalIPs() != null && !spec.getExternalIPs().isEmpty()
                        ? spec.getExternalIPs().get(0) : null)
                .ports(spec != null ? mapPorts(spec.getPorts()) : Collections.emptyList())
                .selector(spec != null && spec.getSelector() != null ? spec.getSelector() : Collections.emptyMap())
                .endpoints(null)
                .readyEndpoints(0)
                .fullyReady(false)
                .build();
    }

    private List<ServiceServicePort> mapPorts(List<ServicePort> portList) {
        if (portList == null || portList.isEmpty()) return Collections.emptyList();
        return portList.stream()
                .map(port -> ServiceServicePort.builder()
                        .name(port.getName())
                        .protocol(port.getProtocol())
                        .port(port.getPort())
                        .nodePort(port.getNodePort())
                        .targetPort(port.getTargetPort() != null ? port.getTargetPort().getIntVal() : null)
                        .build())
                .collect(Collectors.toList());
    }
}
