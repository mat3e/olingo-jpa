package io.github.mat3e.odata.common.util;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

/**
 * Helper class to return full qualified names of types, actions, annotations
 */
public class FullQualifiedNamesUtil {
    private static final String NAMESPACE_BASE = propertyOrDefault("namespace", "Schema");

    public static final class NAMESPACE {
        public static final String ENUMS = subnamespace(propertyOrDefault("namespace.enums", "Enums"));
        public static final String ENTITIES = subnamespace(propertyOrDefault("namespace.entities", "Entities"));
        public static final String ACTIONS = subnamespace(propertyOrDefault("namespace.actions", "Actions"));
        public static final String FUNCTIONS = subnamespace(propertyOrDefault("namespace.functions", "Functions"));
        public static final String COMPLEX_TYPES = subnamespace(
                propertyOrDefault("namespace.complextypes", "ComplexTypes"));
    }

    public static final String CONTAINER = propertyOrDefault("container", "Service");
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE.ENTITIES, CONTAINER);

    public static FullQualifiedName createFullQualifiedEnumName(String name) {
        return new FullQualifiedName(NAMESPACE.ENUMS, name);
    }

    public static FullQualifiedName createFullQualifiedEntityName(String name) {
        return new FullQualifiedName(NAMESPACE.ENTITIES, name);
    }

    public static FullQualifiedName createFullQualifiedActionName(String name) {
        return new FullQualifiedName(NAMESPACE.ACTIONS, name);
    }

    public static FullQualifiedName createFullQualifiedFunctionName(String name) {
        return new FullQualifiedName(NAMESPACE.FUNCTIONS, name);
    }

    public static FullQualifiedName createFullQualifiedComplexTypeName(String name) {
        return new FullQualifiedName(NAMESPACE.COMPLEX_TYPES, name);
    }

    private static String propertyOrDefault(String property, String defaultValue) {
        String prop = System.getProperty(property);
        return prop == null ? defaultValue : prop;
    }

    private static String subnamespace(String sub) {
        return NAMESPACE_BASE + "." + sub;
    }
}
