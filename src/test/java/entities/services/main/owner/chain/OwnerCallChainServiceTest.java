package entities.services.main.owner.chain;

import entities.services.main.pod.parent.CurrentPodServiceTestAbstract;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import kubernetes.introspection.entities.models.dto.owner.OwnerInfo;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.owner.OwnerCallChainService;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OwnerCallChainServiceTest extends CurrentPodServiceTestAbstract {

    private OwnerCallChainService callChainService;
    private List<OwnerService> mockServices;

    @BeforeEach
    void setUp() {
        mockServices = List.of(
                mock(OwnerService.class),
                mock(OwnerService.class),
                mock(OwnerService.class)
        );
        callChainService = new OwnerCallChainService(mockServices);
    }

    @Test
    void getOwnerWithPermissionLastServiceValidTest() {
        PermissionInfo permissionInfo = new PermissionInfo();
        OwnerReference ownerRef = new OwnerReference();

        OwnerInfo mockOwnerInfo = mock(OwnerInfo.class);
        OwnerTypeEnum mockType = mock(OwnerTypeEnum.class);
        HasMetadata mockObject = mock(HasMetadata.class);

        OwnerService.OwnerDto mockDto = new OwnerService.OwnerDto(mockOwnerInfo, mockType, mockObject);

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getOwnerWithPermission(ownerRef, permissionInfo)).thenReturn(mockDto);

        OwnerService.OwnerDto result = callChainService.getOwnerWithPermission(ownerRef, permissionInfo);

        Assertions.assertNotNull(result);
        Assertions.assertSame(mockDto, result);

        verify(first, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
        verify(second, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
        verify(third, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
    }

    @Test
    void getOwnerWithPermissionNoValidTest() {
        PermissionInfo permissionInfo = new PermissionInfo();
        OwnerReference ownerRef = new OwnerReference();

        OwnerInfo mockOwnerInfo = mock(OwnerInfo.class);
        OwnerTypeEnum mockType = mock(OwnerTypeEnum.class);
        HasMetadata mockObject = mock(HasMetadata.class);

        OwnerService.OwnerDto mockDto = new OwnerService.OwnerDto(mockOwnerInfo, mockType, mockObject);

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));

        Assertions.assertThrows(KubernetesException.class, () -> callChainService.getOwnerWithPermission(ownerRef, permissionInfo));

        verify(first, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
        verify(second, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
        verify(third, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
    }

    @Test
    void getOwnerLastServiceValidTest() {
        OwnerReference ownerRef = new OwnerReference();

        OwnerInfo mockOwnerInfo = mock(OwnerInfo.class);
        OwnerTypeEnum mockType = mock(OwnerTypeEnum.class);
        HasMetadata mockObject = mock(HasMetadata.class);

        OwnerService.OwnerDto mockDto = new OwnerService.OwnerDto(mockOwnerInfo, mockType, mockObject);

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getOwner(ownerRef)).thenReturn(mockDto);

        OwnerService.OwnerDto result = callChainService.getOwner(ownerRef);

        Assertions.assertNotNull(result);
        Assertions.assertSame(mockDto, result);

        verify(first, times(1)).getOwner(ownerRef);
        verify(second, times(1)).getOwner(ownerRef);
        verify(third, times(1)).getOwner(ownerRef);
    }

    @Test
    void getOwnerNoValidTest() {
        OwnerReference ownerRef = new OwnerReference();

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));

        Assertions.assertThrows(KubernetesException.class, () -> callChainService.getOwner(ownerRef));

        verify(first, times(1)).getOwner(ownerRef);
        verify(second, times(1)).getOwner(ownerRef);
        verify(third, times(1)).getOwner(ownerRef);
    }


}
