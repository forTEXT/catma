package de.catma.api.pre;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

import de.catma.api.pre.serialization.model_wrappers.LegacyPreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.LegacyPreApiTagDefinition;
import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotationProperty;
import de.catma.api.pre.serialization.model_wrappers.PreApiSourceDocument;
import de.catma.api.pre.serialization.model_wrappers.PreApiUserPropertyDefinition;
import de.catma.api.pre.serialization.models.LegacyExport;
import de.catma.api.pre.serialization.models.LegacyExportDocument;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

@Deprecated
public class ProjectResourceExportApiRequestHandler implements RequestHandler {
    private static final String BASE_URL = CATMAPropertyKey.BASE_URL.getValue();
    private static final String API_BASE_PATH = "/api";
    private static final String API_PACKAGE = "pre"; // project resource export
    private static final String API_VERSION = "beta";

    private final String handlerPath;
    private final Project project;

    private static final Logger logger = Logger.getLogger(ProjectResourceExportApiRequestHandler.class.getName());

    public ProjectResourceExportApiRequestHandler(Project project) {
        String instanceId = new IDGenerator().generate().toLowerCase();
        this.handlerPath = String.format("%s/%s/%s/%s", API_BASE_PATH, API_PACKAGE, API_VERSION, instanceId);
        this.project = project;
    }

    public String getHandlerUrl() {
        return String.format("%s%s", BASE_URL, handlerPath.substring(1));
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        String requestPath = request.getPathInfo().toLowerCase();

        if (requestPath.equals(handlerPath)) {
            response.setContentType("application/json");
            response.setNoCacheHeaders();

            OutputStream outputStream = response.getOutputStream();
            outputStream.write(serializeProjectResources().getBytes(StandardCharsets.UTF_8));

            return true;
        }
        else if (requestPath.startsWith(handlerPath + "/doc/")) {
            try {
                String[] requestPathParts = requestPath.split("/");
                String documentUuid = requestPathParts[requestPathParts.length - 1];
                SourceDocument sourceDocument = project.getSourceDocument(documentUuid.toUpperCase());

                response.setContentType("text/plain; charset=UTF-8");

                OutputStream outputStream = response.getOutputStream();
                outputStream.write(sourceDocument.getContent().getBytes(StandardCharsets.UTF_8));
            }
            catch (UncheckedExecutionException e) {
                if (e.getCause() instanceof NoSuchElementException) {
                    response.setStatus(404);
                }
                else
                {
                    throw e;
                }
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling document request", e);
                response.setStatus(500);
            }

            return true;
        }

        return false;
    }

    private String serializeProjectResources() {
        try {
            Builder<LegacyExportDocument> exportListBuilder = ImmutableList.builder();

            for (SourceDocumentReference sourceDocumentRef : project.getSourceDocumentReferences()) {
                SourceDocument sourceDocument = project.getSourceDocument(sourceDocumentRef.getUuid());

                ArrayList<AnnotationCollection> annotationCollections = new ArrayList<>();
                for (AnnotationCollectionReference annotationCollectionReference : sourceDocumentRef.getUserMarkupCollectionRefs()) {
                    annotationCollections.add(project.getAnnotationCollection(annotationCollectionReference));
                }

                HashSet<TagDefinition> tagDefinitions = new HashSet<>();
                ArrayList<TagReference> tagReferences = new ArrayList<>();
                for (AnnotationCollection annotationCollection : annotationCollections) {
                    for (TagsetDefinition tagsetDefinition : annotationCollection.getTagLibrary().getTagsetDefinitions()) {
                        tagDefinitions.addAll(tagsetDefinition.stream().collect(Collectors.toList()));
                    }

                    tagReferences.addAll(annotationCollection.getTagReferences());
                }

                LegacyExportDocument exportDocument = new LegacyExportDocument(
                		getPreApiSourceDocument(sourceDocument),
                        tagDefinitions.stream()
	                        .map(td -> new LegacyPreApiTagDefinition(
	                        		td.getUuid(), 
	                        		td.getParentUuid(), 
	                        		td.getName(), 
	                        		td.getHexColor(),
	                        		td.getUserDefinedPropertyDefinitions().stream()
	                        			.map(pd -> new PreApiUserPropertyDefinition(
	                        					pd.getUuid(), 
	                        					pd.getName(), 
	                        					Collections.unmodifiableList(pd.getPossibleValueList()))).collect(Collectors.toList())))
	                        .collect(Collectors.toList()),
                        tagReferences.stream().map(
                                (TagReference tagReference) -> {
                                    try {
                                    	TagDefinition tag = tagDefinitions.stream().filter(td -> td.getUuid().equals(tagReference.getTagDefinitionId())).findFirst().get();
                                    	return new LegacyPreApiAnnotation(
                                    			tagReference.getTagInstanceId(), 
                                    			sourceDocument.getUuid(), 
                                    			tagReference.getRange().getStartPoint(),
                                    			tagReference.getRange().getEndPoint(),
                                    			sourceDocument.getContent(tagReference.getRange()),
                                    			tag.getUuid(), 
                                    			tag.getName(), 
                                    			tagReference.getTagInstance().getUserDefinedProperties().stream()
                                    				.map((p) -> 
                                    					new PreApiAnnotationProperty(
                                    						p.getPropertyDefinitionId(),
                                    						tag.getUserDefinedPropertyDefinitions().stream()
                                    							.filter(pd -> pd.getUuid().equals(p.getPropertyDefinitionId())).findFirst().get().getName(),
                                    						p.getPropertyValueList()
                                    					)
                                    				).toList()
                                                );
                                    } catch (IOException e) {
                                        logger.log(Level.WARNING, String.format("Error serializing TagReference: %s", tagReference), e);
                                        return null;
                                    }
                                }
                        ).collect(Collectors.toList())
                );

                exportListBuilder.add(exportDocument);
            }

            return new SerializationHelper<LegacyExport>().serialize(
            		new LegacyExport(exportListBuilder.build()));
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to serialize project resources", e);
            return "{\"error\": \"Failed to serialize project resources, please contact CATMA support\"}";
        }
    }
    
    
	private PreApiSourceDocument getPreApiSourceDocument(SourceDocument sourceDocument) throws Exception {
		int size = 0;
		String crc32bChecksum = null;
		
		try {
			byte[] bytes = sourceDocument.getContent().getBytes(StandardCharsets.UTF_8);
			
			// checksum - not using sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getChecksum() because it does not
			// respect the charset when it is created
			CRC32 crc = new CRC32();
			crc.update(bytes);
			crc32bChecksum = Long.toHexString(crc.getValue());
			
			// size
			size = bytes.length;
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Couldn't get document content", e);
		}
		
		
		return new PreApiSourceDocument(
				sourceDocument.getUuid(),
                String.format("%s%s/doc/%s", BASE_URL, handlerPath.substring(1), sourceDocument.getUuid().toLowerCase()),
                crc32bChecksum,
                size,
                sourceDocument.toString(),
                sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getAuthor(),
                sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getDescription(),
                sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getPublisher(),
                sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getURI(),
                sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getResponsibleUser()
        );
	}

}
