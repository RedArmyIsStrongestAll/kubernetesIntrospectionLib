package kubernetes.introspection.entities.services.main.pod;


import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.dto.pod.ContainerInfo;
import kubernetes.introspection.entities.models.dto.pod.ContainerStateEnum;
import kubernetes.introspection.entities.models.dto.pod.PodInfo;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstDownwardApiExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstIpPodExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceConstNamePodExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceHostnameInetAddressExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceHostnamePathFileExt;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceLabelsExt;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.BROKEN_NAME_IN_POD;
import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.FORBIDDEN;
import static kubernetes.introspection.entities.models.exceptions.ErrorCodeEnum.POD_NOT_FOUND;

/**
 * Абстрактный базовый класс для сервисов, предоставляющих информацию о текущем Pod'е в Kubernetes.
 * Реализует общую логику получения информации о Pod'е, такую как:
 * <ul>
 *   <li>Получение имени Pod'а (через разные стратегии — метки, Downward API, hostname и др.)</li>
 *   <li>Получение объекта Pod через Fabric8 Kubernetes Client</li>
 *   <li>Преобразование данных Pod'а в DTO ({@link PodInfo})</li>
 * </ul>
 *
 * <p>Конкретные реализации должны переопределять следующие методы:
 * <ul>
 *   <li>{@link #getNameClassExt()} — возвращает имя конкретного делегата для логирования</li>
 *   <li>{@link #getPodName()} — возвращает имя текущего Pod'а по определённой стратегии</li>
 *   <li>{@link #getPod()} — находит и возвращает объект Pod из Kubernetes API</li>
 * </ul>
 *
 * <p>Данный класс служит основой для нескольких стратегий определения Pod'а, реализованных в виде подклассов:
 *
 * <h3>Доступные реализации:</h3>
 * <ul>
 *   <li>{@link CurrentPodServiceConstDownwardApiExt} — получает имя Pod'а через Downward API (переменная окружения).</li>
 *   <li>{@link CurrentPodServiceConstIpPodExt} — ищет Pod по его IP-адресу.</li>
 *   <li>{@link CurrentPodServiceConstNamePodExt} — ищет Pod по жёстко заданному имени.</li>
 *   <li>{@link CurrentPodServiceHostnameInetAddressExt} — получает имя Pod'а через {@link java.net.InetAddress}.</li>
 *   <li>{@link CurrentPodServiceHostnamePathFileExt} — читает имя Pod'а из файла {@code /etc/hostname}.</li>
 *   <li>{@link CurrentPodServiceLabelsExt} — ищет Pod по набору меток (labels).</li>
 * </ul>
 *
 * <p>Каждая реализация использует свою стратегию обнаружения Pod'а, но все они возвращают унифицированный объект {@link PodInfo}.
 *
 * @see PodInfo
 * @see KubernetesClient
 */
@Slf4j
public abstract class CurrentPodService {

    protected final KubernetesClient kubernetesClient;
    protected String podName;
    protected final String namespace;

    public CurrentPodService(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
    }


    /**
     * Основной метод для получения информации о текущем Pod'е.
     *
     * <p>Проверяет наличие разрешений у приложения, при наличии пытается найти под</p>
     *
     * @return объект {@link PodInfo}, содержащий информацию о Pod'е
     * или вернет ошибку ErrorCode.POD_NOT_FOUND
     */
    public PodInfo getCurrentPodInfoWithCheckPermissions(PermissionInfo permissionInfo) {
        log.info("Start getCurrentPodInfoWithCheckPermissions in {}", getNameClassExt());

        if (permissionInfo == null || permissionInfo.getPermissions() == null) {
            log.error("Error start getCurrentPodInfo in {}, forbidden", getNameClassExt());
            throw new KubernetesException(FORBIDDEN);
        }

        List<ResourcePermissionEnum> resourcePermissionList = getPermission();

        log.info("Resource permissions: {}", resourcePermissionList.stream()
                .map(ResourcePermissionEnum::getStringValue)
                .collect(Collectors.joining(", ")));

        log.info("App permissions: {}", permissionInfo.getPermissions().stream()
                .map(p -> p.getResource().getStringValue())
                .collect(Collectors.joining(", ")));

        boolean hasPermission = resourcePermissionList.stream().anyMatch(requiredPerm ->
                permissionInfo.getPermissions().stream()
                        .anyMatch(appPerm -> appPerm.isAllowed() &&
                                appPerm.getResource().getResource().equals(requiredPerm.getResource()) &&
                                appPerm.getResource().getVerb().equals(requiredPerm.getVerb()))
        );

        if (!hasPermission) {
            log.error("Error start getCurrentPodInfo in {}, forbidden", getNameClassExt());
            throw new KubernetesException(FORBIDDEN);
        }

        return getCurrentPodInfo();
    }

