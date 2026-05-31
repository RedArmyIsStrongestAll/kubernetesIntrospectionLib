package usesCases.main.owner;

import engine.owners.OwnerAnalyzerCronJob;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.Fabric8OwnerAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceCronJobExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.owner.parent.OwnerServiceTestAbstract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceCronJobExtTest extends OwnerServiceTestAbstract {
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

        OwnerReferenceInfo ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.CRON_JOB, ownerDto.getOwnerInfo().getType());
        assertEquals(CRONJOB_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(1, ownerDto.getOwnerInfo().getDesiredReplicas());
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

        OwnerReferenceInfo ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }

    @Test
    void getOwnerNoPermissionCronJobByRbac() throws Exception {
        OwnerAnalyzerCronJob ownerEngine = (OwnerAnalyzerCronJob) getOwnerAnalyzer("rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-cronjob-owner.yaml", OwnerAnalyzerCronJob.class);
        setupMockServerWithValidCronJob(ownerEngine, CRONJOB_NAME);

        OwnerReferenceInfo ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> ownerService.getOwner(ownerRef));
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerCronJob() throws Exception {
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.CRONJOBS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(CRONJOB_NAME, OwnerTypeEnum.CRON_JOB);
        OwnerService ownerService = new OwnerServiceCronJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }
}
