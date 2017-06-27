package io.github.mat3e.odata.common.provider.csdl;

import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;

/**
 * Basic building block which allows to build $metadata.
 * CSDL = Common Schema Definition Language, lang for $metadata.
 */
public interface CsdlProvider {
    /**
     * Bound operations should have a first parameter a "binding" one.
     * This parameter should be of type to which they are bound.
     */
    String BINDING_PARAM_NAME = "BindingParameter";

    /**
     * FQN of the object provided.
     */
    FullQualifiedName getFQN();

    /**
     * Extracting Entity Set.
     */
    CsdlEntitySet getCsdlEntitySet();

    /**
     * Extracting Entity.
     */
    CsdlEntityType getCsdlEntityType();

    /**
     * Extracting Enum.
     */
    CsdlEnumType getCsdlEnumType();

    /**
     * Extracting Complex Type.
     */
    CsdlComplexType getCsdlComplexType();

    /**
     * Method for extracting bound/unbound Actions.
     */
    List<CsdlAction> getCsdlActions();

    /**
     * Method for extracting bound/unbound Functions.
     * Functions are read-only. Shouldn't change any type.
     */
    List<CsdlFunction> getCsdlFunctions();
}
