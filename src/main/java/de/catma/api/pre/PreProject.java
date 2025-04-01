package de.catma.api.pre;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import javax.ws.rs.core.UriBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import de.catma.api.pre.cache.AnnotationCountCache;
import de.catma.api.pre.cache.AnnotationCountCache.CacheKey;
import de.catma.api.pre.serialization.model_wrappers.*;
import de.catma.api.pre.serialization.models.Export;
import de.catma.api.pre.serialization.models.ExportDocument;
import de.catma.api.pre.serialization.models.ExtendedMetadata;
import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.graph.interfaces.GraphProjectHandler;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.*;

public class PreProject {
	public static final int DEFAULT_PAGE_SIZE = 100;
	@VisibleForTesting
	public static final Comparator<TagReference> TAG_REFERENCE_COMPARATOR = 
			(tRef1, tRef2) -> tRef1.getTagInstanceId().equals(tRef2.getTagInstanceId())?tRef1.getRange().compareTo(tRef2.getRange()):tRef1.getTagInstanceId().compareTo(tRef2.getTagInstanceId());
	
	private final Logger logger = Logger.getLogger(PreProject.class.getName());
	
	private final GitProjectHandler gitProjectHandler;
	private final GraphProjectHandler graphProjectHandler;
	private final ReadWriteLock accessLock = new ReentrantReadWriteLock();
	private final String userName;
	private final String namespace;
	private final String projectId;
	private final TagManager tagManager;
	private final AnnotationCountCache annotationCountCache;
	
	public PreProject(String userName, String namespace, String projectId, TagManager tagManager, GitProjectHandler gitProjectHandler, GraphProjectHandler graphProjectHandler, AnnotationCountCache annotationCountCache) {
		super();
		this.userName = userName;
		this.namespace = namespace;
		this.projectId = projectId;
		this.tagManager = tagManager;
		this.gitProjectHandler = gitProjectHandler;
		this.graphProjectHandler = graphProjectHandler;
		this.annotationCountCache = annotationCountCache;
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
                		sourceDocument.getUuid()),
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
	
	private ExtendedMetadata createExtendendMetadata() throws Exception {
    	ImmutableMap.Builder<String, PreApiSourceDocument> documentMapBuilder = ImmutableMap.builder();
    	ImmutableMap.Builder<String, PreApiAnnotationCollection> collectionMapBuilder = ImmutableMap.builder();
    	
    	for (SourceDocumentReference sourceDocumentReference : graphProjectHandler.getSourceDocumentReferences()) {
        	SourceDocument sourceDocument = this.graphProjectHandler.getSourceDocument(sourceDocumentReference.getUuid());
    		documentMapBuilder.put(sourceDocumentReference.getUuid(), getPreApiSourceDocument(sourceDocument));
            for (AnnotationCollectionReference annotationCollectionReference : sourceDocumentReference.getUserMarkupCollectionRefs()) {
                collectionMapBuilder.put(
                		annotationCollectionReference.getId(), 
                		new PreApiAnnotationCollection(
                				annotationCollectionReference.getId(), 
                				annotationCollectionReference.getName(), 
                				annotationCollectionReference.getContentInfoSet().getAuthor(),
                				annotationCollectionReference.getContentInfoSet().getDescription(),
                				annotationCollectionReference.getContentInfoSet().getPublisher(),
                				annotationCollectionReference.getResponsibleUser(),
                				annotationCollectionReference.getSourceDocumentId()));
            }
    	}
    	
    	ImmutableMap.Builder<String, PreApiTagsetDefinition> tagsetMapBuilder = ImmutableMap.builder();
    	ImmutableMap.Builder<String, PreApiTagDefinition> tagMapBuilder = ImmutableMap.builder();
    	tagManager.getTagLibrary().forEach(tagset -> {
    		tagsetMapBuilder.put(tagset.getUuid(), new PreApiTagsetDefinition(tagset.getUuid(), tagset.getName(), tagset.getDescription(), tagset.getResponsibleUser()));
    		for (TagDefinition tag : tagset) {
    			tagMapBuilder.put(
    					tag.getUuid(), 
    					new PreApiTagDefinition(
    							tag.getUuid(), 
    							tag.getParentUuid(), 
    							tag.getName(),
								tag.getSystemPropertyDefinitions().stream()
										// PropertyDefinitions never store a value for catma_markuptimestamp
										.filter(pd -> !pd.getName().equals(PropertyDefinition.SystemPropertyName.catma_markuptimestamp.name()))
										.collect(Collectors.toMap(
												pd -> {
													if (pd.getName().equals(PropertyDefinition.SystemPropertyName.catma_displaycolor.name())) {
														return "color";
													}
													else if (pd.getName().equals(PropertyDefinition.SystemPropertyName.catma_markupauthor.name())) {
														return "author";
													}
													return pd.getName();
												},
												pd -> {
													if (pd.getName().equals(PropertyDefinition.SystemPropertyName.catma_displaycolor.name())) {
														return TagDefinition.getHexColor(pd.getFirstValue());
													}
													return pd.getFirstValue();
												}
										)),
    							tag.getUserDefinedPropertyDefinitions()
    								.stream()
		                			.map(pd -> new PreApiUserPropertyDefinition(
		                					pd.getUuid(), 
		                					pd.getName(), 
		                					Collections.unmodifiableList(pd.getPossibleValueList()))).collect(Collectors.toList()),
								tag.getTagsetDefinitionUuid()
						)
				);
    		}
    	});
    	
    	return new ExtendedMetadata(documentMapBuilder.build(), collectionMapBuilder.build(), tagsetMapBuilder.build(), tagMapBuilder.build()); 
		
	}

