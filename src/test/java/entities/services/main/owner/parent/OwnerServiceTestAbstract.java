package entities.services.main.owner.parent;

import engine.RbacAnalyzer;
import engine.owner.OwnerAnalyzer;
import engine.owner.OwnerAnalyzerCronJob;
import engine.owner.OwnerAnalyzerDaemonSet;
import engine.owner.OwnerAnalyzerDeployment;
import engine.owner.OwnerAnalyzerJob;
import engine.owner.OwnerAnalyzerReplicaSet;
import engine.owner.OwnerAnalyzerReplicationController;
import engine.owner.OwnerAnalyzerStatefulSet;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;

import static entities.services.utils.KubernetesYamlUtils.loadRbacYaml;

@Slf4j
public class OwnerServiceTestAbstract {
    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;

    protected static final String NAMESPACE = "test-namespace";

    protected OwnerAnalyzer getOwnerAnalyzer(String rbacFilename, String ownerFilename, Class<? extends OwnerAnalyzer> analyzerClass) throws Exception {
        String rbacContent = loadRbacYaml(rbacFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacContent);

        String yamlContent = loadRbacYaml(ownerFilename);

        Constructor<? extends OwnerAnalyzer> constructor = analyzerClass.getConstructor(String.class, RbacAnalyzer.class);
        return constructor.newInstance(yamlContent, rbacAnalyzer);
    }

    protected OwnerReference getOwnerReference(String ownerName, OwnerTypeEnum typeEnum) {
        OwnerReference ownerRef = new OwnerReference();
        ownerRef.setKind(typeEnum.getOriginalName());
        ownerRef.setName(ownerName);
        return ownerRef;
    }

    protected void setupMockServerWithValidOwnerDeployment(OwnerAnalyzerDeployment analyzer, String ownerName) {
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/deployments/" + ownerName)
                .andReply(200, request -> {
                            log.info("Received GET request for owner: {}/{}", NAMESPACE, ownerName);
                            return analyzer.getOwner(ownerName, NAMESPACE);
                        }
                )
                .always();
    }

    protected void setupMockServerWithValidCronJob(OwnerAnalyzerCronJob analyzer, String cronJobName) {
        mockServer.expect().get()
                .withPath("/apis/batch/v1/namespaces/" + NAMESPACE + "/cronjobs/" + cronJobName)
                .andReply(200, request -> {
                    log.info("Received GET request for CronJob: {}/{}", NAMESPACE, cronJobName);
                    return analyzer.getOwner(cronJobName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithValidDaemonSet(OwnerAnalyzerDaemonSet analyzer, String daemonsetName) {
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/daemonsets/" + daemonsetName)
                .andReply(200, request -> {
                    log.info("Received GET request for DaemonSet: {}/{}", NAMESPACE, daemonsetName);
                    return analyzer.getOwner(daemonsetName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithValidJob(OwnerAnalyzerJob analyzer, String jobName) {
        mockServer.expect().get()
                .withPath("/apis/batch/v1/namespaces/" + NAMESPACE + "/jobs/" + jobName)
                .andReply(200, request -> {
                    log.info("Received GET request for Job: {}/{}", NAMESPACE, jobName);
                    return analyzer.getOwner(jobName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithValidReplicaSet(OwnerAnalyzerReplicaSet analyzer, String replicaSetName) {
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/replicasets/" + replicaSetName)
                .andReply(200, request -> {
                    log.info("Received GET request for ReplicaSet: {}/{}", NAMESPACE, replicaSetName);
                    return analyzer.getOwner(replicaSetName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithValidReplicationController(OwnerAnalyzerReplicationController analyzer, String rcName) {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/replicationcontrollers/" + rcName)
                .andReply(200, request -> {
                    log.info("Received GET request for ReplicationController: {}/{}", NAMESPACE, rcName);
                    return analyzer.getOwner(rcName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithValidStatefulSet(OwnerAnalyzerStatefulSet analyzer, String statefulSetName) {
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/statefulsets/" + statefulSetName)
                .andReply(200, request -> {
                    log.info("Received GET request for StatefulSet: {}/{}", NAMESPACE, statefulSetName);
                    return analyzer.getOwner(statefulSetName, NAMESPACE);
                })
                .always();
    }

    protected void setupMockServerWithError() {
    }

}
