package entities.services.main.owner;

import entities.services.main.owner.parent.OwnerServiceTestAbstract;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.services.main.owner.OwnerService.OwnerDto;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceUnknownExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OwnerServiceUnknownExtTest extends OwnerServiceTestAbstract {

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

        OwnerReference ownerRef = getOwnerReference(null, OwnerTypeEnum.UNKNOWN);
        OwnerServiceUnknownExt ownerService = new OwnerServiceUnknownExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.UNKNOWN, ownerDto.getK8sType());
        assertNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerWithPermissionNoPermissionUnknown() throws Exception {
        PermissionInfo permission = new PermissionInfo(true,
                List.of());

        OwnerReference ownerRef = getOwnerReference(null, OwnerTypeEnum.UNKNOWN);
        OwnerServiceUnknownExt ownerService = new OwnerServiceUnknownExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerWithPermission(ownerRef, permission);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.UNKNOWN, ownerDto.getK8sType());
        assertNull(ownerDto.getK8sObject());
    }

    @Test
    void getOwnerNoPermissionUnknown() throws Exception {
        OwnerReference ownerRef = getOwnerReference(null, OwnerTypeEnum.UNKNOWN);
        OwnerServiceUnknownExt ownerService = new OwnerServiceUnknownExt(client, NAMESPACE);
        OwnerDto ownerDto = ownerService.getOwnerDto(ownerRef);

        assertNotNull(ownerDto);
        assertNotNull(ownerDto.getOwnerInfo());
        assertEquals(OwnerTypeEnum.UNKNOWN, ownerDto.getK8sType());
        assertNull(ownerDto.getK8sObject());
    }
}