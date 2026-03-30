package kubernetes.introspection.useCases;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import kubernetes.introspection.entities.models.enviroment.CollectionError;
import kubernetes.introspection.entities.models.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.models.permision.PermissionInfo;
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

            } finally {
                disableServices();
            }


            ////////////////////////////////////////МОГУТ БЫТЬ NULL//////////////////////////////////////////


            //нахожу эндпоинты
            EndpointService endpointService = new EndpointService(client);
            EndpointService.EndpointDto endpointDto = endpointService.getEndpointsForServiceWithPermission(serviceDto.getServiceInfo().getName(), namespace, permissionInfo);
            //throw new KubernetesException(ErrorCodeEnum.ENDPOINTS_NOT_FOUND);
            //collectionErrorList

            //нахожу конфигмапы
            ConfigMapSourceService configMapSourceService = new ConfigMapSourceService(client, namespace);
            ConfigMapSourceService.ConfigMapDto configMapDto = configMapSourceService.getConfigMapSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
            //throw new KubernetesException(ErrorCodeEnum.CONFIG_MAP_NOT_FOUND);
            //collectionErrorList

            //нахожу секреты
            SecretSourceService secretSourceService = new SecretSourceService(client, namespace);
            SecretSourceService.SecretDto secretDto = secretSourceService.getSecretSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
            //throw new KubernetesException(ErrorCodeEnum.SECRET_NOT_FOUND);
            //collectionErrorList

            //объединяем
            configMapDto.getConfigSourceInfoList().addAll(secretDto.getConfigSourceInfoList());
            //можетбыть null point!

            //////////////////////////////////

            KubernetesEnvironmentInfo kubernetesEnvironmentInfo = KubernetesEnvironmentInfo.builder()
                    .currentPod(currentPodDto.getPodInfo())
                    .owner(ownerDto.getOwnerInfo())
                    .replicaPods(replicaPodsDto.getPodInfoList())
                    .services(serviceDto.getServiceInfo())
                    .configSources(configMapDto.getConfigSourceInfoList())
                    .collectionTimestamp(Instant.now().toString())
                    .errors(collectionErrorList).build();

            return kubernetesEnvironmentInfo;

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
            configMapDto = new ConfigMapSourceService.ConfigMapDto(null, null);
        }
        return configMapDto
    }

    private SecretSourceService.SecretDto getSecrets(KubernetesClient client, String namespace,
                                                     CurrentPodService.CurrentPodDto currentPodDto,
                                                     PermissionInfo permissionInfo,
                                                     List<CollectionError> collectionErrorList) {
        try {
            SecretSourceService secretSourceService = new SecretSourceService(client, namespace);
            return secretSourceService.getSecretSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
        } catch (KubernetesException e) {
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new SecretSourceService.SecretDto(null, null);
        }
    }

//    private List<ConfigMapSourceService.ConfigSourceInfo> mergeConfigSources(
//            ConfigMapSourceService.ConfigMapDto configMapDto,
//            SecretSourceService.SecretDto secretDto) {
//        List<ConfigMapSourceService.ConfigSourceInfo> result = configMapDto.getConfigSourceInfoList();
//        if (secretDto != null && secretDto.getConfigSourceInfoList() != null) {
//            result.addAll(secretDto.getConfigSourceInfoList());
//        }
//        return result;
//    }

    private void disableServices() {
        podCallServiceList = null;
        ownerCallServiceList = null;
        replicCallServiceList = null;
    }
}
