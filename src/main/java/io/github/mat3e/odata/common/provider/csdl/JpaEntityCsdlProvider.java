package io.github.mat3e.odata.common.provider.csdl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import io.github.mat3e.odata.common.annotation.ODataEntity;
import io.github.mat3e.odata.common.annotation.ODataKey;
import io.github.mat3e.odata.common.annotation.ODataNavigationProperty;
import io.github.mat3e.odata.common.annotation.ODataProperty;
import io.github.mat3e.odata.common.entity.JpaOlingoEntity;
import io.github.mat3e.odata.common.entity.JpaOlingoMediaEntity;
import io.github.mat3e.odata.common.exception.CsdlExtractException;
import io.github.mat3e.odata.common.util.FullQualifiedNamesUtil;
import io.github.mat3e.odata.common.util.ReflectionUtil;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Class which describes an entity in the way understandable for Provider.
 * Operates on Java class in order to get its declared OData annotations.
 */
public class JpaEntityCsdlProvider<T extends JpaOlingoEntity> extends JavaObjectCsdlProvider<T> {
    private CsdlEntitySet eSet;
    private CsdlEntityType eType;
    private FullQualifiedName fqn;

    private ODataEntity entityAnnotation;

    public JpaEntityCsdlProvider(Class<T> clazz) throws CsdlExtractException {
        super(clazz);

        this.entityAnnotation = clazz.getAnnotation(ODataEntity.class);
        if (this.entityAnnotation == null) {
            throw new CsdlExtractException("Entity must be annotated as ODataEntity to build its CSDL representation");
        }

        init();
    }

    @Override
    public CsdlEntitySet getCsdlEntitySet() {
        return this.eSet;
    }

    @Override
    public CsdlEntityType getCsdlEntityType() {
        return this.eType;
    }

    @Override
    public FullQualifiedName getFQN() {
        return fqn;
    }

    private void init() throws CsdlExtractException {
        this.fqn = FullQualifiedNamesUtil.createFullQualifiedEntityName(entityAnnotation.name());

        this.eType = new CsdlEntityType().setName(entityAnnotation.name())
                                         .setHasStream(JpaOlingoMediaEntity.class.isAssignableFrom(getClass()));

        List<CsdlPropertyRef> keys = new ArrayList<>();
        List<CsdlProperty> properties = new ArrayList<>();
        List<CsdlNavigationProperty> navProps = new ArrayList<>();
        List<CsdlNavigationPropertyBinding> navBinds = new ArrayList<>();

        Field[] fields = ReflectionUtil.getFieldsUpToJpaOlingoEntity(clazz);
        for (Field f : fields) {
            if (f.isAnnotationPresent(ODataKey.class)) {
                keys.add(extractKey(f));
            }
            if (f.isAnnotationPresent(ODataProperty.class)) {
                properties.add(extractProperty(f));
            } else if (f.isAnnotationPresent(ODataNavigationProperty.class)) { // TODO: exception when both annotations
                CsdlNav nav = extractNavigation(f);
                navProps.add(nav.navigationProp);
                navBinds.add(nav.navigationBinding);
            }
        }

        this.eType.setProperties(properties).setNavigationProperties(navProps).setKey(keys);
        this.eSet = new CsdlEntitySet().setName(entityAnnotation.entitySetName()).setType(getFQN())
                                       .setNavigationPropertyBindings(navBinds).setIncludeInServiceDocument(true);
    }

    private CsdlPropertyRef extractKey(Field f) throws CsdlExtractException {
        ODataProperty prop = f.getAnnotation(ODataProperty.class);
        if (prop == null) {
            throw new CsdlExtractException("Field annotated as ODataKey must be annotated as ODataProperty as well");
        }
        return new CsdlPropertyRef().setName(prop.name());
    }

    private CsdlProperty extractProperty(Field f) {
        ODataProperty odataAnn = f.getAnnotation(ODataProperty.class);
        CsdlProperty result = new CsdlProperty().setName(odataAnn.name())
                                                .setType(odataAnn.type().getFullQualifiedName())
                                                .setCollection(ReflectionUtil.isArrayOrCollection(f))
                                                .setNullable(!f.isAnnotationPresent(NotNull.class));
        if (f.isAnnotationPresent(Size.class)) {
            result.setMaxLength(f.getAnnotation(Size.class).max());
        }

        return result;
    }

    private CsdlNav extractNavigation(Field f) throws CsdlExtractException {
        String navigationName = f.getAnnotation(ODataNavigationProperty.class).name();
        Class<?> type = ReflectionUtil.extractType(f);

        return new CsdlNav(type, navigationName, ReflectionUtil.isArrayOrCollection(f));
    }

    private class CsdlNav {
        CsdlNavigationProperty navigationProp;
        CsdlNavigationPropertyBinding navigationBinding;

        CsdlNav(Class<?> typeOfTarget, String sourceProp, boolean isCollection) throws CsdlExtractException {
            ODataEntity targetEntity = typeOfTarget.getAnnotation(ODataEntity.class);
            if (targetEntity == null) {
                throw new CsdlExtractException("Type " + typeOfTarget.getName() + " must be annotated as ODataEntity");
            }

            navigationBinding = new CsdlNavigationPropertyBinding().setTarget(targetEntity.entitySetName())
                                                                   .setPath(sourceProp);
            navigationProp = new CsdlNavigationProperty().setName(sourceProp).setType(
                    FullQualifiedNamesUtil.createFullQualifiedEntityName(targetEntity.name()))
                                                         .setCollection(isCollection);
        }
    }
}