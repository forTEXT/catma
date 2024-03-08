package de.catma.api.pre.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.catma.api.pre.PreProjectService;
import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.PreApiSourceDocument;
import de.catma.api.pre.serialization.model_wrappers.PreApiTagDefinition;
import de.catma.api.pre.serialization.models.Export;
import de.catma.api.pre.serialization.models.ExportDocument;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.graph.interfaces.GraphProjectHandler;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class ProjectSerializer {
	private static final Logger logger = Logger.getLogger(ProjectSerializer.class.getName());
	
    public String serializeProjectResources(GraphProjectHandler graphProjectHandler) {
        try {
            Export export = new Export();

            		
            for (SourceDocumentReference sourceDocumentReference : graphProjectHandler.getSourceDocumentReferences()) {
            	SourceDocument sourceDocument = graphProjectHandler.getSourceDocument(sourceDocumentReference.getUuid());
            	
                ArrayList<AnnotationCollection> annotationCollections = new ArrayList<>();
                for (AnnotationCollectionReference annotationCollectionReference : sourceDocumentReference.getUserMarkupCollectionRefs()) {
                    annotationCollections.add(graphProjectHandler.getAnnotationCollection(annotationCollectionReference));
                }

                ArrayList<TagDefinition> tagDefinitions = new ArrayList<>();
                ArrayList<TagReference> tagReferences = new ArrayList<>();
                for (AnnotationCollection annotationCollection : annotationCollections) {
                    for (TagsetDefinition tagsetDefinition : annotationCollection.getTagLibrary().getTagsetDefinitions()) {
                        tagDefinitions.addAll(tagsetDefinition.stream().collect(Collectors.toList()));
                    }

                    tagReferences.addAll(annotationCollection.getTagReferences());
                }

                ExportDocument exportDocument = new ExportDocument(
                        new PreApiSourceDocument(
                                sourceDocument,
                                String.format("%s%s/%s/doc/%s", 
                                		CATMAPropertyKey.API_BASE_URL.getValue(), 
                                		PreProjectService.API_PACKAGE, 
                                		PreProjectService.API_VERSION, 
                                		sourceDocument.getUuid().toLowerCase())
                        ),
                        tagDefinitions.stream().map(PreApiTagDefinition::new).collect(Collectors.toList()),
                        tagReferences.stream().map(
                                (TagReference tagReference) -> {
                                    try {
                                        return new PreApiAnnotation(
                                                tagReference,
                                                tagDefinitions.stream().filter(td -> td.getUuid().equals(tagReference.getTagDefinitionId())).findFirst().get(),
                                                sourceDocument
                                        );
                                    } catch (IOException e) {
                                        logger.log(Level.WARNING, String.format("Error serializing TagReference: %s", tagReference), e);
                                        return null;
                                    }
                                }
                        ).collect(Collectors.toList())
                );

                export.addExportDocument(exportDocument);
            }

            return new SerializationHelper<Export>().serialize(export);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to serialize project resources", e);
            return "{\"error\": \"Failed to serialize project resources, please contact CATMA support\"}";
        }
    }
}
