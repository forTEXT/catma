package de.catma.repository.git.graph;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;

public class TPGraphProjectHandler implements GraphProjectHandler {

	
	
	
	@Override
	public void ensureProjectRevisionIsLoaded(String revisionHash, TagManager tagManager,
			Supplier<List<TagsetDefinition>> tagsetsSupplier, Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document,
			Path tokenizedSourceDocumentPath) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public int getSourceDocumentsCount(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return 0;
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
	public void addCollection(String rootRevisionHash, String collectionId, String name, String umcRevisionHash,
			SourceDocument document, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTagDefinition(String rootRevisionHash, TagDefinition tagDefinition, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTagDefinition(String rootRevisionHash, TagDefinition tagDefinition, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public List<UserMarkupCollectionReference> getCollectionReferences(String rootRevisionHash, int offset, int limit)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCollectionReferenceCount(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<TagsetDefinition> getTagsets(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTagsetsCount(String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addPropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void createOrUpdatePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public UserMarkupCollection getCollection(String rootRevisionHash, TagLibrary tagLibrary,
			UserMarkupCollectionReference collectionReference) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTagReferences(String rootRevisionHash, UserMarkupCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagReferences(String rootRevisionHash, UserMarkupCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeProperties(String rootRevisionHash, String collectionId, String collectionRevisionHash,
			String propertyDefId) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProperties(String rootRevisionHash, TagInstance tagInstance, Collection<Property> properties)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(String rootRevisionHash, TagDefinition tagDefinition)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(String rootRevisionHash,
			PropertyDefinition propertyDefinition, TagLibrary tagLibrary) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTagInstances(String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds,
			String collectionRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagDefinition(String rootRevisionHash, TagDefinition tagDefinition, TagsetDefinition tagset)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProjectRevisionHash(String oldRootRevisionHash, String rootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCollectionRevisionHash(String rootRevisionHash, UserMarkupCollectionReference collectionReference)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
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
	public void updateCollection(String rootRevisionHash, UserMarkupCollectionReference collectionRef,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeCollection(String rootRevisionHash, UserMarkupCollectionReference collectionReference,
			String oldRootRevisionHash) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeDocument(String rootRevisionHash, SourceDocument document, String oldRootRevisionHash)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