	// TODO: move to a more general package
	@FunctionalInterface
	private interface CheckedFunction<T, R, E extends Exception> {
		R apply(T t) throws E;
	}

    public String serializeProjectResources(boolean includeExtendedMetadata, int page, int pageSize) {
    	Lock readLock = accessLock.readLock();
        try {
        	readLock.lock();
        	
        	final ExtendedMetadata extendedMetadata = includeExtendedMetadata?createExtendendMetadata():null;
        	if (page<1) {
        		page = 1;
        	}
        	if (pageSize<1) {
        		pageSize = DEFAULT_PAGE_SIZE;
        	}
        	
			// this CheckedFunction and the template CacheKey simply avoid some repetition later
			CheckedFunction<AnnotationCollectionReference, Integer, Exception> annotationCountCacheLoaderFn =
					(annotationCollectionRef) -> graphProjectHandler.getAnnotationCollection(annotationCollectionRef).getSize();

			CacheKey templateAnnotationCountCacheKey = new CacheKey(userName, namespace, projectId, null, gitProjectHandler.getRootRevisionHash());

			List<SourceDocumentReference> sourceDocumentRefs = graphProjectHandler.getSourceDocumentReferences().stream().sorted(
					Comparator.comparing(SourceDocumentReference::getUuid)
			).toList();

			// compute total no. of annotations for pagination info, also pre-populates annotationCountCache
			int totalAnnotationsCount = 0;
			for (SourceDocumentReference sourceDocumentRef : sourceDocumentRefs) {
				for (AnnotationCollectionReference annotationCollectionRef : sourceDocumentRef.getUserMarkupCollectionRefs().stream().toList()) {
					totalAnnotationsCount += annotationCountCache.get(
							templateAnnotationCountCacheKey.setCollectionId(annotationCollectionRef.getId()),
							() -> annotationCountCacheLoaderFn.apply(annotationCollectionRef)
					);
				}
			}

			int totalPagesCount = Math.ceilDiv(totalAnnotationsCount, pageSize);
			String currentUrl = String.format( // TODO: get this from the request context, as only query parameters change
					"%s%s/%s/project/%s/%s",
					CATMAPropertyKey.API_BASE_URL.getValue(),
					PreApplication.API_PACKAGE,
					PreApplication.API_VERSION,
					getNamespace(),
					getProjectId()
			);
			UriBuilder uriBuilder = UriBuilder.fromUri(currentUrl)
					.queryParam("includeExtendedMetadata", includeExtendedMetadata)
					.queryParam("page", page) // included here purely to preserve the ordering of parameters
					.queryParam("pageSize", pageSize);
			String prevPageUrl = page == 1 ? null : uriBuilder
					.replaceQueryParam("page", page - 1)
					.build().toString();
			String nextPageUrl = page >= totalPagesCount ? null : uriBuilder
					.replaceQueryParam("page", page + 1)
					.build().toString();
        	
        	int pageCapacityLeft = pageSize;
        	int startAnnotationCount = (page-1)*pageSize;
        	int processedAnnotationsCount = 0;
        	
            Builder<ExportDocument> documentListBuilder = ImmutableList.builder();
            
            
            for (SourceDocumentReference sourceDocumentReference : sourceDocumentRefs) {
            	
            	SourceDocument sourceDocument = graphProjectHandler.getSourceDocument(sourceDocumentReference.getUuid());
            	
                for (AnnotationCollectionReference annotationCollectionReference : sourceDocumentReference.getUserMarkupCollectionRefs().stream().sorted(Comparator.comparing(AnnotationCollectionReference::getId)).toList()) {
                	int annotationCount = annotationCountCache.get(
                			templateAnnotationCountCacheKey.setCollectionId(annotationCollectionReference.getId()),
                			() -> annotationCountCacheLoaderFn.apply(annotationCollectionReference));
                	                	
                	// did we reach the collection where we start to include annotations?
                	if (processedAnnotationsCount+annotationCount >= startAnnotationCount) {
                		// if the last page already included annotations of this collection we need to skip
                		int skip = Math.max(startAnnotationCount-processedAnnotationsCount, 0);
                		
                		AnnotationCollection annotationCollection = graphProjectHandler.getAnnotationCollection(annotationCollectionReference);
                		List<PreApiAnnotation> annotations = 
	                        annotationCollection.getTagReferences()
	                        .stream()
	                        .sorted(TAG_REFERENCE_COMPARATOR)
	                        .map(TagReference::getTagInstance)
	                        .distinct() // to compute skip and limit we only need one tag instance per corresponding reference
	                        .skip(skip)
	                        .limit(pageCapacityLeft)
	                        .map(tagInstance -> toPreApiAnnotation(tagInstance, annotationCollection, sourceDocument))
	                        .toList();
                		
                		pageCapacityLeft -= annotations.size();
                		
                		ExportDocument exportDocument = new ExportDocument( // TODO: this is duplicating documents, move outside collections loop
                				sourceDocument.getUuid(),
                				sourceDocument.toString(),
                				annotations
                		);
                		
                		documentListBuilder.add(exportDocument);
                	}
                	
                	processedAnnotationsCount += annotationCount;
                	
                	if (pageCapacityLeft <= 0) {
                		break;
                	}
                }
            	
            	if (pageCapacityLeft <= 0) {
            		break;
            	}
            }

            return new SerializationHelper<Export>().serialize(
                    new Export(totalPagesCount, page, pageSize, prevPageUrl, nextPageUrl, extendedMetadata, documentListBuilder.build())
            );
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to serialize project resources", e);
            return "{\"error\": \"Failed to serialize project resources, please contact CATMA support\"}";
        }
        finally {
        	readLock.unlock();
        }
    }

