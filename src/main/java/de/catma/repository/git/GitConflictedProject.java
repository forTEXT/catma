package de.catma.repository.git;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.project.TagsetConflict;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.ConflictedProject;
import de.catma.project.conflict.TagConflict;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class GitConflictedProject implements ConflictedProject {
	
	private GitProjectHandler gitProjectHandler;
	private Function<String, URI> documentIdTofileUriMapper;
	private ProjectReference projectReference;
	
	public GitConflictedProject(
			ProjectReference projectReference, 
			GitProjectHandler gitProjectHandler, 
			Function<String, URI> documentIdTofileUriMapper) {
		super();
		this.projectReference = projectReference;
		this.gitProjectHandler = gitProjectHandler;
		this.documentIdTofileUriMapper = documentIdTofileUriMapper;
	}

	@Override
	public List<TagsetConflict> getTagsetConflicts() throws Exception {
		return gitProjectHandler.getTagsetConflicts();
	}

	@Override
	public List<CollectionConflict> getCollectionConflicts() throws Exception {
		return gitProjectHandler.getCollectionConflicts();
	}

	@Override
	public Collection<TagsetDefinition> getTagsets() throws Exception {
		return gitProjectHandler.getTagsets();
	}

	@Override
	public Collection<SourceDocument> getDocuments() throws Exception {
		Collection<SourceDocument> documents = gitProjectHandler.getDocuments();
		documents.forEach(
			doc -> 
				doc
				.getSourceContentHandler()
				.getSourceDocumentInfo()
				.getTechInfoSet()
				.setURI(documentIdTofileUriMapper.apply(doc.getUuid())));
		
		return documents;
	}
	
	@Override
	public void resolveCollectionConflict(List<CollectionConflict> conflictedCollections, TagLibrary tagLibrary) throws Exception {
		for (CollectionConflict collectionConflict : conflictedCollections) {
			if (!collectionConflict.getAnnotationConflicts().isEmpty()) {
				for (AnnotationConflict annotationConflict : collectionConflict.getAnnotationConflicts()) {
					gitProjectHandler.resolveAnnotationConflict(
						collectionConflict.getCollectionId(), annotationConflict, tagLibrary);
				}
				gitProjectHandler.addCollectionToStagedAndCommit(
					collectionConflict.getCollectionId(), "Auto-committing merged changes", true);
				
			}
			gitProjectHandler.addCollectionSubmoduleToStagedAndCommit(
				collectionConflict.getCollectionId(), "Auto-committing merged changes", false);

			gitProjectHandler.checkoutCollectionDevBranchAndRebase(collectionConflict.getCollectionId());
			
			gitProjectHandler.synchronizeCollectionWithRemote(collectionConflict.getCollectionId());
		}
		
	}
	
	@Override
	public void resolveTagsetConflicts(List<TagsetConflict> tagsetConflicts) throws Exception {
		for (TagsetConflict tagsetConflict : tagsetConflicts) {
			if (!tagsetConflict.getTagConflicts().isEmpty() || tagsetConflict.isHeaderConflict()) {
				for (TagConflict tagConflict : tagsetConflict.getTagConflicts()) {
					gitProjectHandler.resolveTagConflict(tagsetConflict.getUuid(), tagConflict);
				}
				gitProjectHandler.addTagsetToStagedAndCommit(
						tagsetConflict.getUuid(), "Auto-committing merged changes");
			}
			
			gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
					tagsetConflict.getUuid(), "Auto-committing merged changes");
			
			gitProjectHandler.checkoutTagsetDevBranchAndRebase(tagsetConflict.getUuid());
			
			gitProjectHandler.synchronizeTagsetWithRemote(tagsetConflict.getUuid());

		}
		
	}
	
	@Override
	public ProjectReference getProjectReference() {
		return projectReference;
	}
	
	@Override
	public void resolveRootConflicts() throws Exception {
		gitProjectHandler.resolveRootConflicts();
		
	}
}
