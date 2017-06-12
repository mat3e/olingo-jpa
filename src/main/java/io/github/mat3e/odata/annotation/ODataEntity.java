package io.github.mat3e.odata.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Main annotation for marking as OData Entity.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ODataEntity {
    /**
     * We don't need to expose all the entities in sets.
     * This is a default entitySetName, which indicates no set for
     * annotated entity.
     */
    String NO_SET = "[none]";

    String name();

    String entitySetName() default NO_SET;

    // openType, abstract, baseType
}