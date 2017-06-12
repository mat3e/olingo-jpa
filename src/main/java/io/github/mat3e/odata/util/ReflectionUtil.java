package io.github.mat3e.odata.util;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import io.github.mat3e.odata.entity.JpaOlingoEntity;

/**
 * Helper class for extracting annotations from Olingo Entities.
 */
public class ReflectionUtil {

    /**
     * Checks class conditions to answer if it contains multiple elements.
     *
     * @param f field to be checked
     * @return true if the array of iterable
     */
    public static boolean isArrayOrCollection(Field f) {
        Class<?> fieldClass = f.getType();
        return fieldClass.isArray() || Iterable.class.isAssignableFrom(fieldClass);
    }

    /**
     * Extracts fields from the whole hierarchy.
     *
     * @param firstClass
     *         first class in the hierarchy
     * @return array of extracted fields
     */
    public static Field[] getFieldsUpToJpaOlingoEntity(Class<?> firstClass) {
        Stream<Field> result = Stream.of(firstClass.getDeclaredFields());
        Class<?> superclass = firstClass.getSuperclass();
        if (!isLastClass(superclass)) {
            result = Stream.concat(result, Stream.of(getFieldsUpToJpaOlingoEntity(superclass)));
        }
        return result.toArray(Field[]::new);
    }

    private static boolean isLastClass(Class<?> clazz) {
        return clazz.equals(JpaOlingoEntity.class);
    }
}
