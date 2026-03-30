package kubernetes.introspection.useCases;

import kubernetes.introspection.entities.models.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.entities.services.env.GetVarsServicesDtoService;

public interface kubernetesIntrospectionEnvironmentService {

    KubernetesEnvironmentInfo getKubernetesEnvironmentInfo(GetVarsServicesDtoService vars);

}
