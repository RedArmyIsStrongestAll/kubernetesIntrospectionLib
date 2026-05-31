package kubernetes.introspection.controllers;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import kubernetes.introspection.adapters.kubernetes.*;
import kubernetes.introspection.entities.enviroment.CollectionError;
import kubernetes.introspection.entities.enviroment.KubernetesEnvironmentInfo;
import kubernetes.introspection.entities.exceptions.KubernetesException;
import kubernetes.introspection.entities.owner.OwnerReferenceInfo;
import kubernetes.introspection.entities.permision.PermissionInfo;
import kubernetes.introspection.entities.pod.PodInfo;
import kubernetes.introspection.entities.service.ServiceEndpointAddress;
import kubernetes.introspection.entities.source.ConfigSourceInfo;
import kubernetes.introspection.useCases.env.GetVarsServicesDtoService;
import kubernetes.introspection.useCases.env.KubernetesFileReadService;
import kubernetes.introspection.useCases.env.KubernetesFileReadServiceFileImpl;
import kubernetes.introspection.useCases.init.InitDetectorService;
import kubernetes.introspection.useCases.init.InitPermissionsService;
import kubernetes.introspection.useCases.main.owner.OwnerCallChainService;
import kubernetes.introspection.useCases.main.owner.OwnerService;
import kubernetes.introspection.useCases.main.owner.delegate.*;
import kubernetes.introspection.useCases.main.owner.reference.OwnerReferenceService;
import kubernetes.introspection.useCases.main.pod.CurrentPodService;
import kubernetes.introspection.useCases.main.pod.CurrentPorCallChainService;
import kubernetes.introspection.useCases.main.pod.delegate.*;
import kubernetes.introspection.useCases.main.replics.ReplicaPodsService;
import kubernetes.introspection.useCases.main.service.EndpointService;
import kubernetes.introspection.useCases.main.service.ServiceService;
import kubernetes.introspection.useCases.main.source.ConfigMapSourceService;
import kubernetes.introspection.useCases.main.source.SecretSourceService;
import kubernetes.introspection.useCases.ports.KubernetesConfigPort;
import kubernetes.introspection.useCases.ports.KubernetesOwnerPort;
import kubernetes.introspection.useCases.ports.KubernetesPermissionPort;
import kubernetes.introspection.useCases.ports.KubernetesPodPort;
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
    KubernetesPodPort podPort;
    KubernetesOwnerPort ownerPort;
    KubernetesConfigPort configPort;

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
                return getKubernetesEnvironmentInfo(vars, client, namespace);
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

    @Override
    public KubernetesEnvironmentInfo getKubernetesEnvironmentInfoWithClient(GetVarsServicesDtoService vars, KubernetesClient client) {
        try {
            log.info("Starting getKubernetesEnvironmentInfoWithClient");
            String namespace = getNamespace();
            return getKubernetesEnvironmentInfo(vars, client, namespace);
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

    private KubernetesEnvironmentInfo getKubernetesEnvironmentInfo(GetVarsServicesDtoService vars, KubernetesClient client, String namespace) {
        PermissionInfo permissionInfo = getPermission(client, namespace);
        List<CollectionError> collectionErrorList = ConvertorToCollectionErrorUtil.convertToCollectionErrors(permissionInfo, namespace);

        initServices(client, namespace, vars);

        CurrentPodService.CurrentPodDto currentPodDto = getCurrentPod(permissionInfo);
        PodInfo currentPod = currentPodDto.getPodInfo();

        OwnerReferenceInfo ownerRef = getOwnerReference(currentPod, collectionErrorList);
        OwnerService.OwnerDto ownerDto = getOwner(permissionInfo, ownerRef, collectionErrorList);

        ReplicaPodsService.ReplicaPodsDto replicaPodsDto = getReplicaPods(permissionInfo, ownerRef, ownerDto, currentPod, collectionErrorList);

        ServiceService.ServiceDto serviceDto = getServices(permissionInfo, namespace, client, currentPod, collectionErrorList);
        EndpointService.EndpointDto endpointDto = getEndpoints(serviceDto.getServiceInfo().getName(), namespace, client, permissionInfo, collectionErrorList);
        fillEndpointToService(endpointDto, serviceDto);

        ConfigMapSourceService.ConfigMapDto configMapDto = getConfigMaps(client, namespace, currentPod, permissionInfo, collectionErrorList);
        SecretSourceService.SecretDto secretDto = getSecrets(client, namespace, currentPod, permissionInfo, collectionErrorList);
        List<ConfigSourceInfo> configSourceInfoList = mergerConfigSourceInfo(configMapDto.getConfigSourceInfoList(), secretDto.getConfigSourceInfoList());

        KubernetesEnvironmentInfo kubernetesEnvironmentInfo = KubernetesEnvironmentInfo.builder()
                .currentPod(currentPod)
                .owner(ownerDto.getOwnerInfo())
                .replicaPods(replicaPodsDto.getPodInfoList())
                .services(serviceDto.getServiceInfo())
                .configSources(configSourceInfoList)
                .collectionTimestamp(Instant.now().toString())
                .errors(collectionErrorList).build();

        log.info("getKubernetesEnvironmentInfo result: {}", kubernetesEnvironmentInfo);
        return kubernetesEnvironmentInfo;
    }

    private PermissionInfo getPermission(KubernetesClient client, String namespace) throws KubernetesException {
        log.info("Starting getPermission");
        try {
            KubernetesPermissionPort permPort = new Fabric8PermissionAdapter(client);
            InitPermissionsService initPermissionsService = new InitPermissionsService(permPort);
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
        podPort = new Fabric8PodAdapter(client);
        ownerPort = new Fabric8OwnerAdapter(client);
        configPort = new Fabric8ConfigAdapter(client);

        podCallServiceList = List.of(
                new CurrentPodServiceConstDownwardApiExt(podPort, namespace, vars.getEnvironmentProviderSystemImpl()),
                new CurrentPodServiceHostnameInetAddressExt(podPort, namespace, vars.getEnvironmentProviderSystemImpl()),
                new CurrentPodServiceHostnamePathFileExt(podPort, namespace, vars.getEnvironmentProviderSystemImpl()),
                new CurrentPodServiceConstNamePodExt(podPort, namespace, vars.getPodConstName()),
                new CurrentPodServiceLabelsExt(podPort, namespace, vars.getPodConstLabels()),
                new CurrentPodServiceConstIpPodExt(podPort, namespace, vars.getPodConstIp())
        );
        ownerCallServiceList = List.of(
                new OwnerServiceCronJobExt(ownerPort, namespace),
                new OwnerServiceDaemonSetExt(ownerPort, namespace),
                new OwnerServiceDeploymentExt(ownerPort, namespace),
                new OwnerServiceJobExt(ownerPort, namespace),
                new OwnerServiceReplicaSetExt(ownerPort, namespace),
                new OwnerServiceReplicationControllerExt(ownerPort, namespace),
                new OwnerServiceStatefulSetExt(ownerPort, namespace),
                new OwnerServiceUnknownExt(ownerPort, namespace)
        );
        log.info("initServices finished");
    }

    private CurrentPodService.CurrentPodDto getCurrentPod(PermissionInfo permissionInfo) throws KubernetesException {
        log.info("Starting getCurrentPod");
        try {
            CurrentPorCallChainService chain = new CurrentPorCallChainService(podCallServiceList);
            CurrentPodService.CurrentPodDto dto = chain.getPodWithPermission(permissionInfo);
            log.info("getCurrentPod result: {}", dto);
            return dto;
        } catch (KubernetesException e) {
            log.error("Error in getCurrentPod: {}", e.getMessage(), e);
            throw e;
        }
    }

    private OwnerReferenceInfo getOwnerReference(PodInfo currentPod, List<CollectionError> collectionErrorList) {
        log.info("Starting getOwnerReference");
        try {
            OwnerReferenceService ownerReferenceService = new OwnerReferenceService();
            OwnerReferenceInfo ownerRef = ownerReferenceService.getPodOwner(currentPod);
            log.info("getOwnerReference result: {}", ownerRef);
            return ownerRef;
        } catch (KubernetesException e) {
            log.error("Error in getOwnerReference: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return null;
        }
    }

    private OwnerService.OwnerDto getOwner(PermissionInfo permissionInfo, OwnerReferenceInfo ownerRef, List<CollectionError> collectionErrorList) {
        log.info("Starting getOwner");
        try {
            OwnerCallChainService ownerCallChainService = new OwnerCallChainService(ownerCallServiceList);
            OwnerService.OwnerDto ownerDto = ownerCallChainService.getOwnerWithPermission(ownerRef, permissionInfo);
            log.info("getOwner result: {}", ownerDto);
            return ownerDto;
        } catch (KubernetesException e) {
            log.error("Error in getOwner: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new OwnerService.OwnerDto(null, null);
        }
    }

    private ReplicaPodsService.ReplicaPodsDto getReplicaPods(PermissionInfo permissionInfo, OwnerReferenceInfo ownerRef,
                                                             OwnerService.OwnerDto ownerDto, PodInfo currentPod,
                                                             List<CollectionError> collectionErrorList) {
        log.info("Starting getReplicaPods");
        try {
            if (ownerDto.getK8sType() == null) {
                return new ReplicaPodsService.ReplicaPodsDto(null);
            }
            ReplicaPodsService replicaPodsService = new ReplicaPodsService(podPort);
            ReplicaPodsService.ReplicaPodsDto dto = replicaPodsService.getReplicaPodsWithPermission(ownerRef, ownerDto, currentPod, permissionInfo);
            log.info("getReplicaPods result: {}", dto);
            return dto;
        } catch (KubernetesException e) {
            log.error("Error in getReplicaPods: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new ReplicaPodsService.ReplicaPodsDto(null);
        }
    }

    private ServiceService.ServiceDto getServices(PermissionInfo permissionInfo, String namespace, KubernetesClient client, PodInfo currentPod, List<CollectionError> collectionErrorList) {
        log.info("Starting getServices");
        try {
            ServiceService serviceService = new ServiceService(new Fabric8ServiceAdapter(client));
            ServiceService.ServiceDto dto = serviceService.findServicesForPodWithPermission(currentPod, namespace, permissionInfo);
            log.info("getServices result: {}", dto);
            return dto;
        } catch (KubernetesException e) {
            log.error("Error in getServices: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new ServiceService.ServiceDto(null);
        }
    }

    private EndpointService.EndpointDto getEndpoints(String serviceName, String namespace, KubernetesClient client, PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {
        log.info("Starting getEndpoints");
        try {
            EndpointService endpointService = new EndpointService(new Fabric8EndpointAdapter(client));
            EndpointService.EndpointDto dto = endpointService.getEndpointsForServiceWithPermission(serviceName, namespace, permissionInfo);
            log.info("getEndpoints result: {}", dto);
            return dto;
        } catch (KubernetesException e) {
            log.error("Error in getEndpoints: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new EndpointService.EndpointDto(null);
        }
    }

    private static void fillEndpointToService(EndpointService.EndpointDto endpointDto, ServiceService.ServiceDto serviceDto) {
        if (endpointDto.getEndpointsInfo() == null || serviceDto.getServiceInfo() == null) return;
        List<ServiceEndpointAddress> endpoints = endpointDto.getEndpointsInfo();
        int readyEndpoints = (int) endpoints.stream().filter(ServiceEndpointAddress::isReady).count();
        serviceDto.getServiceInfo().setEndpoints(endpoints);
        serviceDto.getServiceInfo().setReadyEndpoints(readyEndpoints);
        serviceDto.getServiceInfo().setFullyReady(readyEndpoints > 0 && readyEndpoints == endpoints.size());
    }

    private ConfigMapSourceService.ConfigMapDto getConfigMaps(KubernetesClient client, String namespace, PodInfo currentPod, PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {
        log.info("Starting getConfigMaps");
        try {
            ConfigMapSourceService configMapSourceService = new ConfigMapSourceService(configPort, namespace);
            ConfigMapSourceService.ConfigMapDto dto = configMapSourceService.getConfigMapSourcesWithPermission(currentPod, permissionInfo);
            log.info("getConfigMaps result: {}", dto);
            return dto;
        } catch (KubernetesException e) {
            log.error("Error in getConfigMaps: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new ConfigMapSourceService.ConfigMapDto();
        }
    }

    private SecretSourceService.SecretDto getSecrets(KubernetesClient client, String namespace, PodInfo currentPod, PermissionInfo permissionInfo, List<CollectionError> collectionErrorList) {
        log.info("Starting getSecrets");
        try {
            SecretSourceService secretSourceService = new SecretSourceService(configPort, namespace);
            SecretSourceService.SecretDto dto = secretSourceService.getSecretSourcesWithPermission(currentPod, permissionInfo);
            log.info("getSecrets result: {}", dto);
            return dto;
        } catch (KubernetesException e) {
            log.error("Error in getSecrets: {}", e.getMessage(), e);
            collectionErrorList.add(ConvertorToCollectionErrorUtil.convertToCollectionErrors(e.getErrorCodeEnum()));
            return new SecretSourceService.SecretDto();
        }
    }

    private List<ConfigSourceInfo> mergerConfigSourceInfo(List<ConfigSourceInfo> configMapList, List<ConfigSourceInfo> secretsList) {
        log.info("Starting mergerConfigSourceInfo");
        if (configMapList != null && secretsList == null) return configMapList;
        if (secretsList != null && configMapList == null) return secretsList;
        if (secretsList == null && configMapList == null) return null;
        List<ConfigSourceInfo> result = new ArrayList<>();
        result.addAll(configMapList);
        result.addAll(secretsList);
        return result;
    }

    private void disableServices() {
        log.info("Starting disableServices");
        podCallServiceList = null;
        ownerCallServiceList = null;
        podPort = null;
        ownerPort = null;
        configPort = null;
        log.info("disableServices finished");
    }
}
