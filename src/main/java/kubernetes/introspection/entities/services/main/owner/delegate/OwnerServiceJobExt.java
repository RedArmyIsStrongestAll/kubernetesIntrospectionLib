package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public class OwnerServiceJobExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceJobExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.JOB;

    public OwnerServiceJobExt(KubernetesClient kubernetesClient, String namespace) {
        super(kubernetesClient, namespace);
    }

    @Override
    protected String getNameClassExt() {
        return SERVICE_NAME;
    }

    @Override
    protected OwnerTypeEnum getKindOwnerType() {
        return OWNER_TYPE;
    }

    @Override
    protected List<ResourcePermissionEnum> getPermissionResource() {
        return List.of(ResourcePermissionEnum.JOBS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching Job owner: {}", SERVICE_NAME, ownerRef.getName());

        Job job = kubernetesClient
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (job == null) {
            log.error("{}: Job not found: {}", SERVICE_NAME, ownerRef.getName());
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }

        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(OwnerTypeEnum.JOB)
                .name(job.getMetadata().getName())
                .exists(true)
                .selector(job.getSpec().getSelector().getMatchLabels())
                .desiredReplicas(1)
                .availableReplicas(job.getStatus().getSucceeded())
                .jobStatus(job.getStatus())
                .build();

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.JOB, job);
    }
}