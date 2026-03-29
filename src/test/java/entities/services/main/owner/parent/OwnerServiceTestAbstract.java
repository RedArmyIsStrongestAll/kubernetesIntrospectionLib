package entities.services.main.owner.parent;

import engine.RbacAnalyzer;
import engine.owner.OwnerAnalyzer;
import engine.owner.OwnerAnalyzerDeployment;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import kubernetes.introspection.entities.models.dto.owner.OwnerTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static entities.services.utils.TestUtils.loadRbacYaml;

@Slf4j
public class OwnerServiceTestAbstract {
    protected KubernetesMockServer mockServer;
    protected KubernetesClient client;

    protected static final String NAMESPACE = "test-namespace";

    protected OwnerAnalyzer getOwnerAnalyzer(String rbacFilename, String ownerFilename) throws IOException {
        String rbacContent = loadRbacYaml(rbacFilename);
        RbacAnalyzer rbacAnalyzer = new RbacAnalyzer(rbacContent);

        String yamlContent = loadRbacYaml(ownerFilename);
        return new OwnerAnalyzerDeployment(yamlContent, rbacAnalyzer);
    }

    protected OwnerReference getOwnerReference(String ownerName, OwnerTypeEnum typeEnum) {
        OwnerReference ownerRef = new OwnerReference();
        ownerRef.setKind(typeEnum.getOriginalName());
        ownerRef.setName(ownerName);
        return ownerRef;
    }

    protected void setupMockServerWithValidOwner(OwnerAnalyzer analyzer, String ownerName) {
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/" + NAMESPACE + "/deployments/" + ownerName)
                .andReply(200, request -> {
                            log.info("Received GET request for owner: {}/{}", NAMESPACE, ownerName);
                            return analyzer.getOwner(ownerName, NAMESPACE);
                        }
                )
                .always();
    }

    protected void setupMockServerWithError() {
//        mockServer.expect().get()
//                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + POD_NAME)
//                .andReply(500, request -> "Internal Server Error")
//                .always();
    }

}
