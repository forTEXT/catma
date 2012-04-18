package de.catma.indexer.elasticsearch;

import java.util.List;
import java.util.UUID;

import de.catma.core.document.Range;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagLibrary;
import de.catma.core.util.IDGenerator;

public class ESTagReferenceDocument {

	private UUID tagReferenceId;
	private String documentId;
	private String userMarkupCollectionId;
	private UUID tagDefinitionId;
	private UUID tagInstanceId;
	private String tagPath;
	private List<ESTagProperty> properties;
	private Range range;
	
	public ESTagReferenceDocument(String sourceDocId,
			String userMarkupCollectionId, TagReference tagReference,
			TagLibrary tagLibrary) {
		this.documentId = sourceDocId;
		this.userMarkupCollectionId = userMarkupCollectionId;
		this.tagDefinitionId = catmaIDToUUID(tagReference.getTagDefinition().getID());
		this.tagInstanceId = catmaIDToUUID(tagReference.getTagInstanceID());
	}
	
	private UUID catmaIDToUUID(String catmastr){
		int index = catmastr.indexOf(IDGenerator.ID_PREFIX);
		return UUID.fromString(catmastr.substring(index));
	}
}
