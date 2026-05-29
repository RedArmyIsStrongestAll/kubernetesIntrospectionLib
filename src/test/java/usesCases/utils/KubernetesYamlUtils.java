package usesCases.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KubernetesYamlUtils {
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static String loadRbacYaml(String filename) throws IOException {
        URL resource = KubernetesYamlUtils.class.getClassLoader().getResource(filename);
        if (resource != null) {
            try (java.io.InputStream in = resource.openStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("No " + filename + " file found");
    }

    public static <T> T trySetYamlObject(String yamlObjects, Class<T> classObject) throws IOException {
        return trySetYamlObject(yamlObjects, classObject, null);
    }

    /**
     * Извлекает первый документ заданного kind из multi-document YAML.
     * Без kind-фильтрации Jackson без ошибок конвертирует любой документ в целевой класс
     * (заполняя metadata чужим именем), поэтому фильтрация по kind обязательна.
     */
    public static <T> T trySetYamlObject(String yamlObjects, Class<T> classObject, String kind) throws IOException {
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        for (Object doc : yaml.loadAll(yamlObjects)) {
            try {
                if (!(doc instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) doc;
                if (kind != null && !kind.equals(map.get("kind"))) continue;
                T object = objectMapper.convertValue(doc, classObject);
                if (object != null) return object;
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException(String.format("No YAML document of kind '%s' found for type %s", kind, classObject));
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
