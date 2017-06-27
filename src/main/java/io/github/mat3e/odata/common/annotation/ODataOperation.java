package io.github.mat3e.odata.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * If this annotate a method from ODataEntity class,
 * this method will be exposed as OData Bound Function/Action.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ODataOperation {
    String name();

    /**
     * Actions are called with POST method, while Functions are just with GET.
     * Action should also modify the entity.
     */
    boolean action() default false;
}
