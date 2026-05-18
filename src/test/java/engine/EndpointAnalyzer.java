package engine;

import usesCases.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.ListMeta;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EndpointAnalyzer {
    private final List<Endpoints> endpointsList;
    private final RbacAnalyzer rbacAnalyzer;

    public EndpointAnalyzer(String endpointsYaml, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.rbacAnalyzer = rbacAnalyzer;
        this.endpointsList = KubernetesYamlUtils.trySetYamlObjectList(endpointsYaml, Endpoints.class, "Endpoints");
    }

    public Endpoints getEndpointsByName(String name, String namespace) {
        if (!rbacAnalyzer.isAllowed("endpoints", "get", namespace)) {
            return null;
        }
        Endpoints endpoints = endpointsList.stream()
                .filter(ep -> ep.getMetadata() != null)
                .filter(ep -> name.equals(ep.getMetadata().getName()))
                .filter(ep -> namespace.equals(ep.getMetadata().getNamespace()))
                .findFirst()
                .orElse(null);
        return endpoints;
    }

    public EndpointsList listEndpointsByServiceName(String serviceName, String namespace) {
        if (!rbacAnalyzer.isAllowed("endpoints", "list", namespace)) {
            return new EndpointsList("v1", Collections.emptyList(), "EndpointsList", new ListMeta());
        }

        List<Endpoints> matchedEndpoints = endpointsList.stream()
                .filter(ep -> ep.getMetadata() != null)
                .filter(ep -> serviceName.equals(ep.getMetadata().getName()))
                .filter(ep -> namespace.equals(ep.getMetadata().getNamespace()))
                .toList();

        return new EndpointsList("v1", matchedEndpoints, "EndpointsList", new ListMeta());
    }

    public EndpointsList listAllEndpoints(String namespace) {
        if (!rbacAnalyzer.isAllowed("endpoints", "list", namespace)) {
            return new EndpointsList("v1", Collections.emptyList(), "EndpointsList", new ListMeta());
        }
        List<Endpoints> matchedEndpoints = endpointsList.stream()
                .filter(ep -> ep.getMetadata() != null && ep.getMetadata().getNamespace().equals(namespace))
                .toList();
        return new EndpointsList("v1", matchedEndpoints, "EndpointsList", new ListMeta());
    }
}