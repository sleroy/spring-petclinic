package org.springframework.samples.petclinic;

import com.byoskill.architecture.boot.config.ArchitectureModuleConfigurationBean;
import com.byoskill.architecture.boot.config.DatabaseSchemaImporterBean;
import com.byoskill.architecture.tools.indexer.ArchitectureModuleIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArchitectureEntityIndexer {

    private static final Logger                              LOGGER         = LoggerFactory.getLogger(ArchitectureModuleIndexer.class);
    private              ArchitectureModuleIndexer           moduleIndexer;
    private              ArchitectureModuleConfigurationBean architectureModuleConfiguration;
    private              DatabaseSchemaImporterBean          databaseSchemaImporterBean;
    private              ComponentIndex                      componentIndex = new ComponentIndex();
    private              ContainerIndex                      containerIndex = new ContainerIndex();

    public ArchitectureEntityIndexer(ArchitectureModuleIndexer moduleIndexer,
                                     ArchitectureModuleConfigurationBean architectureModuleConfiguration,
                                     DatabaseSchemaImporterBean databaseSchemaImporterBean) {

        this.moduleIndexer = moduleIndexer;
        this.architectureModuleConfiguration = architectureModuleConfiguration;
        this.databaseSchemaImporterBean = databaseSchemaImporterBean;
    }


    @PostConstruct
    public void init() {

        componentIndex.index(moduleIndexer.getArchitectureDocument());
        containerIndex.index(moduleIndexer.getArchitectureDocument());

        ClassPathScanningCandidateComponentProvider scanningProvider = new ClassPathScanningCandidateComponentProvider(false);

        List<MetadataReader> beanDefinitions = new ArrayList<>(100);

        scanningProvider.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
            final boolean isArchitectureAnnotatedBean = metadataReader.getAnnotationMetadata().getAnnotationTypes()
                                                                      .stream()
                                                                      .anyMatch(aT -> aT.startsWith("com.byoskill"));
            metadataReader.getAnnotationMetadata().getAnnotations();
            if (isArchitectureAnnotatedBean) {
                beanDefinitions.add(metadataReader);
            }
            return isArchitectureAnnotatedBean;
        });

        for (String basePackage : this.architectureModuleConfiguration.getBasePackages()) {
            LOGGER.info("Scanning the package ... {}", basePackage);
            scanningProvider.findCandidateComponents(basePackage);
        }

        LOGGER.info("Found {} decorated entities ...", beanDefinitions.size());
        for (MetadataReader metadataReader : beanDefinitions) {
            beanAnalysis(metadataReader);
        }
    }

    private void beanAnalysis(final MetadataReader metadataReader) {
        AnnotationIndexer annotationIndexer = new AnnotationIndexer(componentIndex,
            containerIndex,
            architectureModuleConfiguration,
            this.moduleIndexer.getArchitectureDocument(),
            metadataReader.getAnnotationMetadata());
        annotationIndexer.indexation();
    }

}
