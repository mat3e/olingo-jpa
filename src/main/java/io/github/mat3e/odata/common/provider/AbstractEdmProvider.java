package io.github.mat3e.odata.common.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlActionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlOperation;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;

import io.github.mat3e.odata.common.provider.csdl.CsdlProvider;
import io.github.mat3e.odata.common.util.FullQualifiedNamesUtil;

/**
 * Base for EdmProviders which makes a usage of {@link CsdlProvider CsdlProviders}.
 */
public abstract class AbstractEdmProvider extends CsdlAbstractEdmProvider {
    private List<CsdlSchema> schemas = new ArrayList<>();

    /**
     * It contains all the sets and imported Functions, Actions.
     */
    // TODO: extended containers
    private CsdlEntityContainer container = new CsdlEntityContainer().setName(FullQualifiedNamesUtil.CONTAINER);

    // maps, for making request by fqn more readable
    private Map<FullQualifiedName, CsdlEntityType> entities = new HashMap<>();
    private Map<FullQualifiedName, CsdlEnumType> enums = new HashMap<>();
    private Map<FullQualifiedName, CsdlComplexType> complexTypes = new HashMap<>();
    private Map<FullQualifiedName, List<CsdlAction>> actions = new HashMap<>();
    private Map<FullQualifiedName, List<CsdlFunction>> functions = new HashMap<>();

    public AbstractEdmProvider(List<CsdlProvider> providers) {

        // Set is always in the context of container.
        List<CsdlEntitySet> entitySets = new ArrayList<>();

        providers.forEach(csdlProvider -> {
            CsdlEntitySet set = csdlProvider.getCsdlEntitySet();
            if (set != null) {
                entitySets.add(set);
            }

            updateTypesMapWithType(csdlProvider.getCsdlEntityType(), entities);

            updateTypesMapWithType(csdlProvider.getCsdlEnumType(), enums);
            updateTypesMapWithType(csdlProvider.getCsdlComplexType(), complexTypes);

            updateOperationsMapFromList(csdlProvider.getCsdlActions(), actions);
            updateOperationsMapFromList(csdlProvider.getCsdlFunctions(), functions);
        });

        container.setEntitySets(entitySets).setActionImports(getActionImports())
                 .setFunctionImports(getFunctionImports());

        schemas.add(
                new CsdlSchema().setNamespace(FullQualifiedNamesUtil.NAMESPACE.ENTITIES).setEntityContainer(container)
                                .setEntityTypes(new ArrayList<>(entities.values())));
        schemas.add(new CsdlSchema().setNamespace(FullQualifiedNamesUtil.NAMESPACE.ACTIONS)
                                    .setActions(normalizeOperations(actions)));
        schemas.add(new CsdlSchema().setNamespace(FullQualifiedNamesUtil.NAMESPACE.FUNCTIONS)
                                    .setFunctions(normalizeOperations(functions)));
        schemas.add(new CsdlSchema().setNamespace(FullQualifiedNamesUtil.NAMESPACE.ENUMS)
                                    .setEnumTypes(new ArrayList<>(enums.values())));
        schemas.add(new CsdlSchema().setNamespace(FullQualifiedNamesUtil.NAMESPACE.COMPLEX_TYPES)
                                    .setComplexTypes(new ArrayList<>(complexTypes.values())));
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        return schemas;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        return container;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {
        return entityContainerName == null || FullQualifiedNamesUtil.CONTAINER_FQN.equals(entityContainerName) ?
                new CsdlEntityContainerInfo().setContainerName(FullQualifiedNamesUtil.CONTAINER_FQN) :
                null;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        return entities.get(entityTypeName);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {
        if (entityContainer.equals(FullQualifiedNamesUtil.CONTAINER_FQN)) {
            return container.getEntitySet(entitySetName);
        }
        return null;
    }

    @Override
    public CsdlEnumType getEnumType(FullQualifiedName enumTypeName) throws ODataException {
        return enums.get(enumTypeName);
    }

    @Override
    public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) throws ODataException {
        return complexTypes.get(complexTypeName);
    }

    @Override
    public List<CsdlAction> getActions(FullQualifiedName actionName) {
        return actions.get(actionName);
    }

    @Override
    public List<CsdlFunction> getFunctions(FullQualifiedName functionName) {
        return functions.get(functionName);
    }

    @Override
    public CsdlActionImport getActionImport(FullQualifiedName entityContainer, String actionImportName) {
        if (entityContainer.equals(FullQualifiedNamesUtil.CONTAINER_FQN)) {
            return container.getActionImport(actionImportName);
        }
        return null;
    }

    @Override
    public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, String functionImportName) {
        if (entityContainer.equals(FullQualifiedNamesUtil.CONTAINER_FQN)) {
            return container.getFunctionImport(functionImportName);
        }
        return null;
    }

    /**
     * For now the only option for adding imported actions is by specifying them here.
     */
    abstract protected List<CsdlActionImport> getActionImports();

    /**
     * For now the only option for adding imported functions is by specifying them here.
     */
    abstract protected List<CsdlFunctionImport> getFunctionImports();

    private <T extends CsdlAbstractEdmItem> void updateTypesMapWithType(T type, Map<FullQualifiedName, T> types) {
        if (type != null) {
            FullQualifiedName fqn;
            if (type instanceof CsdlEntityType) {
                fqn = FullQualifiedNamesUtil.createFullQualifiedEntityName(((CsdlEntityType) type).getName());
            } else if (type instanceof CsdlEnumType) {
                fqn = FullQualifiedNamesUtil.createFullQualifiedEnumName(((CsdlEnumType) type).getName());
            } else if (type instanceof CsdlComplexType) {
                fqn = FullQualifiedNamesUtil.createFullQualifiedComplexTypeName(((CsdlComplexType) type).getName());
            } else {
                throw new IllegalStateException("Unsupported CSDL Type");
            }

            types.put(fqn, type);
        }
    }

    private <T extends CsdlOperation> void updateOperationsMapFromList(List<T> list,
            Map<FullQualifiedName, List<T>> operations) {
        for (T operation : list) {
            FullQualifiedName fqn;
            if (operation instanceof CsdlAction) {
                fqn = FullQualifiedNamesUtil.createFullQualifiedActionName(operation.getName());
            } else if (operation instanceof CsdlFunction) {
                fqn = FullQualifiedNamesUtil.createFullQualifiedFunctionName(operation.getName());
            } else {
                throw new IllegalStateException("Not an operation");
            }

            if (operations.containsKey(fqn)) {
                operations.get(fqn).add(operation);
            } else {
                operations.put(fqn, Arrays.asList(operation));
            }
        }
    }

    private <T extends CsdlOperation> List<T> normalizeOperations(Map<FullQualifiedName, List<T>> operations) {
        List<T> normalized = new ArrayList<>();
        for (FullQualifiedName fqn : operations.keySet()) {
            normalized.addAll(operations.get(fqn));
        }
        return normalized;
    }
}
