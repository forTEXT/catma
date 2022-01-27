package de.catma.repository.git;

import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.project.conflict.*;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Deprecated
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
		// return gitProjectHandler.getTagsetConflicts();
		return null;
	}

	@Override
	public List<CollectionConflict> getCollectionConflicts() throws Exception {
		return gitProjectHandler.getCollectionConflicts();
	}

	@Override
	public List<SourceDocumentConflict> getSourceDocumentConflicts() throws Exception {
		return gitProjectHandler.getSourceDocumentConflicts();
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
			if (!collectionConflict.getAnnotationConflicts().isEmpty() || collectionConflict.isHeaderConflict()) {
				for (AnnotationConflict annotationConflict : collectionConflict.getAnnotationConflicts()) {
					gitProjectHandler.resolveAnnotationConflict(
							collectionConflict.getCollectionId(), annotationConflict, tagLibrary
					);
				}

				gitProjectHandler.addCollectionToStagedAndCommit(
						collectionConflict.getCollectionId(),
						"Auto-committing merged changes (GitConflictedProject.resolveCollectionConflict -> " +
								"GitProjectHandler.addCollectionToStagedAndCommit)",
						true
				);
			}

			gitProjectHandler.addCollectionSubmoduleToStagedAndCommit(
					collectionConflict.getCollectionId(),
					"Auto-committing merged changes (GitConflictedProject.resolveCollectionConflict -> " +
							"GitProjectHandler.addCollectionSubmoduleToStagedAndCommit)",
					false
			);

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

				// TODO: why does this function not have a 'force' param, like the equivalent functions for collections and documents?
//				gitProjectHandler.addTagsetToStagedAndCommit(
//						tagsetConflict.getUuid(),
//						"Auto-committing merged changes (GitConflictedProject.resolveTagsetConflicts -> " +
//								"GitProjectHandler.addTagsetToStagedAndCommit)"
//				);
			}

			// TODO: why does this function not have a 'force' param, like the equivalent functions for collections and documents?
			gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
					tagsetConflict.getUuid(),
					"Auto-committing merged changes (GitConflictedProject.resolveTagsetConflicts -> " +
							"GitProjectHandler.addTagsetSubmoduleToStagedAndCommit)"
			);

			gitProjectHandler.checkoutTagsetDevBranchAndRebase(tagsetConflict.getUuid());

			gitProjectHandler.synchronizeTagsetWithRemote(tagsetConflict.getUuid());
		}
	}

	@Override
	public void resolveSourceDocumentConflicts(List<SourceDocumentConflict> sourceDocumentConflicts) throws Exception {
		// for now there shouldn't be conflicts on anything other than the header file (nothing else about a document can currently be edited by users)
		// those are resolved/merged automatically when they're fetched, but need to commit and push here
		for (SourceDocumentConflict sourceDocumentConflict : sourceDocumentConflicts) {
			gitProjectHandler.addSourceDocumentToStagedAndCommit(
					sourceDocumentConflict.getSourceDocumentId(),
					"Auto-committing merged changes (GitConflictedProject.resolveSourceDocumentConflicts -> " +
							"GitProjectHandler.addSourceDocumentToStagedAndCommit)",
					true
			);

			gitProjectHandler.addSourceDocumentSubmoduleToStagedAndCommit(
					sourceDocumentConflict.getSourceDocumentId(),
					"Auto-committing merged changes (GitConflictedProject.resolveSourceDocumentConflicts -> " +
							"GitProjectHandler.addSourceDocumentSubmoduleToStagedAndCommit)",
					false
			);

			gitProjectHandler.checkoutSourceDocumentDevBranchAndRebase(sourceDocumentConflict.getSourceDocumentId());

			gitProjectHandler.synchronizeSourceDocumentWithRemote(sourceDocumentConflict.getSourceDocumentId());
		}
	}

	@Override
	public void resolveDeletedResourceConflicts(Collection<DeletedResourceConflict> deletedResourceConflicts) throws Exception {
		gitProjectHandler.resolveDeletedResourceConflicts(deletedResourceConflicts);
	}
	
	@Override
	public ProjectReference getProjectReference() {
		return projectReference;
	}
	
	@Override
	public Collection<DeletedResourceConflict> resolveRootConflicts() throws Exception {
		return gitProjectHandler.resolveRootConflicts();
	}
}
