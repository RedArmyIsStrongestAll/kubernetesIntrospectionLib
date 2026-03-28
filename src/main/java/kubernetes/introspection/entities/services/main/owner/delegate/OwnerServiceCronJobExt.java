package kubernetes.introspection.entities.services.main.owner.delegate;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
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

        OwnerInfo ownerInfo = OwnerInfo.builder()
                .type(OwnerTypeEnum.CRON_JOB)
                .name(cronJob.getMetadata().getName())
                .exists(true)
                .selector(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getMetadata().getLabels())
                .desiredReplicas(1)
                .availableReplicas(null)
                .jobStatus(null)
                .lastSuccessfulTime(cronJob.getStatus().getLastSuccessfulTime())
                .build();

        log.info("{}: created info: {}", SERVICE_NAME, ownerInfo);

        return new OwnerDto(ownerInfo, OwnerTypeEnum.CRON_JOB, cronJob);
    }
}