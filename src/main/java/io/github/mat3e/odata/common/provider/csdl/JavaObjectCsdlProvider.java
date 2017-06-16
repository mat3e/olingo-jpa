package io.github.mat3e.odata.common.provider.csdl;

import io.github.mat3e.odata.common.exception.CsdlExtractException;

/**
 * Provider, which is connected with a Java class, e.g. with JPA Entity.
 */
public abstract class JavaObjectCsdlProvider<T> implements CsdlProvider {
    protected Class<T> clazz;

    public JavaObjectCsdlProvider(Class<T> clazz) throws CsdlExtractException {
        this.clazz = clazz;
    }

    public Class<T> getBackingClass() {
        return clazz;
    }
}
