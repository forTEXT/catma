package de.catma.repository.git.graph.gcg;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Multimap;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.repository.git.graph.GraphProjectHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;

public class GcGraphProjectHandler implements GraphProjectHandler {

	@Override
	public void ensureProjectRevisionIsLoaded(ExecutionListener<TagManager> openProjectListener,
			ProgressListener progressListener, String revisionHash, TagManager tagManager,
			Supplier<List<TagsetDefinition>> tagsetsSupplier, Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier, boolean forceGraphReload, BackgroundService backgroundService)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document,
			Path tokenizedSourceDocumentPath) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSourceDocument(String rootRevisionHash, SourceDocumentReference sourceDocument, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<SourceDocument> getDocuments(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SourceDocument getSourceDocument(String rootRevisionHash, String sourceDocumentId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCollection(String rootRevisionHash, String collectionId, String name, SourceDocumentReference document,
			TagLibrary tagLibrary, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<TagsetDefinition> getTagsets(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addPropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition, TagDefinition tag,
			TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void createOrUpdatePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public AnnotationCollection getCollection(String rootRevisionHash,
			AnnotationCollectionReference collectionReference) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTagReferences(String rootRevisionHash, AnnotationCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagReferences(String rootRevisionHash, AnnotationCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeProperties(String rootRevisionHash, String collectionId, String propertyDefId) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProperties(String rootRevisionHash, AnnotationCollection collection, TagInstance tagInstance,
			Collection<Property> properties) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(String rootRevisionHash, TagDefinition tag)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(String rootRevisionHash,
			PropertyDefinition propertyDefinition, TagDefinition tag) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTagInstances(String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCollection(String rootRevisionHash, AnnotationCollectionReference collectionRef,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeDocument(String rootRevisionHash, SourceDocumentReference document, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasDocument(String rootRevisionHash, String documentId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(String rootRevisionHash,
			TagsetDefinition tagsetDefinition) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateProject(String oldRootRevisionHash, String rootRevisionHash) throws IOException {
		// TODO Auto-generated method stub

	}

}
