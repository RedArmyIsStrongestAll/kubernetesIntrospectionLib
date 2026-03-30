package entities.services.main.pod.chain;

import io.fabric8.kubernetes.api.model.Pod;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.pod.PodInfo;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.CurrentPorCallChainService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CurrentPodCallChainServiceTest {

    private CurrentPorCallChainService callChainService;
    private List<CurrentPodService> mockServices;

    @BeforeEach
    void setUp() {
        mockServices = List.of(
                mock(CurrentPodService.class),
                mock(CurrentPodService.class),
                mock(CurrentPodService.class)
        );
        callChainService = new CurrentPorCallChainService(mockServices);
    }

    @Test
    void getPodWithPermissionLastServiceValidTest() {
        PermissionInfo permissionInfo = new PermissionInfo();

        Pod mockPod = mock(Pod.class);
        PodInfo mockPodInfo = mock(PodInfo.class);
        CurrentPodService.CurrentPodDto mockDto = new CurrentPodService.CurrentPodDto(mockPod, mockPodInfo);

        CurrentPodService first = mockServices.get(0);
        CurrentPodService second = mockServices.get(1);
        CurrentPodService third = mockServices.get(2);

        when(first.getCurrentPodWithCheckPermissions(permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getCurrentPodWithCheckPermissions(permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getCurrentPodWithCheckPermissions(permissionInfo)).thenReturn(mockDto);

        CurrentPodService.CurrentPodDto result = callChainService.getPodWithPermission(permissionInfo);

        Assertions.assertNotNull(result);
        Assertions.assertSame(mockDto, result);

        verify(first, times(1)).getCurrentPodWithCheckPermissions(permissionInfo);
        verify(second, times(1)).getCurrentPodWithCheckPermissions(permissionInfo);
        verify(third, times(1)).getCurrentPodWithCheckPermissions(permissionInfo);
    }

    @Test
    void getPodWithPermissionNoValidTest() {
        PermissionInfo permissionInfo = new PermissionInfo();

        Pod mockPod = mock(Pod.class);
        PodInfo mockPodInfo = mock(PodInfo.class);
        CurrentPodService.CurrentPodDto mockDto = new CurrentPodService.CurrentPodDto(mockPod, mockPodInfo);

        CurrentPodService first = mockServices.get(0);
        CurrentPodService second = mockServices.get(1);
        CurrentPodService third = mockServices.get(2);

        when(first.getCurrentPodWithCheckPermissions(permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getCurrentPodWithCheckPermissions(permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getCurrentPodWithCheckPermissions(permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));


        Assertions.assertThrows(KubernetesException.class, () -> callChainService.getPodWithPermission(permissionInfo));

        verify(first, times(1)).getCurrentPodWithCheckPermissions(permissionInfo);
        verify(second, times(1)).getCurrentPodWithCheckPermissions(permissionInfo);
        verify(third, times(1)).getCurrentPodWithCheckPermissions(permissionInfo);
    }

    @Test
    void getPodLastServiceValidTest() {
        Pod mockPod = mock(Pod.class);
        PodInfo mockPodInfo = mock(PodInfo.class);
        CurrentPodService.CurrentPodDto mockDto = new CurrentPodService.CurrentPodDto(mockPod, mockPodInfo);

        CurrentPodService first = mockServices.get(0);
        CurrentPodService second = mockServices.get(1);
        CurrentPodService third = mockServices.get(2);

        when(first.getCurrentPod()).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getCurrentPod()).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getCurrentPod()).thenReturn(mockDto);

        CurrentPodService.CurrentPodDto result = callChainService.getPod();

        Assertions.assertNotNull(result);
        Assertions.assertSame(mockDto, result);

        verify(first, times(1)).getCurrentPod();
        verify(second, times(1)).getCurrentPod();
        verify(third, times(1)).getCurrentPod();
    }

    @Test
    void getPodNoValidTest() {
        Pod mockPod = mock(Pod.class);
        PodInfo mockPodInfo = mock(PodInfo.class);
        CurrentPodService.CurrentPodDto mockDto = new CurrentPodService.CurrentPodDto(mockPod, mockPodInfo);

        CurrentPodService first = mockServices.get(0);
        CurrentPodService second = mockServices.get(1);
        CurrentPodService third = mockServices.get(2);

        when(first.getCurrentPod()).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(second.getCurrentPod()).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));
        when(third.getCurrentPod()).thenThrow(new KubernetesException(ErrorCodeEnum.POD_NOT_FOUND));

        Assertions.assertThrows(KubernetesException.class, () -> callChainService.getPod());

        verify(first, times(1)).getCurrentPod();
        verify(second, times(1)).getCurrentPod();
        verify(third, times(1)).getCurrentPod();
    }
}
