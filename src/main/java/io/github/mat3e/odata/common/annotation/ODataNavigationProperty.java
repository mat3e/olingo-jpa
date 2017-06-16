package io.github.mat3e.odata.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for properties which navigate to other entities.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ODataNavigationProperty {
    String name();
}
