package io.github.mat3e.odata.common.entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mat3e.odata.common.annotation.ODataEntity;
import io.github.mat3e.odata.common.annotation.ODataKey;
import io.github.mat3e.odata.common.annotation.ODataNavigationProperty;
import io.github.mat3e.odata.common.annotation.ODataProperty;
import io.github.mat3e.odata.common.util.ReflectionUtil;

/**
 * Base for all OData Entities.
 * Subclasses has to be POJOs.
 */
@MappedSuperclass
public abstract class JpaOlingoEntity extends Entity {
    @Transient
    private static final Logger LOG = LoggerFactory.getLogger(JpaOlingoEntity.class);

    @Transient
    private Proxy proxy = new Proxy();

    /**
     * URI is something like id of the OData entity and it is used in many places,
     * e.g. Navigation Properties, Location Header.
     * If the entity has a set assigned, it should be used inside URI id.
     *
     * @return URI for accessing the entity or null if no set assigned.
     */
    @Override
    public URI getId() {
        if (proxy.set == null) {
            proxy.set = getClass().getAnnotation(ODataEntity.class).entitySetName();
        }

        if (proxy.id == null && !proxy.set.equals(ODataEntity.NO_SET)) {
            try {
                StringBuilder uriString = new StringBuilder(proxy.set).append("(");
                Field[] keys = Stream.of(getAccessibleFields()).filter(f -> f.isAnnotationPresent(ODataKey.class))
                                     .toArray(Field[]::new);
                if (keys.length == 1) {
                    String key = keys[0].get(this).toString();
                    key = parseKeyValue(key, keys[0].getAnnotation(ODataProperty.class).type());
                    uriString.append(key);
                } else {
                    // for many keys either their order is important or they can be used as name=value pairs
                    for (Field key : keys) {
                        ODataProperty propertyAnn = key.getAnnotation(ODataProperty.class);
                        uriString.append(propertyAnn.name()).append("=")
                                 .append(parseKeyValue(key.get(this).toString(), propertyAnn.type())).append(",");
                    }
                    uriString.deleteCharAt(uriString.length() - 1);
                }
                proxy.id = new URI(uriString.append(")").toString());
            } catch (IllegalArgumentException | IllegalAccessException | URISyntaxException e) {
                LOG.error("Can't find Entity Key", e);
            }
        }
        return proxy.id;
    }

    @Override
    public Property getProperty(String name) {
        for (Property prop : getProperties()) {
            if (name.equals(prop.getName())) {
                return prop;
            }
        }
        return null;
    }

    @Override
    public List<Property> getProperties() {
        if (proxy.properties == null) {
            proxy.properties = new ArrayList<>();
            proxy.propertyAccessors = new HashMap<>();
            for (Field f : getAccessibleFields()) {
                ODataProperty annotation = f.getAnnotation(ODataProperty.class);
                if (annotation != null) {
                    String name = annotation.name();
                    RealAccessors<JpaOlingoEntity> accessors = new RealAccessors<>(this, f);
                    proxy.properties.add(new Property(null, name, annotation.valueType(), accessors.get(this)));
                    proxy.propertyAccessors.put(name, accessors);
                }
            }
        }
        return proxy.properties;
    }

    @Override
    public Link getNavigationLink(String name) {
        for (Link link : getNavigationLinks()) {
            if (name.equals(link.getTitle())) {
                return link;
            }
        }
        return null;
    }

    @Override
    public List<Link> getNavigationLinks() {
        if (proxy.links == null) {
            proxy.links = new ArrayList<>();
            proxy.linkAccessors = new HashMap<>();
            for (Field f : getAccessibleFields()) {
                ODataNavigationProperty annotation = f.getAnnotation(ODataNavigationProperty.class);
                if (annotation != null) {
                    Link link = new Link();

                    RealAccessors<JpaOlingoEntity> accessors = new RealAccessors<>(this, f);
                    Object value = accessors.get(this);

                    String name = annotation.name();
                    link.setTitle(name);
                    link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
                    link.setRel(Constants.NS_NAVIGATION_LINK_REL + name);

                    if (ReflectionUtil.isArrayOrCollection(f)) {
                        EntityCollection entityCollection = new EntityCollection();
                        if (value.getClass().isArray()) {
                            entityCollection.getEntities().addAll(Arrays.asList((Entity[]) value));
                        } else {
                            entityCollection.getEntities().addAll((Collection<Entity>) value);
                        }
                        link.setInlineEntitySet(entityCollection);
                    } else {
                        link.setInlineEntity((Entity) value);
                    }

                    proxy.links.add(link);
                    proxy.linkAccessors.put(name, accessors);
                }
            }
        }
        return proxy.links;
    }

