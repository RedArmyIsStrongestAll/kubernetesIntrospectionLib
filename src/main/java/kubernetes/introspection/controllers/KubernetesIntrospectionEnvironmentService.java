package kubernetes.introspection.controllers;

import kubernetes.introspection.entities.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.useCases.env.GetVarsServicesDtoService;

public interface KubernetesIntrospectionEnvironmentService {

    KubernetesEnvironmentInfo getKubernetesEnvironmentInfo(GetVarsServicesDtoService vars);

}
