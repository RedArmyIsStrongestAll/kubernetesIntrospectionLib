package usesCases.main.owner;

import engine.owners.OwnerAnalyzerJob;
import usesCases.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceJobExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class OwnerLabelServiceJobExtTest extends OwnerServiceTestAbstract {
    private static final String JOB_NAME = "test-job";

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
    void getOwnerWithPermissionValidJobOwner() throws Exception {
        OwnerAnalyzerJob ownerEngine = (OwnerAnalyzerJob) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-job-owner.yaml",
                OwnerAnalyzerJob.class
        );
        setupMockServerWithValidJob(ownerEngine, JOB_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.JOBS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.JOB, ownerDto.getOwnerInfo().getType());
        assertEquals(JOB_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(1, ownerDto.getOwnerInfo().getDesiredReplicas());
        assertEquals(1, ownerDto.getOwnerInfo().getAvailableReplicas());
        assertNotNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionJob() throws Exception {
        OwnerAnalyzerJob ownerEngine = (OwnerAnalyzerJob) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-job-owner.yaml",
                OwnerAnalyzerJob.class
        );
        setupMockServerWithValidJob(ownerEngine, JOB_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.JOBS_GET, false)
                ));

        OwnerReference ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }

    @Test
    void getOwnerNoPermissionJobByRbac() throws Exception {
        OwnerAnalyzerJob ownerEngine = (OwnerAnalyzerJob) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml",
                "owner/pod-with-job-owner.yaml",
                OwnerAnalyzerJob.class
        );

        setupMockServerWithValidJob(ownerEngine, JOB_NAME);

        OwnerReference ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwner(ownerRef);
        });
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerJob() throws Exception {
        OwnerAnalyzerJob ownerEngine = (OwnerAnalyzerJob) getOwnerAnalyzer(
                "rbac/test-rbac.yaml",
                "owner/pod-with-job-owner.yaml",
                OwnerAnalyzerJob.class
        );

        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.JOBS_GET, true)
                ));

        OwnerReference ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(client, NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> {
            ownerService.getOwnerWithPermission(ownerRef, permission);
        });
    }
}