    /**
     * Метод для получения информации о текущем Pod'е (без проверки разрешения на это).
     *
     * @return объект {@link PodInfo}, содержащий информацию о Pod'е
     * или вернет ошибку ErrorCode.POD_NOT_FOUND
     */
    public PodInfo getCurrentPodInfo() {
        log.info("Start getCurrentPodInfo in {}", getNameClassExt());

        try {
            podName = getPodName();
            log.info("Possible name of the pod is {}", podName);

            if (podName == null || podName.isBlank()) {
                log.error("Error start getCurrentPodInfo in {}", getNameClassExt());
                log.error(BROKEN_NAME_IN_POD.getMessage());
                throw new KubernetesException(BROKEN_NAME_IN_POD);
            }

            Pod pod = getPod();
            if (pod == null) {
                log.error(POD_NOT_FOUND.getMessage());
                throw new KubernetesException(POD_NOT_FOUND);
            }

            log.info("{}: current pod was found", getNameClassExt());
            return mapToPodInfo(pod);

        } catch (Exception e) {
            log.error(POD_NOT_FOUND.getMessage());
            throw new KubernetesException(POD_NOT_FOUND);
        }
    }


    protected PodInfo mapToPodInfo(Pod pod) {
        log.info("Start mapToPodInfo");
        try {
            return PodInfo.builder()
                    .name(pod.getMetadata().getName())
                    .uid(pod.getMetadata().getUid())
                    .namespace(pod.getMetadata().getNamespace())
                    .labels(pod.getMetadata().getLabels())
                    .phase(pod.getStatus().getPhase())
                    .qosClass(pod.getStatus().getQosClass())
                    .creationTimestamp(pod.getMetadata().getCreationTimestamp())
                    .deletionTimestamp(pod.getMetadata().getDeletionTimestamp() != null ? pod.getMetadata().getDeletionTimestamp() : null)
                    .nodeName(pod.getSpec().getNodeName())
                    .podIP(pod.getStatus().getPodIP())
                    .containers(extractContainers(pod))
                    .build();
        } catch (Exception e) {
            log.error("Error on mapToPodInfo, ", e);
            throw e;
        }
    }

    protected List<ContainerInfo> extractContainers(Pod pod) {
        log.info("Start extractContainers");
        try {
            return pod.getSpec().getContainers().stream()
                    .map(container -> {
                        ContainerStatus status = pod.getStatus().getContainerStatuses().stream()
                                .filter(s -> s.getName().equals(container.getName()))
                                .findFirst()
                                .orElse(null);

                        return ContainerInfo.builder()
                                .name(container.getName())
                                .image(container.getImage())
                                .imageID(status != null ? status.getImageID() : null)
                                .containerID(status != null ? status.getContainerID() : null)
                                .state(status != null ? ContainerStateEnum.parserFromKubernetes(status.getState().toString()) : null)
                                .stateReason(status != null && status.getState().getWaiting() != null ? status.getState().getWaiting().getReason() : null)
                                .restartCount(status != null ? status.getRestartCount() : 0)
                                .lastTerminationReason(status != null && status.getLastState() != null && status.getLastState().getTerminated() != null
                                        ? status.getLastState().getTerminated().getReason() : null)
                                .waitingMessage(status != null && status.getState().getWaiting() != null
                                        ? status.getState().getWaiting().getMessage() : null)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error on extractContainers, ", e);
            throw e;
        }
    }


    protected abstract List<ResourcePermissionEnum> getPermission();

    protected abstract String getNameClassExt();

    protected abstract String getPodName() throws Exception;

    protected abstract Pod getPod() throws Exception;
}