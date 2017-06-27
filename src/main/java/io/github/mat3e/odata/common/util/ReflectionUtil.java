package io.github.mat3e.odata.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.stream.Stream;

import io.github.mat3e.odata.common.entity.JpaOlingoEntity;

/**
 * Helper class for extracting annotations from Olingo Entities.
 */
public class ReflectionUtil {

    /**
     * Checks field class conditions to answer if it contains multiple elements.
     *
     * @param f
     *         field to be checked
     * @return true if the array or iterable
     */
    public static boolean isArrayOrCollection(Field f) {
        return isArrayOrCollection(f.getType());
    }

    /**
     * Checks class conditions to answer if it contains multiple elements.
     *
     * @param fieldClass
     *         class to be checked
     * @return true if the array or iterable
     */
    public static boolean isArrayOrCollection(Class<?> fieldClass) {
        return fieldClass.isArray() || Iterable.class.isAssignableFrom(fieldClass);
    }

    /**
     * Checks the field and returns either its type or component type for arrays or type parameter for collections.
     *
     * @param f
     *         field to be checked
     * @return type or component type or type parameter, depending on the field
     */
    public static Class<?> extractType(Field f) {
        Class<?> fieldClass = f.getType();
        if (isArrayOrCollection(fieldClass)) {
            fieldClass = fieldClass.getComponentType() == null ?
                    (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0] :
                    fieldClass.getComponentType();
        }
        return fieldClass;
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

    public static Method[] getMethodsUpToJpaOdataEntity(Class<?> firstClass) {
        Stream<Method> result = Stream.of(firstClass.getDeclaredMethods());
        Class<?> superclass = firstClass.getSuperclass();
        if (!isLastClass(superclass)) {
            result = Stream.concat(result, Stream.of(getMethodsUpToJpaOdataEntity(superclass)));
        }
        return result.toArray(Method[]::new);
    }

    private static boolean isLastClass(Class<?> clazz) {
        return clazz.equals(JpaOlingoEntity.class);
    }
}
