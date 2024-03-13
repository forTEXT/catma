package de.catma.api.pre;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotationProperty;
import de.catma.api.pre.serialization.model_wrappers.PreApiPropertyDefinition;
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
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.graph.interfaces.GraphProjectHandler;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class PreProject {
	private final Logger logger = Logger.getLogger(PreProject.class.getName());
	
	private final GitProjectHandler gitProjectHandler;
	private final GraphProjectHandler graphProjectHandler;
	private final ReadWriteLock accessLock = new ReentrantReadWriteLock();
	private final String namespace;
	private final String projectId;
	
	public PreProject(String namespace, String projectId, GitProjectHandler gitProjectHandler, GraphProjectHandler graphProjectHandler) {
		super();
		this.namespace = namespace;
		this.projectId = projectId;
		this.gitProjectHandler = gitProjectHandler;
		this.graphProjectHandler = graphProjectHandler;
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
                String.format("%s%s/%s/project/%s/%s/doc/%s", 
                		CATMAPropertyKey.API_BASE_URL.getValue(), 
                		PreApplication.API_PACKAGE, 
                		PreApplication.API_VERSION, 
                		getNamespace(),
                		getProjectId(),
                		sourceDocument.getUuid().toLowerCase()),
                crc32bChecksum,
                size,
                sourceDocument.toString(),
                sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getURI());
	}
	
	public String getProjectId() {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return projectId;
		}
		finally {
			readLock.unlock();
		}
	}
	
	public String getNamespace() {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return namespace;
		}
		finally {
			readLock.unlock();
		}	
	}

    public String serializeProjectResources() {
    	Lock readLock = accessLock.readLock();
        try {
        	readLock.lock();
        	
            Builder<ExportDocument> exportListBuilder = ImmutableList.builder();
            		
            for (SourceDocumentReference sourceDocumentReference : graphProjectHandler.getSourceDocumentReferences()) {
            	SourceDocument sourceDocument = this.graphProjectHandler.getSourceDocument(sourceDocumentReference.getUuid());
            	
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
                		getPreApiSourceDocument(sourceDocument),
                        tagDefinitions.stream()
	                        .map(td -> new PreApiTagDefinition(
	                        		td.getUuid(), 
	                        		td.getParentUuid(), 
	                        		td.getName(), 
	                        		td.getColor(), 
	                        		td.getUserDefinedPropertyDefinitions().stream()
	                        			.map(pd -> new PreApiPropertyDefinition(
	                        					pd.getUuid(), 
	                        					pd.getName(), 
	                        					Collections.unmodifiableList(pd.getPossibleValueList()))).collect(Collectors.toList())))
	                        .collect(Collectors.toList()),
                        tagReferences.stream().map(
                                (TagReference tagReference) -> {
                                    try {
                                    	TagDefinition tag = tagDefinitions.stream().filter(td -> td.getUuid().equals(tagReference.getTagDefinitionId())).findFirst().get();
                                    	return new PreApiAnnotation(
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

            return new SerializationHelper<Export>().serialize(new Export(exportListBuilder.build()));
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to serialize project resources", e);
            return "{\"error\": \"Failed to serialize project resources, please contact CATMA support\"}";
        }
        finally {
        	readLock.unlock();
        }
    }

	public URI getFileUri(String documentId) {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return graphProjectHandler.getSourceDocumentReference(documentId.toUpperCase()).getSourceDocumentInfo().getTechInfoSet().getURI();
		}
		finally {
			readLock.unlock();
		}	

	}

}
