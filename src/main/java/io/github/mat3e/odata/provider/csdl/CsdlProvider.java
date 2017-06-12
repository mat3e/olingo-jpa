package io.github.mat3e.odata.provider.csdl;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;

/**
 * Basic building block which allows to build $metadata.
 * CSDL = Common Schema Definition Language, lang for $metadata.
 */
public interface CsdlProvider {
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
}
