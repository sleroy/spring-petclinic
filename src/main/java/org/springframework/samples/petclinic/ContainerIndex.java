package org.springframework.samples.petclinic;

import com.byoskill.architecture.document.ArchitectureDocument;
import com.byoskill.architecture.document.layer.information.datalayer.DataLayer;
import com.byoskill.architecture.document.metamodel.Container;
import com.byoskill.architecture.document.metamodel.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;

public class ContainerIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerIndex.class);

    private LinkedHashMap<String, Container> containerMap = new LinkedHashMap<>();

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

        LOGGER.info("Found {} containers", containerMap.size());
    }

    private void indexLayer(final Layer container) {
        if (container == null) return;
        final List<Container> containers = container.getContainers();
        if (containers == null) return;
        containers.forEach(
            cnt -> this.containerMap.put(cnt.getKey(), cnt)
        );
    }

    public void indexDataLayer(final DataLayer dataLayer) {
        indexLayer(dataLayer);
        indexLayer(dataLayer.getConceptualLayer());
        indexLayer(dataLayer.getPhysicalLayer());
        indexLayer(dataLayer.getLogicalLayer());

    }

    public <T extends Container> T find(final String databaseLayer) {
        return (T) this.containerMap.get(databaseLayer);
    }
}
