package io.github.mat3e.odata.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

/**
 * Entity property.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ODataProperty {
    String name();

    EdmPrimitiveTypeKind type();

    ValueType valueType() default ValueType.PRIMITIVE;
}
