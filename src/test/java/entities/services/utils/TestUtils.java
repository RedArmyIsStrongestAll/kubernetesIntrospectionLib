package entities.services.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class TestUtils {
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static String loadRbacYaml(String filename) throws IOException {
        URL resource = TestUtils.class.getClassLoader().getResource(filename);
        if (resource != null) {
            File file = new File(resource.getFile());
            if (file.exists()) {
                return new String(Files.readAllBytes(file.toPath()));
            }
        }
        throw new IOException("No " + filename + " file found");
    }

    public static Object changeSetYamlObject(String yamlObjects, Class<?> classObject) throws IOException {
        Object object = null;
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        for (Object doc : yaml.loadAll(yamlObjects)) {
            try {
                object = objectMapper.convertValue(doc, classObject);
            } catch (Exception ignored) {
            }
        }
        if (object == null) {
            throw new IllegalArgumentException(String.format("Invalid YAML: {} to {}", yamlObjects, classObject));
        }
        return object;
    }
}
