package usesCases.statics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import kubernetes.introspection.useCases.statics.YamlGenerateService;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class YamlGenerateServiceTest {

    private final YamlGenerateService service = new YamlGenerateService();

    @Test
    void readTestRbacYamlReturnsValidContentTest() throws Exception {
        String resourcePath = "/rbac/template-rbac.yaml";

        // Проверяем, что файл доступен в classpath
        URL resourceUrl = getClass().getResource(resourcePath);
        assertNotNull(resourceUrl, "Файл должен существовать в classpath: " + resourcePath);

        // Чтение содержимого через метод сервиса
        String content = service.readTestRbacYaml();
        assertNotNull(content, "Содержимое файла не должно быть null");
        assertFalse(content.isEmpty(), "Содержимое файла не должно быть пустым");

        // Проверяем кодировку
        assertEquals(content, new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                "Содержимое должно быть корректно закодировано в UTF-8");

        // Проверяем структуру YAML
        assertTrue(content.contains("kind: Role"), "Файл должен содержать 'kind: Role'");
        assertTrue(content.contains("kind: RoleBinding"), "Файл должен содержать 'kind: RoleBinding'");
        assertTrue(content.contains("apiGroups: [ \"\" ]"), "Файл должен содержать 'apiGroups: [ \"\" ]'");
        assertTrue(content.contains("resources: [ \"pods\" ]"), "Файл должен содержать 'resources: [ \"pods\" ]'");

        // Сравниваем с ожидаемым содержимым из ресурса, игнорируя тип перевода строк
        Path expectedFile = Paths.get(resourceUrl.toURI());
        String expectedContent = Files.readString(expectedFile, StandardCharsets.UTF_8).strip();
        String actualContent = content.strip();

        ObjectMapper yamlMapper = new YAMLMapper();
        Object expectedObj = yamlMapper.readValue(expectedContent, Object.class);
        Object actualObj = yamlMapper.readValue(actualContent, Object.class);

        String normalizedExpected = yamlMapper.writeValueAsString(expectedObj);
        String normalizedActual = yamlMapper.writeValueAsString(actualObj);

        assertEquals(normalizedExpected, normalizedActual, "Содержимое файла должно точно совпадать с ожидаемым");

        //todo-ilia проверить число тестов и кожд ревью сделать по изменения
    }
}
