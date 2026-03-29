package entities.services.main.replics.owner.chain;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelCallChainService;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OwnerLabelCallChainServiceTest {

    private OwnerLabelCallChainService ownerLabelCallChainService;
    private List<OwnerLabelService> mockServices;

    @BeforeEach
    void setUp() {
        mockServices = List.of(
                mock(OwnerLabelService.class),
                mock(OwnerLabelService.class),
                mock(OwnerLabelService.class)
        );
        ownerLabelCallChainService = new OwnerLabelCallChainService(mockServices);
    }

    @Test
    void extractLabelSelectorWithPermissionLastServiceValidTest() {
        PermissionInfo permissionInfo = new PermissionInfo();
        OwnerTypeEnum ownerTypeEnum = OwnerTypeEnum.DAEMONSET;
        HasMetadata hasMetadata = new Deployment("v1", "Deployment", new ObjectMeta(), new DeploymentSpec(), new DeploymentStatus());

        LabelSelector mockDto = new LabelSelector();

        OwnerLabelService first = mockServices.get(0);
        OwnerLabelService second = mockServices.get(1);
        OwnerLabelService third = mockServices.get(2);

        when(first.extractLabelSelectorWithPermission(hasMetadata, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(first.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(second.extractLabelSelectorWithPermission(hasMetadata, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(third.extractLabelSelectorWithPermission(hasMetadata, permissionInfo)).thenReturn(mockDto);
        when(third.getKindOwnerType()).thenReturn(ownerTypeEnum);

        LabelSelector result = ownerLabelCallChainService.getSelectorWithPermission(ownerTypeEnum, hasMetadata, permissionInfo);

        Assertions.assertNotNull(result);
        Assertions.assertSame(mockDto, result);

        verify(first, times(1)).extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
        verify(second, times(1)).extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
        verify(third, times(1)).extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
    }

    @Test
    void extractLabelSelectorWithPermissionNoValidTest() {
        PermissionInfo permissionInfo = new PermissionInfo();
        OwnerTypeEnum ownerTypeEnum = OwnerTypeEnum.DAEMONSET;
        HasMetadata hasMetadata = new Deployment("v1", "Deployment", new ObjectMeta(), new DeploymentSpec(), new DeploymentStatus());

        OwnerLabelService first = mockServices.get(0);
        OwnerLabelService second = mockServices.get(1);
        OwnerLabelService third = mockServices.get(2);

        when(first.extractLabelSelectorWithPermission(hasMetadata, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(first.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(second.extractLabelSelectorWithPermission(hasMetadata, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(third.extractLabelSelectorWithPermission(hasMetadata, permissionInfo)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(third.getKindOwnerType()).thenReturn(ownerTypeEnum);

        Assertions.assertThrows(KubernetesException.class, () -> ownerLabelCallChainService.getSelectorWithPermission(ownerTypeEnum, hasMetadata, permissionInfo));

        verify(first, times(1)).extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
        verify(second, times(1)).extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
        verify(third, times(1)).extractLabelSelectorWithPermission(hasMetadata, permissionInfo);
    }

    @Test
    void extractLabelSelectorLastServiceValidTest() throws Exception {
        OwnerTypeEnum ownerTypeEnum = OwnerTypeEnum.DAEMONSET;
        HasMetadata hasMetadata = new Deployment("v1", "Deployment", new ObjectMeta(), new DeploymentSpec(), new DeploymentStatus());

        LabelSelector mockDto = new LabelSelector();

        OwnerLabelService first = mockServices.get(0);
        OwnerLabelService second = mockServices.get(1);
        OwnerLabelService third = mockServices.get(2);

        when(first.extractLabelSelector(hasMetadata)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(first.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(second.extractLabelSelector(hasMetadata)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(third.extractLabelSelector(hasMetadata)).thenReturn(mockDto);
        when(third.getKindOwnerType()).thenReturn(ownerTypeEnum);

        LabelSelector result = ownerLabelCallChainService.getSelector(ownerTypeEnum, hasMetadata);

        Assertions.assertNotNull(result);
        Assertions.assertSame(mockDto, result);

        verify(first, times(1)).extractLabelSelector(hasMetadata);
        verify(second, times(1)).extractLabelSelector(hasMetadata);
        verify(third, times(1)).extractLabelSelector(hasMetadata);
    }

    @Test
    void extractLabelSelectorNoValidTest() throws Exception {
        OwnerTypeEnum ownerTypeEnum = OwnerTypeEnum.DAEMONSET;
        HasMetadata hasMetadata = new Deployment("v1", "Deployment", new ObjectMeta(), new DeploymentSpec(), new DeploymentStatus());

        OwnerLabelService first = mockServices.get(0);
        OwnerLabelService second = mockServices.get(1);
        OwnerLabelService third = mockServices.get(2);

        when(first.extractLabelSelector(hasMetadata)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(first.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(second.extractLabelSelector(hasMetadata)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(second.getKindOwnerType()).thenReturn(ownerTypeEnum);
        when(third.extractLabelSelector(hasMetadata)).thenThrow(new KubernetesException(ErrorCodeEnum.OWNER_NOT_FOUND));
        when(third.getKindOwnerType()).thenReturn(ownerTypeEnum);

        Assertions.assertThrows(KubernetesException.class, () -> ownerLabelCallChainService.getSelector(ownerTypeEnum, hasMetadata));

        verify(first, times(1)).extractLabelSelector(hasMetadata);
        verify(second, times(1)).extractLabelSelector(hasMetadata);
        verify(third, times(1)).extractLabelSelector(hasMetadata);
    }


}
