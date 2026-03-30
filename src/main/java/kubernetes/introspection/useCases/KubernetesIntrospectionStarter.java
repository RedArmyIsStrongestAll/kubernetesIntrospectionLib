package kubernetes.introspection.useCases;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import kubernetes.introspection.entities.models.enviroment.CollectionError;
import kubernetes.introspection.entities.models.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
import kubernetes.introspection.entities.models.source.ConfigSourceInfo;
import kubernetes.introspection.entities.services.env.GetVarsServicesDtoService;
import kubernetes.introspection.entities.services.init.InitDetectorService;
import kubernetes.introspection.entities.services.init.InitPermissionsService;
import kubernetes.introspection.entities.services.main.owner.OwnerCallChainService;
import kubernetes.introspection.entities.services.main.owner.OwnerService;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceCronJobExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceDaemonSetExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceDeploymentExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceJobExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceReplicaSetExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceReplicationControllerExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceStatefulSetExt;
import kubernetes.introspection.entities.services.main.owner.delegate.OwnerServiceUnknownExt;
import kubernetes.introspection.entities.services.main.owner.reference.OwnerReferenceService;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.CurrentPorCallChainService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstDownwardApiExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstIpPodExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstNamePodExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceHostnameInetAddressExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceHostnamePathFileExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceLabelsExt;
import kubernetes.introspection.entities.services.main.replics.ReplicaPodsService;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelCallChainService;
import kubernetes.introspection.entities.services.main.replics.owner.OwnerLabelService;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceCronJobExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceDaemonSetExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceDeploymentExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceJobExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceReplicaSetExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceReplicationControllerExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceStatefulSetExt;
import kubernetes.introspection.entities.services.main.replics.owner.delegate.OwnerLabelServiceUnknownExt;
import kubernetes.introspection.entities.services.main.service.EndpointService;
import kubernetes.introspection.entities.services.main.service.ServiceService;
import kubernetes.introspection.entities.services.main.source.ConfigMapSourceService;
import kubernetes.introspection.entities.services.main.source.SecretSourceService;
import kubernetes.introspection.entities.services.utils.ConvertorToCollectionErrorUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KubernetesIntrospectionStarter {

    private final InitDetectorService initDetectorService;

    List<CurrentPodService> podCallServiceList;
    List<OwnerService> ownerCallServiceList;
    List<OwnerLabelService> replicCallServiceList;

    public KubernetesIntrospectionStarter() {
        initDetectorService = new InitDetectorService();
    }

    public KubernetesEnvironmentInfo getKubernetesEnvironmentInfo(GetVarsServicesDtoService vars) {

        try {
            String namespace = getNamespace();

            try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                PermissionInfo permissionInfo = getPermission(client, namespace);
                List<CollectionError> collectionErrorList = ConvertorToCollectionErrorUtil.convertToCollectionErrors(permissionInfo, namespace);

                initServices(client, namespace, vars);

                CurrentPodService.CurrentPodDto currentPodDto = getCurrentPod(permissionInfo);

                OwnerReference k8sOwnerReference = getOwnerReference(currentPodDto, collectionErrorList);

                OwnerService.OwnerDto ownerDto = getOwner(permissionInfo, k8sOwnerReference, collectionErrorList);

                ReplicaPodsService.ReplicaPodsDto replicaPodsDto = getReplicaPods(permissionInfo, client, k8sOwnerReference, ownerDto, currentPodDto, collectionErrorList);

                ServiceService.ServiceDto serviceDto = getServices(permissionInfo, namespace, client, currentPodDto, collectionErrorList);
                EndpointService.EndpointDto endpointDto = getEndpoints(serviceDto.getServiceInfo().getName(), namespace, client, permissionInfo, collectionErrorList);

                ConfigMapSourceService.ConfigMapDto configMapDto = getConfigMaps(client, namespace, currentPodDto, permissionInfo, collectionErrorList);
                SecretSourceService.SecretDto secretDto = getSecrets(client, namespace, currentPodDto, permissionInfo, collectionErrorList);
                List<ConfigSourceInfo> configSourceInfoList = mergerConfigSourceInfo(configMapDto.getConfigSourceInfoList(), secretDto.getConfigSourceInfoList());

                KubernetesEnvironmentInfo kubernetesEnvironmentInfo = KubernetesEnvironmentInfo.builder()
                        .currentPod(currentPodDto.getPodInfo())
                        .owner(ownerDto.getOwnerInfo())
                        .replicaPods(replicaPodsDto.getPodInfoList())
                        .services(serviceDto.getServiceInfo())
                        .configSources(configMapDto.getConfigSourceInfoList())
                        .collectionTimestamp(Instant.now().toString())
                        .errors(collectionErrorList).build();
                return kubernetesEnvironmentInfo;

            } finally {
                disableServices();
            }

        } catch (KubernetesException e) {
            log.error("Critical known error: {}", e.getErrorCodeEnum());
            CollectionError error = ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum());
            return KubernetesEnvironmentInfo.builder().errors(List.of(error)).build();

        } catch (Exception e) {
            log.error("Critical unknown error: {}", e.getMessage());
            CollectionError error = ConvertorToCollectionErrorUtil.convertToCollectionErrors(e);
            return KubernetesEnvironmentInfo.builder().errors(List.of(error)).build();
        }
    }

    private String getNamespace() throws KubernetesException {
        return initDetectorService.getNamespace();
    }

    private PermissionInfo getPermission(KubernetesClient client, String namespace) throws KubernetesException {
        InitPermissionsService initPermissionsService = new InitPermissionsService(client);
        return initPermissionsService.checkPermissions(namespace);
    }


    private void initServices(KubernetesClient client, String namespace, GetVarsServicesDtoService vars) {
        podCallServiceList = List.of(
                new CurrentPodServiceConstDownwardApiExt(client, namespace, vars.getEnvironmentProviderSystemImpl()),
                new CurrentPodServiceHostnameInetAddressExt(client, namespace, vars.getEnvironmentProviderSystemImpl()),
                new CurrentPodServiceHostnamePathFileExt(client, namespace, vars.getEnvironmentProviderSystemImpl()),
                new CurrentPodServiceConstNamePodExt(client, namespace, vars.getPodConstName()),
                new CurrentPodServiceLabelsExt(client, namespace, vars.getPodConstLabels()),
                new CurrentPodServiceConstIpPodExt(client, namespace, vars.getPodConstIp())
        );

        ownerCallServiceList = List.of(
                new OwnerServiceCronJobExt(client, namespace),
                new OwnerServiceDaemonSetExt(client, namespace),
                new OwnerServiceDeploymentExt(client, namespace),
                new OwnerServiceJobExt(client, namespace),
                new OwnerServiceReplicaSetExt(client, namespace),
                new OwnerServiceReplicationControllerExt(client, namespace),
                new OwnerServiceStatefulSetExt(client, namespace),
                new OwnerServiceUnknownExt(client, namespace)
        );

        replicCallServiceList = List.of(
                new OwnerLabelServiceCronJobExt(),
                new OwnerLabelServiceDaemonSetExt(),
                new OwnerLabelServiceDeploymentExt(),
                new OwnerLabelServiceJobExt(),
                new OwnerLabelServiceReplicaSetExt(),
                new OwnerLabelServiceReplicationControllerExt(),
                new OwnerLabelServiceStatefulSetExt(),
                new OwnerLabelServiceUnknownExt()
        );
    }

    private CurrentPodService.CurrentPodDto getCurrentPod(PermissionInfo permissionInfo) throws KubernetesException {
        CurrentPorCallChainService currentPorCallChainService = new CurrentPorCallChainService(podCallServiceList);
        return currentPorCallChainService.getPodWithPermission(permissionInfo);
    }


    private OwnerReference getOwnerReference(CurrentPodService.CurrentPodDto currentPodDto,
                                             List<CollectionError> collectionErrorList) {

        OwnerReference k8sOwnerReference;
        try {
            OwnerReferenceService ownerReferenceService = new OwnerReferenceService();
            k8sOwnerReference = ownerReferenceService.getPodOwner(currentPodDto.getK8sPod());
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return null;
        }
        return k8sOwnerReference;
    }

    private OwnerService.OwnerDto getOwner(PermissionInfo permissionInfo, OwnerReference k8sOwnerReference,
                                           List<CollectionError> collectionErrorList) {

        OwnerService.OwnerDto ownerDto;
        try {
            OwnerCallChainService ownerCallChainService = new OwnerCallChainService(ownerCallServiceList);
            ownerDto = ownerCallChainService.getOwnerWithPermission(k8sOwnerReference, permissionInfo);
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            ownerDto = new OwnerService.OwnerDto(null, null, null);
        }
        return ownerDto;
    }

    private ReplicaPodsService.ReplicaPodsDto getReplicaPods(PermissionInfo permissionInfo, KubernetesClient client,
                                                             OwnerReference k8sOwnerReference, OwnerService.OwnerDto ownerDto,
                                                             CurrentPodService.CurrentPodDto currentPodDto,
                                                             List<CollectionError> collectionErrorList) {

        ReplicaPodsService.ReplicaPodsDto replicaPodsDto;
        try {
            OwnerLabelCallChainService ownerLabelCallChainService = new OwnerLabelCallChainService(replicCallServiceList);
            ReplicaPodsService replicaPodsService = new ReplicaPodsService(client, ownerLabelCallChainService);
            replicaPodsDto = replicaPodsService.getReplicaPodsWithPermission(k8sOwnerReference, ownerDto, currentPodDto.getK8sPod(), permissionInfo);

        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            replicaPodsDto = new ReplicaPodsService.ReplicaPodsDto(null, null);
        }
        return replicaPodsDto;
    }

    private ServiceService.ServiceDto getServices(PermissionInfo permissionInfo, String namespace,
                                                  KubernetesClient client, CurrentPodService.CurrentPodDto currentPodDto,
                                                  List<CollectionError> collectionErrorList) {

        ServiceService.ServiceDto serviceDto;
        try {
            ServiceService serviceService = new ServiceService(client);
            serviceDto = serviceService.findServicesForPodWithPermission(currentPodDto.getPodInfo(), namespace, permissionInfo);
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            serviceDto = new ServiceService.ServiceDto(null, null);
        }
        return serviceDto;
    }

    private EndpointService.EndpointDto getEndpoints(String serviceName, String namespace, KubernetesClient client,
                                                     PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {

        EndpointService.EndpointDto endpointDto;
        try {
            EndpointService endpointService = new EndpointService(client);
            endpointDto = endpointService.getEndpointsForServiceWithPermission(serviceName, namespace, permissionInfo);
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            endpointDto = new EndpointService.EndpointDto(null, null);
        }
        return endpointDto;
    }

    private ConfigMapSourceService.ConfigMapDto getConfigMaps(KubernetesClient client, String namespace,
                                                              CurrentPodService.CurrentPodDto currentPodDto,
                                                              PermissionInfo permissionInfo,
                                                              List<CollectionError> collectionErrorList) {

        ConfigMapSourceService.ConfigMapDto configMapDto;
        try {
            ConfigMapSourceService configMapSourceService = new ConfigMapSourceService(client, namespace);
            configMapDto = configMapSourceService.getConfigMapSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            configMapDto = new ConfigMapSourceService.ConfigMapDto();
        }
        return configMapDto;
    }

    private SecretSourceService.SecretDto getSecrets(KubernetesClient client, String namespace,
                                                     CurrentPodService.CurrentPodDto currentPodDto,
                                                     PermissionInfo permissionInfo,
                                                     List<CollectionError> collectionErrorList) {

        SecretSourceService.SecretDto secretDto;
        try {
            SecretSourceService secretSourceService = new SecretSourceService(client, namespace);
            secretDto = secretSourceService.getSecretSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            secretDto = new SecretSourceService.SecretDto();
        }
        return secretDto;
    }

    private List<ConfigSourceInfo> mergerConfigSourceInfo(List<ConfigSourceInfo> configMapList,
                                                          List<ConfigSourceInfo> secretsList) {

        if (configMapList != null && secretsList == null) return configMapList;
        if (secretsList != null && configMapList == null) return secretsList;

        if (secretsList == null && configMapList == null) return null;

        List<ConfigSourceInfo> configSourceInfoList = new ArrayList<>();
        configSourceInfoList.addAll(configMapList);
        configSourceInfoList.addAll(secretsList);
        return configSourceInfoList;
    }


    private void disableServices() {
        podCallServiceList = null;
        ownerCallServiceList = null;
        replicCallServiceList = null;
    }
}
