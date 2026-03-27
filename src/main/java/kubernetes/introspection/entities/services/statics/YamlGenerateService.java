package kubernetes.introspection.entities.services.statics;

import kubernetes.introspection.entities.models.exceptions.KubernetesException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static kubernetes.introspection.entities.models.exceptions.ErrorCode.ERROR_READ_FILE_RBAC;
import static kubernetes.introspection.entities.models.exceptions.ErrorCode.NO_STATIC_FILE_RBAC;

/**
 * Возвращает шаблон YAML файл для RBAC
 */
@Slf4j
public class YamlGenerateService {

    /**
     * Возвращает шаблон YAML файл для RBAC
     */
    public String readTestRbacYaml() {
        log.info("Start readTestRbacYaml");

        try (InputStream is = getClass().getResourceAsStream("/rbac/test-rbac.yaml")) {
            if (is == null) {
                log.error(NO_STATIC_FILE_RBAC.getMessage());
                throw new KubernetesException(NO_STATIC_FILE_RBAC);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.error(ERROR_READ_FILE_RBAC.getMessage(), e);
            throw new KubernetesException(ERROR_READ_FILE_RBAC);
        }
    }
}
