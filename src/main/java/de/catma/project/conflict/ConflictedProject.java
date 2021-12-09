package de.catma.project.conflict;

import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

import java.util.Collection;
import java.util.List;

public interface ConflictedProject {
	List<TagsetConflict> getTagsetConflicts() throws Exception;
	List<CollectionConflict> getCollectionConflicts() throws Exception;
	List<SourceDocumentConflict> getSourceDocumentConflicts() throws Exception;
	Collection<TagsetDefinition> getTagsets() throws Exception;
	Collection<SourceDocument> getDocuments() throws Exception;
	void resolveCollectionConflict(List<CollectionConflict> conflictedCollections, TagLibrary tagLibrary) throws Exception;
	ProjectReference getProjectReference();
	Collection<DeletedResourceConflict> resolveRootConflicts() throws Exception;
	void resolveTagsetConflicts(List<TagsetConflict> tagsetConflicts) throws Exception;
	void resolveSourceDocumentConflicts(List<SourceDocumentConflict> sourceDocumentConflicts) throws Exception;
	void resolveDeletedResourceConflicts(Collection<DeletedResourceConflict> deletedReourceConflicts) throws Exception;
}
