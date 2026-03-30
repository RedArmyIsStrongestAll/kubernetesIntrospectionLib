package engine;

import entities.services.utils.KubernetesYamlUtils;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ServiceAnalyzer {
    private final List<Service> serviceList;
    private final RbacAnalyzer rbacAnalyzer;

    public ServiceAnalyzer(String serviceYaml, RbacAnalyzer rbacAnalyzer) throws IOException {
        this.rbacAnalyzer = rbacAnalyzer;
        this.serviceList = KubernetesYamlUtils.trySetYamlObjectList(serviceYaml, Service.class, "Service");
    }

    public Service getServiceByName(String requestedName, String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("services", "get", requestedNamespace)) return null;
        return serviceList.stream()
                .filter(service -> service.getMetadata() != null)
                .filter(service -> requestedName.equals(service.getMetadata().getName()))
                .filter(service -> requestedNamespace.equals(service.getMetadata().getNamespace()))
                .findFirst()
                .orElse(null);
    }

    public ServiceList listAllServices(String requestedNamespace) {
        if (!rbacAnalyzer.isAllowed("services", "list", requestedNamespace)) {
            return new ServiceList("v1", Collections.emptyList(), "ServiceList", new ListMeta());
        }
        List<Service> matchedServices = serviceList.stream()
                .filter(service -> service.getMetadata() != null && service.getMetadata().getNamespace().equals(requestedNamespace))
                .toList();
        return new ServiceList("v1", matchedServices, "ServiceList", new ListMeta());
    }
}