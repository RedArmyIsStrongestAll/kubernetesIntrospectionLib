package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        OwnerInfo ownerInfo = createOwnerInfo(job);

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.JOB, job);
    }

    private OwnerInfo createOwnerInfo(Job job) {
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.JOB)
                .name(
                        Optional.ofNullable(job.getMetadata())
                                .map(ObjectMeta::getName)
                                .orElse(null)
                )
                .exists(true)
                .selector(
                        Optional.ofNullable(job.getSpec())
                                .map(JobSpec::getSelector)
                                .map(LabelSelector::getMatchLabels)
                                .orElse(Collections.emptyMap())
                )
                .desiredReplicas(1) // Job запускается один раз
                .availableReplicas(
                        Optional.ofNullable(job.getStatus())
                                .map(JobStatus::getSucceeded)
                                .orElse(null)
                )
                .jobStatus(job.getStatus()) // можно оставить как есть, если нужен полный объект
                .lastSuccessfulTime(null)
                .build();
    }
}