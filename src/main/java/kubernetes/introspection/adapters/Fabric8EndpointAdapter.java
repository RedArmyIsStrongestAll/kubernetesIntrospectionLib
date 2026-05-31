package kubernetes.introspection.adapters;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.service.ServiceEndpointAddress;
import kubernetes.introspection.useCases.ports.KubernetesEndpointPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Fabric8EndpointAdapter implements KubernetesEndpointPort {

    private final KubernetesClient client;

    public Fabric8EndpointAdapter(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public List<ServiceEndpointAddress> listEndpointsByServiceName(String serviceName, String namespace) {
        EndpointsList endpointsList = client.endpoints().inNamespace(namespace)
                .withField("metadata.name", serviceName).list();

        if (endpointsList == null || endpointsList.getItems() == null || endpointsList.getItems().isEmpty()) {
            return null;
        }

        if (endpointsList.getItems().size() > 1) {
            throw new kubernetes.introspection.entities.exceptions.KubernetesException(
                    kubernetes.introspection.entities.exceptions.ErrorCodeEnum.ENDPOINTS_MANY_FOUND);
        }

        return mapToServiceEndpointAddresses(endpointsList.getItems().get(0));
    }

    private List<ServiceEndpointAddress> mapToServiceEndpointAddresses(Endpoints endpoints) {
        if (endpoints == null || endpoints.getSubsets() == null) return Collections.emptyList();

        return endpoints.getSubsets().stream()
                .flatMap(subset -> {
                    List<ServiceEndpointAddress> ready = Optional.ofNullable(subset.getAddresses())
                            .orElse(Collections.emptyList()).stream()
                            .map(addr -> toEndpointAddress(addr, subset.getPorts(), true))
                            .collect(Collectors.toList());

                    List<ServiceEndpointAddress> notReady = Optional.ofNullable(subset.getNotReadyAddresses())
                            .orElse(Collections.emptyList()).stream()
                            .map(addr -> toEndpointAddress(addr, subset.getPorts(), false))
                            .collect(Collectors.toList());

                    List<ServiceEndpointAddress> all = new ArrayList<>();
                    all.addAll(ready);
                    all.addAll(notReady);
                    return all.stream();
                })
                .collect(Collectors.toList());
    }

    private ServiceEndpointAddress toEndpointAddress(EndpointAddress addr, List<EndpointPort> ports, boolean ready) {
        Integer port = (ports != null && !ports.isEmpty()) ? ports.get(0).getPort() : null;
        return ServiceEndpointAddress.builder()
                .ip(addr.getIp())
                .podName(addr.getTargetRef() != null ? addr.getTargetRef().getName() : null)
                .port(port)
                .ready(ready)
                .targetKind(addr.getTargetRef() != null ? addr.getTargetRef().getKind() : null)
                .targetName(addr.getTargetRef() != null ? addr.getTargetRef().getName() : null)
                .build();
    }
}
