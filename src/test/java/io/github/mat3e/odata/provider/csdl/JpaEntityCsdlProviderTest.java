package io.github.mat3e.odata.provider.csdl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.testng.annotations.Test;

import io.github.mat3e.odata.annotation.ODataEntity;
import io.github.mat3e.odata.annotation.ODataKey;
import io.github.mat3e.odata.annotation.ODataNavigationProperty;
import io.github.mat3e.odata.annotation.ODataProperty;
import io.github.mat3e.odata.entity.JpaOlingoEntity;
import io.github.mat3e.odata.exception.CsdlExtractException;
import io.github.mat3e.odata.util.FullQualifiedNamesUtil;

public class JpaEntityCsdlProviderTest {

    private final static String SET_1 = "TestEntities";
    private final static String SET_2 = "NestedEntities";
    private final static String NAME_1 = "TestEntity";
    private final static String ID_FIELD = "ID";
    private final static String NAME_FIELD = "Name";
    private final static String NESTED_FIELD = "NestedEntity";
    private final static String ID_VALUE = "1";
    private final static String NAME_VALUE = "dummy";

    @ODataEntity(name = "NestedEntity", entitySetName = SET_2)
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

    @ODataEntity(name = NAME_1, entitySetName = SET_1)
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

    class TestCsdlEntityProvider extends JpaEntityCsdlProvider<TestEntity> {
        TestCsdlEntityProvider() throws CsdlExtractException {
            super(TestEntity.class);
        }
    }

    @Test
    public void test_JpaEntityCsdlProvider_getCsdlEntitySet_returnsWhatDefined() throws CsdlExtractException {

        // GIVEN
        final JpaEntityCsdlProvider sut = new TestCsdlEntityProvider();

        // WHEN
        CsdlEntitySet result = sut.getCsdlEntitySet();
        List<CsdlNavigationPropertyBinding> bindings = result.getNavigationPropertyBindings();

        // THEN
        assertThat(result.getName()).isEqualTo(SET_1);
        assertThat(result.getTypeFQN().getNamespace()).isEqualTo(FullQualifiedNamesUtil.NAMESPACE.ENTITIES);
        assertThat(bindings).hasSize(1);
        assertThat(bindings.get(0).getTarget()).isEqualTo(SET_2);
        assertThat(bindings.get(0).getPath()).isEqualTo(NESTED_FIELD);
    }

    @Test
    public void test_JpaEntityCsdlProvider_getCsdlEntityType_returnsWhatDefined() throws CsdlExtractException {

        // GIVEN
        final JpaEntityCsdlProvider sut = new TestCsdlEntityProvider();

        // WHEN
        CsdlEntityType result = sut.getCsdlEntityType();

        // THEN
        assertThat(result.getName()).isEqualTo(NAME_1);
        assertThat(result.hasStream()).isFalse();
        assertThat(result.getKey()).hasSize(1);
        assertThat(result.getKey().get(0).getName()).isEqualTo(ID_FIELD);
        assertThat(result.getProperty(ID_FIELD).getType()).isEqualTo("Edm.String");
        assertThat(result.getProperty(NAME_FIELD).getType()).isEqualTo("Edm.String");
    }

    @Test
    public void test_JpaEntityCsdlProvider_getFQN_returnsWhatDefined() throws CsdlExtractException {

        // GIVEN
        final JpaEntityCsdlProvider sut = new TestCsdlEntityProvider();

        // WHEN
        FullQualifiedName result = sut.getFQN();

        // THEN
        assertThat(result.getName()).isEqualTo(NAME_1);
        assertThat(result.getNamespace()).isEqualTo(FullQualifiedNamesUtil.NAMESPACE.ENTITIES);
    }
}
