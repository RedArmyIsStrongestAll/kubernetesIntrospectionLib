package kubernetes.introspection.entities.services.init;

import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.env.KubernetesFileReadService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class InitDetectorService {
    private final KubernetesFileReadService fileReadService;

    public InitDetectorService(KubernetesFileReadService fileReadService) {
        this.fileReadService = fileReadService;
    }

    public boolean runningInKubernetes() {
        log.info("Start runningInKubernetes");
        String kubeHost = fileReadService.getKubernetesHostEnv();
        boolean hasKubeEnv = kubeHost != null && !kubeHost.isBlank();
        boolean hasToken = fileReadService.tokenExists();
        boolean hasNs = fileReadService.namespaceExists();

        log.info("Result runningInKubernetes: hasKubeEnv {}, hasToken {}, hasNs {}",
                hasKubeEnv, hasToken, hasNs);
        return hasKubeEnv && hasToken && hasNs;
    }

    public String getNamespace() {
        log.info("Start getNamespace");

        if (!runningInKubernetes()) {
            log.error(ErrorCodeEnum.NOT_IN_CLUSTER.getMessage());
            throw new KubernetesException(ErrorCodeEnum.NOT_IN_CLUSTER);
        }

        try {
            String ns = fileReadService.getNamespace().trim();
            if (ns.isBlank()) {
                throw new IOException("Namespace content is blank");
            }
            return ns;

        } catch (Exception e) {
            log.error(ErrorCodeEnum.NOT_NAMESPACE.getMessage(), e);
            throw new KubernetesException(ErrorCodeEnum.NOT_NAMESPACE);
        }
    }
}