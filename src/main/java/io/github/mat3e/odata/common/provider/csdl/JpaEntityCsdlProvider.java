package io.github.mat3e.odata.common.provider.csdl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlOperation;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import io.github.mat3e.odata.common.annotation.ODataEntity;
import io.github.mat3e.odata.common.annotation.ODataKey;
import io.github.mat3e.odata.common.annotation.ODataNavigationProperty;
import io.github.mat3e.odata.common.annotation.ODataOperation;
import io.github.mat3e.odata.common.annotation.ODataOperationParameter;
import io.github.mat3e.odata.common.annotation.ODataProperty;
import io.github.mat3e.odata.common.entity.JpaOlingoEntity;
import io.github.mat3e.odata.common.entity.JpaOlingoMediaEntity;
import io.github.mat3e.odata.common.exception.CsdlExtractException;
import io.github.mat3e.odata.common.util.EdmTypeUtil;
import io.github.mat3e.odata.common.util.FullQualifiedNamesUtil;
import io.github.mat3e.odata.common.util.ReflectionUtil;

/**
 * Class which describes an entity in the way understandable for Provider.
 * Operates on Java class in order to get its declared OData annotations.
 */
public class JpaEntityCsdlProvider<T extends JpaOlingoEntity> extends JavaObjectCsdlProvider<T> {
    private CsdlEntitySet eSet;
    private CsdlEntityType eType;
    private FullQualifiedName fqn;

    private ODataEntity entityAnnotation;

    // operations bound to the entity
    private List<CsdlAction> actions = new ArrayList<>();
    private List<CsdlFunction> functions = new ArrayList<>();

    private Map<String, String> ODataToJavaProperties = new HashMap<>();

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

    /**
     * Methods from the entity.
     */
    @Override
    public List<CsdlAction> getCsdlActions() {
        return this.actions;
    }

    /**
     * Read-only methods from the entity.
     */
    @Override
    public List<CsdlFunction> getCsdlFunctions() {
        return this.functions;
    }

    /**
     * Gets Java property name for a given OData property name. Works for Java properties which were the sources of
     * OData properties.
     *
     * @param ODataProperty
     *         name of the OData property
     * @return name of Java property, which was the source of OData property
     */
    public String getJavaPropertyForODataProperty(String ODataProperty) {
        return ODataToJavaProperties.get(ODataProperty);
    }

    /**
     * Entity shouldn't define enums inside.
     */
    @Override
    public CsdlEnumType getCsdlEnumType() {
        return null;
    }

    /**
     * Entity shouldn't define classes inside.
     */
    @Override
    public CsdlComplexType getCsdlComplexType() {
        return null;
    }

    private void init() throws CsdlExtractException {
        this.fqn = FullQualifiedNamesUtil.createFullQualifiedEntityName(entityAnnotation.name());

        this.eType = new CsdlEntityType().setName(entityAnnotation.name())
                                         .setHasStream(JpaOlingoMediaEntity.class.isAssignableFrom(clazz));

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

        Method[] methods = ReflectionUtil.getMethodsUpToJpaOdataEntity(clazz);
        for (Method method : methods) {
            ODataOperation funcAnnotation = method.getAnnotation(ODataOperation.class);
            if (funcAnnotation != null) {
                if (funcAnnotation.action()) {
                    actions.add(extractAction(method));
                } else {
                    functions.add(extractFunction(method));
                }
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
        ODataProperty odataPropAnn = f.getAnnotation(ODataProperty.class);
        String odataName = odataPropAnn.name();
        CsdlProperty result = new CsdlProperty().setName(odataName).setType(odataPropAnn.type().getFullQualifiedName())
                                                .setCollection(ReflectionUtil.isArrayOrCollection(f))
                                                .setNullable(!f.isAnnotationPresent(NotNull.class));
        if (f.isAnnotationPresent(Size.class)) {
            result.setMaxLength(f.getAnnotation(Size.class).max());
        }

        ODataToJavaProperties.put(odataName, f.getName());

        return result;
    }

    private CsdlNav extractNavigation(Field f) throws CsdlExtractException {
        String navigationName = f.getAnnotation(ODataNavigationProperty.class).name();
        Class<?> type = ReflectionUtil.extractType(f);

        ODataToJavaProperties.put(navigationName, f.getName());

        return new CsdlNav(type, navigationName, ReflectionUtil.isArrayOrCollection(f));
    }

    private CsdlAction extractAction(Method method) throws CsdlExtractException {
        return (CsdlAction) handleOperation(method, new CsdlAction());
    }

    private CsdlFunction extractFunction(Method method) throws CsdlExtractException {
        return (CsdlFunction) handleOperation(method, new CsdlFunction());
    }

    private CsdlOperation handleOperation(Method method, CsdlOperation operation) throws CsdlExtractException {
        ODataOperation methodAnnotation = method.getAnnotation(ODataOperation.class);
        operation.setName(methodAnnotation.name()).setBound(true);

        List<CsdlParameter> parameters = new ArrayList<>();
        parameters.add(new CsdlParameter().setNullable(false).setType(getFQN()).setName(BINDING_PARAM_NAME));

        Class<?>[] clazzes = method.getParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < clazzes.length; ++i) {
            Class<?> clazz = clazzes[i];
            // TODO: more than primitive types
            CsdlParameter csdlParameter = new CsdlParameter().setNullable(false).setType(
                    EdmTypeUtil.getEdmPrimitiveTypeFor(clazz).getFullQualifiedName())
                                                             .setCollection(ReflectionUtil.isArrayOrCollection(clazz));

            for (Annotation annotation : paramAnnotations[i]) {
                if ((annotation instanceof ODataOperationParameter)) {
                    ODataOperationParameter paramAnnotation = (ODataOperationParameter) annotation;
                    csdlParameter.setName(paramAnnotation.name());
                    break;
                }
            }

            parameters.add(csdlParameter);
        }

        operation.setParameters(parameters);

        Class<?> returnTypeClass = method.getReturnType();
        if (!returnTypeClass.equals(Void.TYPE)) {
            CsdlReturnType returnType = new CsdlReturnType()
                    .setCollection(ReflectionUtil.isArrayOrCollection(returnTypeClass))
                    .setType(EdmTypeUtil.getEdmPrimitiveTypeFor(returnTypeClass).getFullQualifiedName());

            operation.setReturnType(returnType);
        }

        return operation;
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