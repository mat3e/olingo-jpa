package io.github.mat3e.odata.common.provider.csdl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.testng.annotations.Test;

import io.github.mat3e.odata.common.annotation.ODataEntity;
import io.github.mat3e.odata.common.annotation.ODataKey;
import io.github.mat3e.odata.common.annotation.ODataNavigationProperty;
import io.github.mat3e.odata.common.annotation.ODataProperty;
import io.github.mat3e.odata.common.entity.JpaOlingoEntity;
import io.github.mat3e.odata.common.entity.JpaOlingoMediaEntity;
import io.github.mat3e.odata.common.exception.CsdlExtractException;
import io.github.mat3e.odata.common.util.FullQualifiedNamesUtil;

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

        @ODataNavigationProperty(name = SET_2)
        List<NestedEntity> nestedEntities = Arrays.asList(new NestedEntity(), new NestedEntity());

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

        public List<NestedEntity> getNestedEntities() {
            return this.nestedEntities;
        }

        public void setNestedEntities(List<NestedEntity> nestedEntities) {
            this.nestedEntities = nestedEntities;
        }
    }

    @ODataEntity(name = "MediaEntity", entitySetName = "MediaEntities")
    class MediaEntity extends JpaOlingoMediaEntity {
        @ODataKey
        @ODataProperty(name = ID_FIELD, type = EdmPrimitiveTypeKind.String)
        private String ID = ID_VALUE;

        public String getID() {
            return this.ID;
        }

        public void setID(String ID) {
            this.ID = ID;
        }

        @Override
        public byte[] getContent() {
            return new byte[0];
        }

        @Override
        public void setContent(byte[] data) {

        }
    }

    class TestCsdlEntityProvider extends JpaEntityCsdlProvider<TestEntity> {
        TestCsdlEntityProvider() throws CsdlExtractException {
            super(TestEntity.class);
        }
    }

    class TestCsdlMediaEntityProvider extends JpaEntityCsdlProvider<MediaEntity> {
        TestCsdlMediaEntityProvider() throws CsdlExtractException {
            super(MediaEntity.class);
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
        assertThat(bindings).hasSize(2);
        assertThat(bindings.get(0).getTarget()).isEqualTo(SET_2);
        assertThat(bindings.get(0).getPath()).isEqualTo(NESTED_FIELD);
        assertThat(bindings.get(1).getPath()).isEqualTo(SET_2);
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

    @Test
    public void test_JpaEntityCsdlProvider_mapsMediaEntity() throws CsdlExtractException {

        // GIVEN
        final JpaEntityCsdlProvider sut = new TestCsdlMediaEntityProvider();

        // WHEN
        boolean hasStream = sut.getCsdlEntityType().hasStream();

        // THEN
        assertThat(hasStream).isTrue();
    }
}
