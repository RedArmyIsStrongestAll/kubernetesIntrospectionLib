package kubernetes.introspection.controllers;

import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.useCases.env.GetVarsServicesDtoService;

public interface KubernetesIntrospectionEnvironmentService {

    KubernetesEnvironmentInfo getKubernetesEnvironmentInfo(GetVarsServicesDtoService vars);

    KubernetesEnvironmentInfo getKubernetesEnvironmentInfoWithClient(GetVarsServicesDtoService vars, KubernetesClient client);

}
