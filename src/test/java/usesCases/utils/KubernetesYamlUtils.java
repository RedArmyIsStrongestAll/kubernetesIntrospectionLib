package usesCases.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KubernetesYamlUtils {
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static String loadRbacYaml(String filename) throws IOException {
        URL resource = KubernetesYamlUtils.class.getClassLoader().getResource(filename);
        if (resource != null) {
            File file = new File(resource.getFile());
            if (file.exists()) {
                return new String(Files.readAllBytes(file.toPath()));
            }
        }
        throw new IOException("No " + filename + " file found");
    }

    public static <T> T trySetYamlObject(String yamlObjects, Class<T> classObject) throws IOException {
        T object = null;
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

    public static <T> List<T> trySetYamlObjectList(String yamlContent, Class<T> classObject, String k8sKindNameOrig) throws IOException {
        List<T> objectList = new ArrayList<>();
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        for (Object doc : yaml.loadAll(yamlContent)) {
            try {
                if (!(doc instanceof Map)) continue;
                Map<String, Object> map = (Map<String, Object>) doc;
                if (k8sKindNameOrig.equals(map.get("kind"))) {
                    T object = objectMapper.convertValue(doc, classObject);
                    objectList.add(object);
                }
            } catch (Exception ignored) {
            }
        }
        if (objectList.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid YAML: %s for type %s", yamlContent, classObject));
        }
        return objectList;
    }

    public static String buildLabelSelectorQuery(Map<String, String> labels) {
        return labels.entrySet().stream()
                .map(entry -> {
                    String key = java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                    String value = java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    return key + "%3D" + value;
                })
                .collect(Collectors.joining("%2C")); // , → %2C
    }

    public static String buildFieldSelectorQuery(String key, String value) {
        String encodedKey = java.net.URLEncoder.encode(key, StandardCharsets.UTF_8);
        String encodedValue = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encodedKey + "%3D" + encodedValue;
    }
}
