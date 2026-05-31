package usesCases.main.owner;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.adapters.kubernetes.Fabric8OwnerAdapter;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.permision.ResourcePermissionEnum;
import kubernetes.introspection.useCases.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceUnknownExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usesCases.main.owner.parent.OwnerServiceTestAbstract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OwnerLabelServiceUnknownImplTest extends OwnerServiceTestAbstract {

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
    void getOwnerWithPermissionValidUnknown() {
        PermissionInfo permission = new PermissionInfo(true,
                List.of(
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.PODS_GET, true),
                        new PermissionInfo.PermissionInfoDto(ResourcePermissionEnum.STATEFULSETS_GET, true)
                ));

        OwnerReferenceInfo ownerRef = getOwnerReference(null, OwnerTypeEnum.UNKNOWN);
        OwnerServiceUnknownExt ownerService = new OwnerServiceUnknownExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.UNKNOWN, ownerDto.getK8sType());
    }

    @Test
    void getOwnerWithPermissionNoPermissionUnknown() {
        PermissionInfo permission = new PermissionInfo(true, List.of());

        OwnerReferenceInfo ownerRef = getOwnerReference(null, OwnerTypeEnum.UNKNOWN);
        OwnerServiceUnknownExt ownerService = new OwnerServiceUnknownExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.UNKNOWN, ownerDto.getK8sType());
    }

    @Test
    void getOwnerNoPermissionUnknown() {
        OwnerReferenceInfo ownerRef = getOwnerReference(null, OwnerTypeEnum.UNKNOWN);
        OwnerServiceUnknownExt ownerService = new OwnerServiceUnknownExt(new Fabric8OwnerAdapter(client), NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerDto(ownerRef);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.UNKNOWN, ownerDto.getK8sType());
    }
}
