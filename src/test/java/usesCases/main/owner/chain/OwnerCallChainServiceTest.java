package usesCases.main.owner.chain;

import kubernetes.introspection.entities.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerInfo;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.useCases.main.owner.OwnerCallChainService;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class OwnerCallChainServiceTest {

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
        OwnerReferenceInfo ownerRef = OwnerReferenceInfo.builder().kind("Deployment").name("test").build();

        OwnerInfo mockOwnerInfo = mock(OwnerInfo.class);
        OwnerService.OwnerDto mockDto = new OwnerService.OwnerDto(mockOwnerInfo, OwnerTypeEnum.DEPLOYMENT);

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
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
        OwnerReferenceInfo ownerRef = OwnerReferenceInfo.builder().kind("Deployment").name("test").build();

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(third.getOwnerWithPermission(ownerRef, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));

        Assertions.assertThrows(KubernetesException.class, () -> callChainService.getOwnerWithPermission(ownerRef, permissionInfo));

        verify(first, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
        verify(second, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
        verify(third, times(1)).getOwnerWithPermission(ownerRef, permissionInfo);
    }

    @Test
    void getOwnerLastServiceValidTest() {
        OwnerReferenceInfo ownerRef = OwnerReferenceInfo.builder().kind("Deployment").name("test").build();

        OwnerInfo mockOwnerInfo = mock(OwnerInfo.class);
        OwnerService.OwnerDto mockDto = new OwnerService.OwnerDto(mockOwnerInfo, OwnerTypeEnum.DEPLOYMENT);

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
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
        OwnerReferenceInfo ownerRef = OwnerReferenceInfo.builder().kind("Deployment").name("test").build();

        OwnerService first = mockServices.get(0);
        OwnerService second = mockServices.get(1);
        OwnerService third = mockServices.get(2);

        when(first.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(third.getOwner(ownerRef)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));

        Assertions.assertThrows(KubernetesException.class, () -> callChainService.getOwner(ownerRef));

        verify(first, times(1)).getOwner(ownerRef);
        verify(second, times(1)).getOwner(ownerRef);
        verify(third, times(1)).getOwner(ownerRef);
    }
}
