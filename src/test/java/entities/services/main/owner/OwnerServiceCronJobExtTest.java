package entities.services.main.owner;

import engine.owner.OwnerAnalyzerCronJob;
import entities.services.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceCronJobExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerServiceCronJobExtTest extends OwnerServiceTestAbstract {
    private static final String CRONJOB_NAME = "test-cronjob";

    @BeforeEach
    void setUp() {
        mockServer = new KubernetesMockServer();
        mockServer.init();
        client = mockServer.createClient();
    }

    @AfterEach
    void tearDown() {
        mockServer.destroy();
    }

    @Test
    void getOwnerWithPermissionValidCronJobOwner() throws Exception {
        OwnerAnalyzerCronJob ownerEngine = (OwnerAnalyzerCronJob) getOwnerAnalyzer("rbac/test-rbac.yaml",
                "owner/pod-with-cronjob-owner.yaml", OwnerAnalyzerCronJob.class);
        setupMockServerWithValidCronJob(ownerEngine, CRONJOB_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CRONJOBS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.CRON_JOB, ownerDto.getOwnerInfo().getType());
        assertEquals(CRONJOB_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(1, ownerDto.getOwnerInfo().getDesiredReplicas());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionCronJob() throws Exception {
        OwnerAnalyzerCronJob ownerEngine = (OwnerAnalyzerCronJob) getOwnerAnalyzer("rbac/test-rbac.yaml",
                "owner/pod-with-cronjob-owner.yaml", OwnerAnalyzerCronJob.class);
        setupMockServerWithValidCronJob(ownerEngine, CRONJOB_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CRONJOBS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoPermissionCronJobByRbac() throws Exception {
        OwnerAnalyzerCronJob ownerEngine = (OwnerAnalyzerCronJob) getOwnerAnalyzer("rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-cronjob-owner.yaml", OwnerAnalyzerCronJob.class);

        setupMockServerWithValidCronJob(ownerEngine, CRONJOB_NAME);

        OwnerReference ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerCronJob() throws Exception {
        OwnerAnalyzerCronJob ownerEngine = (OwnerAnalyzerCronJob) getOwnerAnalyzer("rbac/test-rbac.yaml",
                "owner/pod-with-cronjob-owner.yaml", OwnerAnalyzerCronJob.class);

        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CRONJOBS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}