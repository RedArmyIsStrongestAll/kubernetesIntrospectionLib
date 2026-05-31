package usesCases.main.owner;

import engine.owners.OwnerAnalyzerJob;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.Fabric8OwnerAdapter;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceJobExt;
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
                "rbac/test-rbac.yaml", "owner/pod-with-job-owner.yaml", OwnerAnalyzerJob.class);
        setupMockServerWithValidJob(ownerEngine, JOB_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.JOBS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.JOB, ownerDto.getOwnerInfo().getType());
        assertEquals(JOB_NAME, ownerDto.getOwnerInfo().getName());
        assertEquals(1, ownerDto.getOwnerInfo().getDesiredReplicas());
        assertEquals(1, ownerDto.getOwnerInfo().getAvailableReplicas());
    }

    @Test
    void getOwnerWithPermissionNoPermissionJob() throws Exception {
        OwnerAnalyzerJob ownerEngine = (OwnerAnalyzerJob) getOwnerAnalyzer(
                "rbac/test-rbac.yaml", "owner/pod-with-job-owner.yaml", OwnerAnalyzerJob.class);
        setupMockServerWithValidJob(ownerEngine, JOB_NAME);

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.JOBS_GET, false)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }

    @Test
    void getOwnerNoPermissionJobByRbac() throws Exception {
        OwnerAnalyzerJob ownerEngine = (OwnerAnalyzerJob) getOwnerAnalyzer(
                "rbac/fail-test-rbac-default.yaml", "owner/pod-with-job-owner.yaml", OwnerAnalyzerJob.class);
        setupMockServerWithValidJob(ownerEngine, JOB_NAME);

        OwnerReferenceInfo ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () -> ownerService.getOwner(ownerRef));
    }

    @Test
    void getOwnerWithPermissionNoKubernetesAnswerJob() throws Exception {
        setupMockServerWithError();

        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.JOBS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(JOB_NAME, OwnerTypeEnum.JOB);
        OwnerService ownerService = new OwnerServiceJobExt(new Fabric8OwnerAdapter(client), NAMESPACE);

        Assertions.assertThrows(KubernetesException.class, () ->
                ownerService.getOwnerWithPermission(ownerRef, permission));
    }
}
