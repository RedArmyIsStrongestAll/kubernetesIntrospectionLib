package kubernetes.introspection.controllers;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import kubernetes.introspection.entities.enviroment.CollectionError;
import kubernetes.introspection.entities.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.service.ServiceEndpointAddress;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.useCases.env.GetVarsServicesDtoService;
import kubernetes.introspection.useCases.env.KubernetesFileReadService;
import kubernetes.introspection.useCases.env.KubernetesFileReadServiceFileImpl;
import kubernetes.introspection.useCases.init.InitDetectorService;
import kubernetes.introspection.useCases.init.InitPermissionsService;
import kubernetes.introspection.useCases.main.owner.OwnerCallChainService;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceCronJobExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceDaemonSetExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceDeploymentExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceJobExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceReplicaSetExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceReplicationControllerExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceStatefulSetExt;
import kubernetes.introspection.useCases.main.owner.delegate.OwnerServiceUnknownExt;
import kubernetes.introspection.useCases.main.owner.reference.OwnerReferenceService;
import kubernetes.introspection.useCases.main.pod.CurrentPodService;
import kubernetes.introspection.useCases.main.pod.CurrentPorCallChainService;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceConstDownwardApiExt;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceConstIpPodExt;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceConstNamePodExt;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceHostnameInetAddressExt;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceHostnamePathFileExt;
import kubernetes.introspection.useCases.main.pod.delegate.CurrentPodServiceLabelsExt;
import kubernetes.introspection.useCases.main.replics.ReplicaPodsService;
import kubernetes.introspection.useCases.main.replics.owner.OwnerLabelCallChainService;
import kubernetes.introspection.useCases.main.replics.owner.OwnerLabelService;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceCronJobExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceDaemonSetExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceDeploymentExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceJobExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceReplicaSetExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceReplicationControllerExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceStatefulSetExt;
import kubernetes.introspection.useCases.main.replics.owner.delegate.OwnerLabelServiceUnknownExt;
import kubernetes.introspection.useCases.main.service.EndpointService;
import kubernetes.introspection.useCases.main.service.ServiceService;
import kubernetes.introspection.useCases.main.source.ConfigMapSourceService;
import kubernetes.introspection.useCases.main.source.SecretSourceService;
import kubernetes.introspection.useCases.utils.ConvertorToCollectionErrorUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KubernetesIntrospectionEnvironmentServiceImpl implements KubernetesIntrospectionEnvironmentService {

    private final InitDetectorService initDetectorService;
    List<CurrentPodService> podCallServiceList;
    List<OwnerService> ownerCallServiceList;
    List<OwnerLabelService> replicCallServiceList;

    public KubernetesIntrospectionEnvironmentServiceImpl() {
        KubernetesFileReadService k8sFileReadService = new KubernetesFileReadServiceFileImpl();
        this.initDetectorService = new InitDetectorService(k8sFileReadService);
    }

    @Deprecated(since = "Using only for test")
    public KubernetesIntrospectionEnvironmentServiceImpl(InitDetectorService initDetectorService) {
        this.initDetectorService = initDetectorService;
    }

    @Override
    public KubernetesEnvironmentInfo getKubernetesEnvironmentInfo(GetVarsServicesDtoService vars) {
        try {
            log.info("Starting getKubernetesEnvironmentInfo");

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
                fillEndpointToService(endpointDto, serviceDto);

                ConfigMapSourceService.ConfigMapDto configMapDto = getConfigMaps(client, namespace, currentPodDto, permissionInfo, collectionErrorList);
                SecretSourceService.SecretDto secretDto = getSecrets(client, namespace, currentPodDto, permissionInfo, collectionErrorList);
                List<ConfigSourceInfo> configSourceInfoList = mergerConfigSourceInfo(configMapDto.getConfigSourceInfoList(), secretDto.getConfigSourceInfoList());

                KubernetesEnvironmentInfo kubernetesEnvironmentInfo = KubernetesEnvironmentInfo.builder()
                        .currentPod(currentPodDto.getPodInfo())
                        .owner(ownerDto.getOwnerInfo())
                        .replicaPods(replicaPodsDto.getPodInfoList())
                        .services(serviceDto.getServiceInfo())
                        .configSources(configSourceInfoList)
                        .collectionTimestamp(Instant.now().toString())
                        .errors(collectionErrorList).build();

                log.info("getKubernetesEnvironmentInfo result: {}", kubernetesEnvironmentInfo);
                return kubernetesEnvironmentInfo;

            } finally {
                disableServices();
            }
        } catch (KubernetesException e) {
            log.error("Critical known error in getKubernetesEnvironmentInfo: {}", e.getErrorCodeEnum(), e);
            CollectionError error = ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum());
            return KubernetesEnvironmentInfo.builder().errors(List.of(error)).build();
        } catch (Exception e) {
            log.error("Critical unknown error in getKubernetesEnvironmentInfo: {}", e.getMessage(), e);
            CollectionError error = ConvertorToCollectionErrorUtil.convertToCollectionErrors(e);
            return KubernetesEnvironmentInfo.builder().errors(List.of(error)).build();
        }
    }

    private String getNamespace() throws KubernetesException {
        log.info("Starting getNamespace");
        try {
            String namespace = initDetectorService.getNamespace();
            log.info("getNamespace result: {}", namespace);
            return namespace;
        } catch (KubernetesException e) {
            log.error("Error in getNamespace: {}", e.getMessage(), e);
            throw e;
        }
    }

    private PermissionInfo getPermission(KubernetesClient client, String namespace) throws KubernetesException {
        log.info("Starting getPermission");
        try {
            InitPermissionsService initPermissionsService = new InitPermissionsService(client);
            PermissionInfo permissionInfo = initPermissionsService.checkPermissions(namespace);
            log.info("getPermission result: {}", permissionInfo);
            return permissionInfo;
        } catch (KubernetesException e) {
            log.error("Error in getPermission: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void initServices(KubernetesClient client, String namespace, GetVarsServicesDtoService vars) {
        log.info("Starting initServices");
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
        log.info("initServices finished");
    }

    private CurrentPodService.CurrentPodDto getCurrentPod(PermissionInfo permissionInfo) throws KubernetesException {
        log.info("Starting getCurrentPod");
        try {
            CurrentPorCallChainService currentPorCallChainService = new CurrentPorCallChainService(podCallServiceList);
            CurrentPodService.CurrentPodDto currentPodDto = currentPorCallChainService.getPodWithPermission(permissionInfo);
            log.info("getCurrentPod result: {}", currentPodDto);
            return currentPodDto;
        } catch (KubernetesException e) {
            log.error("Error in getCurrentPod: {}", e.getMessage(), e);
            throw e;
        }
    }

    private OwnerReference getOwnerReference(CurrentPodService.CurrentPodDto currentPodDto, List<CollectionError> collectionErrorList) {
        log.info("Starting getOwnerReference");
        try {
            OwnerReferenceService ownerReferenceService = new OwnerReferenceService();
            OwnerReference k8sOwnerReference = ownerReferenceService.getPodOwner(currentPodDto.getK8sPod());
            log.info("getOwnerReference result: {}", k8sOwnerReference);
            return k8sOwnerReference;
        } catch (KubernetesException e) {
            log.error("Error in getOwnerReference: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return null;
        }
    }

    private OwnerService.OwnerDto getOwner(PermissionInfo permissionInfo, OwnerReference k8sOwnerReference, List<CollectionError> collectionErrorList) {
        log.info("Starting getOwner");
        try {
            OwnerCallChainService ownerCallChainService = new OwnerCallChainService(ownerCallServiceList);
            OwnerService.OwnerDto ownerDto = ownerCallChainService.getOwnerWithPermission(k8sOwnerReference, permissionInfo);
            log.info("getOwner result: {}", ownerDto);
            return ownerDto;
        } catch (KubernetesException e) {
            log.error("Error in getOwner: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new OwnerService.OwnerDto(null, null, null);
        }
    }

    private ReplicaPodsService.ReplicaPodsDto getReplicaPods(PermissionInfo permissionInfo, KubernetesClient client, OwnerReference k8sOwnerReference, OwnerService.OwnerDto ownerDto, CurrentPodService.CurrentPodDto currentPodDto, List<CollectionError> collectionErrorList) {
        log.info("Starting getReplicaPods");
        try {
            OwnerLabelCallChainService ownerLabelCallChainService = new OwnerLabelCallChainService(replicCallServiceList);
            ReplicaPodsService replicaPodsService = new ReplicaPodsService(client, ownerLabelCallChainService);
            ReplicaPodsService.ReplicaPodsDto replicaPodsDto = replicaPodsService.getReplicaPodsWithPermission(k8sOwnerReference, ownerDto, currentPodDto.getK8sPod(), permissionInfo);
            log.info("getReplicaPods result: {}", replicaPodsDto);
            return replicaPodsDto;
        } catch (KubernetesException e) {
            log.error("Error in getReplicaPods: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new ReplicaPodsService.ReplicaPodsDto(null, null);
        }
    }

    private ServiceService.ServiceDto getServices(PermissionInfo permissionInfo, String namespace, KubernetesClient client, CurrentPodService.CurrentPodDto currentPodDto, List<CollectionError> collectionErrorList) {
        log.info("Starting getServices");
        try {
            ServiceService serviceService = new ServiceService(client);
            ServiceService.ServiceDto serviceDto = serviceService.findServicesForPodWithPermission(currentPodDto.getPodInfo(), namespace, permissionInfo);
            log.info("getServices result: {}", serviceDto);
            return serviceDto;
        } catch (KubernetesException e) {
            log.error("Error in getServices: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new ServiceService.ServiceDto(null, null);
        }
    }

    private EndpointService.EndpointDto getEndpoints(String serviceName, String namespace, KubernetesClient client, PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {
        log.info("Starting getEndpoints");
        try {
            EndpointService endpointService = new EndpointService(client);
            EndpointService.EndpointDto endpointDto = endpointService.getEndpointsForServiceWithPermission(serviceName, namespace, permissionInfo);
            log.info("getEndpoints result: {}", endpointDto);
            return endpointDto;
        } catch (KubernetesException e) {
            log.error("Error in getEndpoints: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new EndpointService.EndpointDto(null, null);
        }
    }

    private static void fillEndpointToService(EndpointService.EndpointDto endpointDto, ServiceService.ServiceDto serviceDto) {
        List<ServiceEndpointAddress> endpoints = endpointDto.getEndpointsInfo();
        int readyEndpoints = (int) endpoints.stream().filter(ServiceEndpointAddress::isReady).count();
        serviceDto.getServiceInfo().setEndpoints(endpoints);
        serviceDto.getServiceInfo().setReadyEndpoints(readyEndpoints);
        serviceDto.getServiceInfo().setFullyReady(readyEndpoints > 0 && readyEndpoints == endpoints.size());
    }

    private ConfigMapSourceService.ConfigMapDto getConfigMaps(KubernetesClient client, String namespace, CurrentPodService.CurrentPodDto currentPodDto, PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {
        log.info("Starting getConfigMaps");
        try {
            ConfigMapSourceService configMapSourceService = new ConfigMapSourceService(client, namespace);
            ConfigMapSourceService.ConfigMapDto configMapDto = configMapSourceService.getConfigMapSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
            log.info("getConfigMaps result: {}", configMapDto);
            return configMapDto;
        } catch (KubernetesException e) {
            log.error("Error in getConfigMaps: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new ConfigMapSourceService.ConfigMapDto();
        }
    }

    private SecretSourceService.SecretDto getSecrets(KubernetesClient client, String namespace, CurrentPodService.CurrentPodDto currentPodDto, PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {
        log.info("Starting getSecrets");
        try {
            SecretSourceService secretSourceService = new SecretSourceService(client, namespace);
            SecretSourceService.SecretDto secretDto = secretSourceService.getSecretSourcesWithPermission(currentPodDto.getK8sPod(), permissionInfo);
            log.info("getSecrets result: {}", secretDto);
            return secretDto;
        } catch (KubernetesException e) {
            log.error("Error in getSecrets: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new SecretSourceService.SecretDto();
        }
    }

    private List<ConfigSourceInfo> mergerConfigSourceInfo(List<ConfigSourceInfo> configMapList, List<ConfigSourceInfo> secretsList) {
        log.info("Starting mergerConfigSourceInfo");
        if (configMapList != null && secretsList == null) {
            log.info("mergerConfigSourceInfo result: {}", configMapList);
            return configMapList;
        }
        if (secretsList != null && configMapList == null) {
            log.info("mergerConfigSourceInfo result: {}", secretsList);
            return secretsList;
        }
        if (secretsList == null && configMapList == null) {
            log.info("mergerConfigSourceInfo result: null");
            return null;
        }
        List<ConfigSourceInfo> configSourceInfoList = new ArrayList<>();
        configSourceInfoList.addAll(configMapList);
        configSourceInfoList.addAll(secretsList);
        log.info("mergerConfigSourceInfo result: {}", configSourceInfoList);
        return configSourceInfoList;
    }

    private void disableServices() {
        log.info("Starting disableServices");
        podCallServiceList = null;
        ownerCallServiceList = null;
        replicCallServiceList = null;
        log.info("disableServices finished");
    }
}