	private PreApiAnnotation toPreApiAnnotation(TagInstance tagInstance, AnnotationCollection annotationCollection, SourceDocument sourceDocument) {
    	List<Range> ranges = Range.mergeRanges(annotationCollection.getTagReferences(tagInstance).stream().map(TagReference::getRange).sorted());
    	TagDefinition tag = tagManager.getTagLibrary().getTagDefinition(tagInstance.getTagDefinitionId());
    	return new PreApiAnnotation(
    			tagInstance.getUuid(),
    			ranges.stream()
    				.map(range -> {
    					try {
    						return new PreApiAnnotatedPhrase(range.getStartPoint(), range.getEndPoint(), sourceDocument.getContent(range));
    					}
    					catch (IOException e) {
                            logger.log(Level.WARNING, String.format("Error serializing TagInstance: %s", tagInstance), e);
    						return null;
    					}
    				})
    				.toList(),
    			annotationCollection.getUuid(),
    			annotationCollection.getName(),
    			tag.getUuid(),
    			tag.getName(),
    			tag.getHexColor(),
    			annotationCollection.getAnnotation(tagInstance.getUuid()).getTagPath(),
    			tagInstance.getAuthor(),
    			ZonedDateTime.parse(tagInstance.getTimestamp(), DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
    			tagInstance.getUserDefinedProperties().stream()
    				.map((p) -> 
    					new PreApiAnnotationProperty(
    						p.getPropertyDefinitionId(),
    						tag.getUserDefinedPropertyDefinitions().stream()
    							.filter(pd -> pd.getUuid().equals(p.getPropertyDefinitionId())).findFirst().get().getName(),
    						p.getPropertyValueList()
    					)
    				).toList()
                );
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
