package io.github.mat3e.odata.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.testng.annotations.Test;

import io.github.mat3e.odata.annotation.ODataEntity;
import io.github.mat3e.odata.annotation.ODataKey;
import io.github.mat3e.odata.annotation.ODataNavigationProperty;
import io.github.mat3e.odata.annotation.ODataProperty;
import mockit.Mocked;

public class JpaOlingoEntityTest {

    private final static String SET_1 = "TestEntities";
    private final static String SET_2 = "ExtendingEntities";
    private final static String SET_3 = "NestedEntities";
    private final static String ID_FIELD = "ID";
    private final static String PIN_FIELD = "PIN";
    private final static String NAME_FIELD = "Name";
    private final static String NESTED_FIELD = "NestedEntity";
    private final static String ID_VALUE = "1";
    private final static String NAME_VALUE = "dummy";
    private final static int PIN_VALUE = 123;

    private final TestEntity basic = new TestEntity();
    private final ExtendingEntity extended = new ExtendingEntity();
    private final NestedEntity nested = new NestedEntity();

    @Mocked
    EntityToMock entityMock;

    @ODataEntity(name = "EntityToMock")
    class EntityToMock extends JpaOlingoEntity {
    }

    @ODataEntity(name = "NestedEntity", entitySetName = SET_3)
    class NestedEntity extends JpaOlingoEntity {
        @ODataKey
        @ODataProperty(name = ID_FIELD, type = EdmPrimitiveTypeKind.String)
        private String ID = ID_VALUE;

        public String getID() {
            return this.ID;
        }

        public void setID(String ID) {
            this.ID = ID;
        }
    }

    @ODataEntity(name = "TestEntity", entitySetName = SET_1)
    class TestEntity extends JpaOlingoEntity {
        @ODataKey
        @ODataProperty(name = ID_FIELD, type = EdmPrimitiveTypeKind.String)
        private String ID = ID_VALUE;

        @ODataProperty(name = NAME_FIELD, type = EdmPrimitiveTypeKind.String)
        private String name = NAME_VALUE;

        @ODataNavigationProperty(name = NESTED_FIELD)
        NestedEntity nestedEntity = new NestedEntity();

        public String getID() {
            return this.ID;
        }

        public void setID(String ID) {
            this.ID = ID;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NestedEntity getNestedEntity() {
            return this.nestedEntity;
        }

        public void setNestedEntity(NestedEntity nestedEntity) {
            this.nestedEntity = nestedEntity;
        }
    }

    @ODataEntity(name = "ExtendingEntity", entitySetName = SET_2)
    class ExtendingEntity extends TestEntity {
        @ODataKey
        @ODataProperty(name = PIN_FIELD, type = EdmPrimitiveTypeKind.Int32)
        private int PIN = PIN_VALUE;

        @ODataNavigationProperty(name = SET_3)
        NestedEntity[] nestedEntities = Stream.of(new NestedEntity(), new NestedEntity()).toArray(NestedEntity[]::new);

        public int getPIN() {
            return this.PIN;
        }

        public void setPIN(int PIN) {
            this.PIN = PIN;
        }

        public NestedEntity[] getNestedEntities() {
            return this.nestedEntities;
        }

        public void setNestedEntities(NestedEntity[] nestedEntities) {
            this.nestedEntities = nestedEntities;
        }
    }

    @Test
    public void test_JpaOlingoEntity_getId_returnsProperUri() {

        // GIVEN + WHEN
        String result = basic.getId().toString();

        // THEN
        assertThat(result).isEqualTo(SET_1 + "('" + ID_VALUE + "')");
    }

    @Test
    public void test_JpaOlingoEntity_getId_returnsProperUriWithManyKeys() {

        // GIVEN + WHEN
        String result = extended.getId().toString();

        // THEN
        assertThat(result)
                .isEqualTo(SET_2 + "(" + PIN_FIELD + "=" + PIN_VALUE + "," + ID_FIELD + "='" + ID_VALUE + "')");
    }

    @Test
    public void test_JpaOlingoEntity_getProperty_returnsProperProperty() {

        // GIVEN + WHEN
        Property result = basic.getProperty(NAME_FIELD);

        // THEN
        assertThat(result.asPrimitive()).isEqualTo(NAME_VALUE);
        assertThat(result.getValue()).isEqualTo(NAME_VALUE);
    }

    @Test
    public void test_JpaOlingoEntity_getProperties_returnsAllTheHierarchyProperties() {

        // GIVEN + WHEN
        List<Property> result = extended.getProperties();

        // THEN
        assertThat(result).hasSize(3); // id, pin, name
        assertThat(result.stream().map(Property::getName)).contains(NAME_FIELD, ID_FIELD, PIN_FIELD);
    }

    @Test
    public void test_JpaOlingoEntity_getNavigationLink_returnsProperEntity() {

        // GIVEN + WHEN
        Link result = basic.getNavigationLink(NESTED_FIELD);

        // THEN
        assertThat(result.getInlineEntitySet()).isNull();
        assertThat(result.getInlineEntity()).isEqualTo(nested);
    }

    @Test
    public void test_JpaOlingoEntity_getNavigationLink_returnsProperEntitySet() {

        // GIVEN + WHEN
        Link result = extended.getNavigationLink(SET_3);

        // THEN
        assertThat(result.getInlineEntity()).isNull();
        assertThat(result.getInlineEntitySet()).hasSize(2);
        assertThat(result.getInlineEntitySet()).contains(nested);
    }
}
