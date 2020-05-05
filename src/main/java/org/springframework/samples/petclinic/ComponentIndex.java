package org.springframework.samples.petclinic;

import com.byoskill.architecture.document.ArchitectureDocument;
import com.byoskill.architecture.document.layer.information.datalayer.DataLayer;
import com.byoskill.architecture.document.metamodel.Component;
import com.byoskill.architecture.document.metamodel.Container;
import com.byoskill.architecture.document.metamodel.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.Validate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponentIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentIndex.class);

    private Map<String, Component> componentMap = new LinkedHashMap<>();

    public void index(final ArchitectureDocument architectureDocument) {
        indexLayer(architectureDocument.getInformationLayer());
        indexLayer(architectureDocument.getInformationLayer().getApplicationLayer());
        indexLayer(architectureDocument.getInformationLayer().getApplicationLayer().getLogical());
        indexLayer(architectureDocument.getInformationLayer().getApplicationLayer().getPhysical());

        architectureDocument.getInformationLayer().getDataLayers()
                            .forEach(this::indexDataLayer);


        indexLayer(architectureDocument.getIntegrationLayer());
        indexLayer(architectureDocument.getTechnologyLayer());
        indexLayer(architectureDocument.getTechnologyLayer().getInfrastructure());

        LOGGER.info("Found {} components", componentMap.size());
    }

    public <T extends Component> T find(final String databaseLayer) {
        return (T) this.componentMap.get(databaseLayer);
    }

    public void indexDataLayer(final DataLayer dl) {
        indexOneComponent(dl);
        indexLayer(dl.getConceptualLayer());
        indexLayer(dl.getLogicalLayer());
        indexLayer(dl.getPhysicalLayer());
        dl.getPhysicalLayer().getSchemes()
          .forEach(scheme -> {
              indexOneComponent(scheme);
              indexComponentList(scheme.getTables());
          });

        indexComponentList(dl.getConceptualLayer().getConcepts());
        indexComponentList(dl.getConceptualLayer().getContainers());
        indexComponentList(dl.getLogicalLayer().getEntities());
        indexComponentList(dl.getLogicalLayer().getZones());
        indexComponentList(dl.getLogicalLayer().getContainers());


    }

    private void indexLayer(final Layer layer) {
        if (layer == null) return ;
        indexOneComponent(layer);
        if (layer == null) return;
        final List<Container> containers = layer.getContainers();
        if (containers == null) return;
        containers.forEach(
            this::indexComponentsContainer
        );
    }

    private void indexComponentList(final List<? extends Component> components) {
        if (components == null) return;
        for (Component component : components) {
            indexOneComponent(component);
        }
    }

    private void indexComponentsContainer(final Container container) {
        indexOneComponent(container);
        indexComponentList(container.getComponents());
    }

    private void indexOneComponent(final Component container) {
        Validate.notNull(container.getKey(), "Component key cannot be null");
        this.componentMap.put(container.getKey(), container);
    }
}


