package entities.services.main.pod;

import engine.PodAnalyzer;
import entities.services.main.pod.parent.CurrentPodServiceTestAbstract;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.permision.PermissionInfo;
import kubernetes.introspection.entities.models.dto.permision.ResourcePermissionEnum;
import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import kubernetes.introspection.entities.services.main.pod.CurrentPodService;
import kubernetes.introspection.entities.services.main.pod.delegate.CurrentPodServiceLabelsExt;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

@Slf4j
class CurrentPodServiceTest extends CurrentPodServiceTestAbstract {

  //todo написать тесты на метод getPodInfo
}