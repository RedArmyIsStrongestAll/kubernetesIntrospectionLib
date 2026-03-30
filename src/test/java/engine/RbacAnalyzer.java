package engine;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

public class RbacAnalyzer {
    private final Set<RbacRule> rules;
    private final String serviceAccountNamespace;

    public RbacAnalyzer(String yamlContent) {
        this.rules = new HashSet<>();
        this.serviceAccountNamespace = extractNamespaceFromYaml(yamlContent);
        parseYamlRules(yamlContent);
    }

    public boolean isAllowed(String resource, String verb, String namespace) {
        if (!serviceAccountNamespace.equals(namespace)) {
            return false;
        }

        return rules.stream().anyMatch(rule ->
                rule.matches(resource, verb, namespace)
        );
    }

    private void parseYamlRules(String yamlContent) {
        LoaderOptions options = new LoaderOptions();

        Yaml yaml = new Yaml(new SafeConstructor(options));
        Iterable<Object> documents = yaml.loadAll(yamlContent);

        StreamSupport.stream(documents.spliterator(), false)
                .filter(doc -> doc instanceof Map)
                .map(doc -> (Map<String, Object>) doc)
                .filter(this::isRoleDocument)
                .forEach(this::extractRulesFromRole);
    }

    private boolean isRoleDocument(Map<String, Object> document) {
        String kind = (String) document.get("kind");
        return "Role".equals(kind) || "ClusterRole".equals(kind);
    }

    private void extractRulesFromRole(Map<String, Object> roleDocument) {
        Map<String, Object> metadata = (Map<String, Object>) roleDocument.get("metadata");
        String roleNamespace = metadata != null ? (String) metadata.get("namespace") : null;
        boolean isClusterRole = "ClusterRole".equals(roleDocument.get("kind"));

        List<Map<String, Object>> rulesList = (List<Map<String, Object>>) roleDocument.get("rules");
        if (rulesList != null) {
            for (Map<String, Object> rule : rulesList) {
                List<String> apiGroups = (List<String>) rule.get("apiGroups");
                List<String> resources = (List<String>) rule.get("resources");
                List<String> verbs = (List<String>) rule.get("verbs");

                if (apiGroups != null && resources != null && verbs != null) {
                    rules.add(new RbacRule(apiGroups, resources, verbs, roleNamespace, isClusterRole));
                }
            }
        }
    }

    private String extractNamespaceFromYaml(String yamlContent) {
        if (yamlContent.contains("namespace:")) {
            int index = yamlContent.indexOf("namespace:");
            if (index != -1) {
                int start = index + "namespace:".length();
                // Пропускаем пробелы
                while (start < yamlContent.length() && Character.isWhitespace(yamlContent.charAt(start))) {
                    start++;
                }
                int end = yamlContent.indexOf("\n", start);
                if (end == -1) end = yamlContent.length();
                // Убираем кавычки если есть
                String namespace = yamlContent.substring(start, end).trim();
                if (namespace.startsWith("\"") && namespace.endsWith("\"") && namespace.length() > 1) {
                    namespace = namespace.substring(1, namespace.length() - 1);
                }
                if (namespace.startsWith("'") && namespace.endsWith("'") && namespace.length() > 1) {
                    namespace = namespace.substring(1, namespace.length() - 1);
                }
                return namespace;
            }
        }
        return "default";
    }

    private static class RbacRule {
        private final List<String> apiGroups;
        private final List<String> resources;
        private final List<String> verbs;
        private final String namespace;
        private final boolean isClusterRole;

        public RbacRule(List<String> apiGroups, List<String> resources, List<String> verbs,
                        String namespace, boolean isClusterRole) {
            this.apiGroups = apiGroups != null ? new ArrayList<>(apiGroups) : new ArrayList<>();
            this.resources = resources != null ? new ArrayList<>(resources) : new ArrayList<>();
            this.verbs = verbs != null ? new ArrayList<>(verbs) : new ArrayList<>();
            this.namespace = namespace;
            this.isClusterRole = isClusterRole;
        }

        public boolean matches(String resource, String verb, String targetNamespace) {
            return matchesResource(resource) &&
                    matchesVerb(verb) &&
                    matchesNamespace(targetNamespace);
        }

        private boolean matchesResource(String resource) {
            return resources.contains("*") || resources.contains(resource);
        }

        private boolean matchesVerb(String verb) {
            return verbs.contains("*") || verbs.contains(verb);
        }

        private boolean matchesNamespace(String targetNamespace) {
            // ClusterRole - доступ ко всем namespaces
            if (isClusterRole) return true;
            // Role - только к своему namespace
            return namespace == null || namespace.equals(targetNamespace);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RbacRule rbacRule = (RbacRule) o;
            return isClusterRole == rbacRule.isClusterRole &&
                    Objects.equals(apiGroups, rbacRule.apiGroups) &&
                    Objects.equals(resources, rbacRule.resources) &&
                    Objects.equals(verbs, rbacRule.verbs) &&
                    Objects.equals(namespace, rbacRule.namespace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiGroups, resources, verbs, namespace, isClusterRole);
        }
    }
}