    /**
     * Method for reading the refreshed version of the entity.
     * It flushes cache.
     */
    public Entity refreshAndGet() {
        proxy.id = null;
        proxy.links = null;
        proxy.fields = null;

        getId();
        getProperties();
        getNavigationLinks();

        return this;
    }

    /**
     * Method for updating just sent fields (those in the entity).
     */
    public void patch(Entity entity) {
        setFromEntity(entity, false);
    }

    /**
     * Method for updating and setting fields which were not sent to null.
     */
    public void put(Entity entity) {
        setFromEntity(entity, true);
    }

    // TODO: navigation links support
    protected void setFromEntity(Entity entity, boolean overrideWithNull) {
        List<Property> sourceProperties = entity.getProperties();

        if (proxy.properties == null || proxy.propertyAccessors == null) {
            getProperties();
        }

        if (overrideWithNull) {
            proxy.properties.forEach(prop -> {
                if (!sourceProperties.contains(prop)) {
                    proxy.propertyAccessors.get(prop.getName()).set(this, null);
                }
            });
        }

        sourceProperties.forEach(prop -> proxy.propertyAccessors.get(prop.getName()).set(this, prop.getValue()));

        // we need new values in the cache after the next call
        proxy.properties = null;
    }

    private Field[] getAccessibleFields() {
        if (proxy.fields == null) {
            proxy.fields = ReflectionUtil.getFieldsUpToJpaOlingoEntity(getClass());
            Stream.of(proxy.fields).forEach(f -> f.setAccessible(true));
        }
        return proxy.fields;
    }

    private String parseKeyValue(String key, EdmPrimitiveTypeKind type) {
        if (type == EdmPrimitiveTypeKind.String) {
            key = "'" + key + "'";
        }

        return key;
    }

    /**
     * Proxy to skip iterating through annotations each time some method is called.
     */
    private static class Proxy {
        /**
         * URI, ended usually with Set(id).
         */
        URI id;

        /**
         * Name of the set of given entities.
         */
        String set;

        /**
         * Fields from class and its supertypes up to JpaOlingoEntity.
         */
        Field[] fields;

        /**
         * Navigation properties.
         */
        List<Link> links;

        /**
         * Primitive properties.
         */
        List<Property> properties;

        /**
         * Storage of Java getters and setters for links (navigation properties).
         */
        Map<String, RealAccessors> linkAccessors;

        /**
         * Storage of Java getters and setters for OData primitive properties.
         */
        Map<String, RealAccessors> propertyAccessors;
    }

    /**
     * Getter and setter from the "java" entity.
     *
     * @param <T>
     *         entity to be used in OData service
     */
    private static class RealAccessors<T extends JpaOlingoEntity> {
        private String name;
        private boolean isKey;
        private Method getter;
        private Method setter;

        RealAccessors(T entity, Field f) {
            name = f.getName();
            isKey = f.isAnnotationPresent(ODataKey.class);
            try {
                getter = entity.getClass().getMethod(prepareGetter());
                setter = entity.getClass().getMethod(prepareSetter(), f.getType());
            } catch (NoSuchMethodException e) {
                LOG.error("Reflection problem with preparing an accessor", e);
            }
        }

        /**
         * Executes getter.
         *
         * @param entity
         *         this object
         * @return value from the object
         */
        Object get(T entity) {
            try {
                return getter.invoke(entity);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.error("Reflection problem with getting a value", e);
            }

            return null;
        }

        /**
         * Executes setter. Overrides property value (if the property is not a key).
         *
         * @param entity
         *         this object
         * @param value
         *         new value to be set
         */
        void set(T entity, Object value) {
            if (!isKey) {
                try {
                    setter.invoke(entity, value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error("Reflection problem with setting a value", e);
                }
            }
        }

        private String prepareGetter() {
            return prepareAccessor('g');
        }

        private String prepareSetter() {
            return prepareAccessor('s');
        }

        private String prepareAccessor(char type) {
            return type + "et" + name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
}
