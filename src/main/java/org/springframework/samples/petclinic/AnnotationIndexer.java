package org.springframework.samples.petclinic;

import com.byoskill.architecture.annotations.metamodel.DatabaseSchemeDoc;
import com.byoskill.architecture.annotations.metamodel.RelationshipDoc;
import com.byoskill.architecture.annotations.metamodel.TableDoc;
import com.byoskill.architecture.boot.config.ArchitectureModuleConfigurationBean;
import com.byoskill.architecture.document.ArchitectureDocument;
import com.byoskill.architecture.document.layer.information.datalayer.DataLayer;
import com.byoskill.architecture.document.layer.information.datalayer.physical.DatabaseScheme;
import com.byoskill.architecture.document.layer.information.datalayer.physical.Table;
import com.byoskill.architecture.document.metamodel.Component;
import com.byoskill.architecture.document.metamodel.Relationship;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.*;

public class AnnotationIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationIndexer.class);

    private final ComponentIndex                      componentIndex;
    private final ContainerIndex                      containerIndex;
    private final ArchitectureModuleConfigurationBean architectureModuleConfiguration;
    private       ArchitectureDocument                architectureDocument;
    private       AnnotationMetadata                  annotationMetadata;
    private Table currentTable;
    private DataLayer      currentDataLayer;
    private DatabaseScheme currentScheme;

    private List<Component> declaredComponents = new ArrayList<>();

    public AnnotationIndexer(final ComponentIndex componentIndex,
                             final ContainerIndex containerIndex,
                             final ArchitectureModuleConfigurationBean architectureModuleConfiguration,
                             final ArchitectureDocument architectureDocument,
                             final AnnotationMetadata annotationMetadata) {

        this.componentIndex = componentIndex;
        this.containerIndex = containerIndex;
        this.architectureModuleConfiguration = architectureModuleConfiguration;
        this.architectureDocument = architectureDocument;
        this.annotationMetadata = annotationMetadata;
    }


    public void indexation() {
        LOGGER.info("Indexation of the annotations");

        // First we track the component declarations
        findAndIndexDatabaseScheme(getAttributes(DatabaseSchemeDoc.class));
        //findAndIndexTableDoc(getAttributes(ContainerDoc.class));
        //findAndIndexTableDoc(getAttributes(FunctionalZoneDoc.class));
        //findAndIndexTableDoc(getAttributes(FunctionalZonesDoc.class));
        // Entities
        findAndIndexTableDoc(getAttributes(TableDoc.class));
        //findAndIndexComponentDoc(getAttributes(ComponentDoc.class));
        //findAndIndexConceptDoc(getAttributes(ConceptDoc.class));
        //findAndIndexContainerDoc(getAttributes(ContainerDoc.class));
        //findAndIndexLogicalEntityDoc(getAttributes(LogicalEntityDoc.class));

        // unsupported : ColumnDoc
        // unsupported : LogicalColumnDoc


        // Then the relationships

        // RelationshipDoc
    }

    private void findAndIndexDatabaseScheme(final Map<String, Object> attributes) {
        if ( attributes == null) return ;
        final String key = (String) attributes.get("value");

        currentScheme = findScheme(attributes, key);

    }

    private DatabaseScheme findScheme(final Map<String, Object> attributes, final String key) {
        if ( attributes == null) return null;
        DatabaseScheme scheme = null;
        final String databaseLayer = (String) attributes.get("databaseLayer");
        currentDataLayer = findOrCreateDataLayer(attributes, databaseLayer);


        Optional<DatabaseScheme> databaseScheme = currentDataLayer.getPhysicalLayer().findScheme(key);

        if (databaseScheme.isPresent()) {
            scheme = databaseScheme.get();
        } else {
            scheme = new DatabaseScheme();
            currentDataLayer.getPhysicalLayer().addScheme(scheme);
        }
        if (isValueAvailable(attributes, "value")) scheme.setKey(key);
        if (isValueAvailable(attributes, "name")) scheme.setName((String) attributes.get("name"));
        if (isValueAvailable(attributes, "description"))
            scheme.setDescription((String) attributes.get("description"));
        if (isValueAvailable(attributes, "technology"))
            scheme.setTechnology((String) attributes.get("technology"));
        if (isValueAvailable(attributes, "relationships"))
            scheme.setRelationships(convertRelationships((AnnotationAttributes[]) attributes.get("relationships")));
        return scheme;
    }

    private Map<String, Object> getAttributes(final Class<?> tableDocClass) {
        return this.annotationMetadata.getAnnotationAttributes(tableDocClass.getName());
    }

    private void findAndIndexTableDoc(final Map<String, Object> attributes) {
        if (attributes == null) return;


        var scheme = currentScheme;
        if (scheme == null) {
            final DataLayer dataLayer = this.findOrCreateDataLayer(Collections.emptyMap(), (String) attributes.get("databaseLayer"));
            final HashMap<String, Object> hashMap = new HashMap<>();
            final String schemeId = (String) attributes.get("scheme");
            hashMap.put("name", schemeId);
            hashMap.put("value", schemeId);
            scheme = this.findScheme(hashMap, schemeId);
            dataLayer.getPhysicalLayer().addScheme(scheme);
        }

        // Find #DatabaseScheme if already declared
        currentTable = scheme.findTable((String) attributes.get("value")).orElse(null);
        if (currentTable == null) {
            currentTable = new Table();
            scheme.addTable(currentTable);
        }

        if (isValueAvailable(attributes, "value")) currentTable.setKey((String) attributes.get("value"));
        if (isValueAvailable(attributes, "name")) currentTable.setName((String) attributes.get("name"));
        if (isValueAvailable(attributes, "description")) currentTable.setDescription((String) attributes.get("description"));
        if (isValueAvailable(attributes, "technology")) currentTable.setTechnology((String) attributes.get("technology"));
        if (isValueAvailable(attributes, "fullName")) currentTable.setFullName((String) attributes.get("fullName"));
        if (isValueAvailable(attributes, "definition")) currentTable.setDefinition((String) attributes.get("definition"));
        if (isValueAvailable(attributes, "type")) currentTable.setType((String) attributes.get("type"));
        if (isValueAvailable(attributes, "relationships")) {
            currentTable.setRelationships(convertRelationships((AnnotationAttributes[]) attributes.get("relationships")));
        }
        //TODO::setColumns


    }

    private DataLayer findOrCreateDataLayer(final Map<String, Object> attributes, String databaseLayer) {
        DataLayer container = null;
        if (databaseLayer == null) {
            container = this.componentIndex.find(databaseLayer = DataLayer.DATABASE_LAYER);
        } else {
            container = this.componentIndex.find(databaseLayer);
        }
        if (container == null) {
            LOGGER.error("Database Scheme {} without Database layer, adding to the default one", attributes);
            final DataLayer dataLayer = DataLayer.builder()
                                                 .key(databaseLayer)
                                                 .name(databaseLayer)
                                                 .build();
            this.architectureDocument.getInformationLayer().addDataLayer(dataLayer);
            this.componentIndex.indexDataLayer(dataLayer);
            this.containerIndex.indexDataLayer(dataLayer);
            container = dataLayer;
        }
        return container;
    }

    private boolean isValueAvailable(final Map<String, Object> attributes, final String key) {
        final Object value = attributes.get(key);
        if (value == null) return false;
        if (!(value instanceof String)) return attributes.get("value") != null;
        return !StringUtils.isBlank((String) value);
    }

    private List<Relationship> convertRelationships(final AnnotationAttributes[] relationships) {
        return List.of();
    }

    private void findAndIndexComponentDoc(final Map<String, Object> attributes) {
        if ( attributes == null) return ;
    }

    private void findAndIndexConceptDoc(final Map<String, Object> attributes) {
        if ( attributes == null) return ;
    }

    private void findAndIndexContainerDoc(final Map<String, Object> attributes) {
        if ( attributes == null) return ;
    }

    private void findAndIndexLogicalEntityDoc(final Map<String, Object> attributes) {
        if ( attributes == null) return ;
    }
}
