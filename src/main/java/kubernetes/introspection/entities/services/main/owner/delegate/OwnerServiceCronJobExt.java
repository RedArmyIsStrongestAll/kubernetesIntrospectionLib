package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobStatus;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.owner.OwnerInfo;
import kubernetes.introspection.entities.models.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.OWNER_REALIZED_NOT_FOUND;

@Slf4j
public class OwnerServiceCronJobExt extends OwnerService {

    private static final String SERVICE_NAME = "OwnerServiceCronJobExt";
    private static final OwnerTypeEnum OWNER_TYPE = OwnerTypeEnum.CRON_JOB;

    public OwnerServiceCronJobExt(KubernetesClient kubernetesClient, String namespace) {
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
        return List.of(ResourcePermissionEnum.CRONJOBS_GET);
    }

    @Override
    public OwnerDto getOwnerDto(OwnerReference ownerRef) {
        log.info("{}: fetching CronJob owner: {}", SERVICE_NAME, ownerRef.getName());

        CronJob cronJob = kubernetesClient
                .batch()
                .v1()
                .cronjobs()
                .inNamespace(namespace)
                .withName(ownerRef.getName())
                .get();

        if (cronJob == null) {
            log.error("{}: CronJob not found: {}", SERVICE_NAME, ownerRef.getName());
            throw new KubernetesException(OWNER_REALIZED_NOT_FOUND);
        }

        OwnerInfo ownerInfo = createOwnerInfo(cronJob);

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.CRON_JOB, cronJob);
    }

    private OwnerInfo createOwnerInfo(CronJob cronJob) {
        return OwnerInfo.builder()
                .type(OwnerTypeEnum.CRON_JOB)
                .name(
                        Optional.ofNullable(cronJob.getMetadata())
                                .map(ObjectMeta::getName)
                                .orElse(null)
                )
                .exists(true)
                .selector(
                        Optional.ofNullable(cronJob.getSpec())
                                .map(CronJobSpec::getJobTemplate)
                                .map(JobTemplateSpec::getSpec)
                                .map(jobSpec -> jobSpec.getTemplate())
                                .map(template -> template.getMetadata())
                                .map(ObjectMeta::getLabels)
                                .orElse(Collections.emptyMap())
                )
                .desiredReplicas(1) // CronJob всегда запускает 1 Job
                .availableReplicas(null) // Job не имеет availableReplicas
                .jobStatus(null) // JobStatus не используется
                .lastSuccessfulTime(
                        Optional.ofNullable(cronJob.getStatus())
                                .map(CronJobStatus::getLastSuccessfulTime)
                                .orElse(null)
                )
                .build();
    }
}