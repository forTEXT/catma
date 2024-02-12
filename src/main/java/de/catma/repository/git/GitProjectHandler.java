package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.SourceDocumentReference;
import de.catma.indexer.TermInfo;
import de.catma.project.CommitInfo;
import de.catma.project.MergeRequestInfo;
import de.catma.project.ProjectReference;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.resource.provider.LatestContribution;
import de.catma.repository.git.resource.provider.SynchronizedResourceProvider;
import de.catma.repository.git.resource.provider.interfaces.GitProjectResourceProvider;
import de.catma.repository.git.resource.provider.interfaces.GitProjectResourceProviderFactory;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.user.Member;
import de.catma.user.SharedGroup;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class GitProjectHandler {
	public static final String TAGSETS_DIRECTORY_NAME = "tagsets";
	public static final String ANNOTATION_COLLECTIONS_DIRECTORY_NAME = "collections";
	public static final String DOCUMENTS_DIRECTORY_NAME = "documents";

	private final Logger logger = Logger.getLogger(GitProjectHandler.class.getName());

	private final User user;
	private final ProjectReference projectReference;
	private final File projectPath;
	private final String projectId;
	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final RemoteGitManagerRestricted remoteGitServerManager;

	private final IDGenerator idGenerator;
	private final JGitCredentialsManager jGitCredentialsManager;
	private GitProjectResourceProvider resourceProvider;

	public GitProjectHandler(
			User user,
			ProjectReference projectReference,
			File projectPath,
			LocalGitRepositoryManager localGitRepositoryManager,
			RemoteGitManagerRestricted remoteGitServerManager
	) {
		this.user = user;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.projectId = projectReference.getProjectId();
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;

		this.idGenerator = new IDGenerator();
		this.jGitCredentialsManager = new JGitCredentialsManager(this.remoteGitServerManager);
		this.resourceProvider = new SynchronizedResourceProvider(
				this.projectId,
				this.projectReference,
				this.projectPath,
				this.localGitRepositoryManager,
				this.remoteGitServerManager,
				this.jGitCredentialsManager
		);
	}

	// tagset & tag operations
	public List<TagsetDefinition> getTagsets() {
		return resourceProvider.getTagsets();
	}

	public String createTagset(String tagsetId, String name, String description, String forkedFromCommitURL) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			File tagsetDirectory = Paths.get(
					projectPath.getAbsolutePath(),
					TAGSETS_DIRECTORY_NAME,
					tagsetId
			).toFile();

			// create the tagset
			String projectRevisionHash = gitTagsetHandler.create(
					tagsetDirectory,
					tagsetId,
					name,
					description,
					forkedFromCommitURL
			);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevisionHash;
		}
	}

	public String updateTagset(TagsetDefinition tagsetDefinition) throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitTagsetHandler.updateTagsetDefinition(tagsetDefinition);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String removeTagset(TagsetDefinition tagsetDefinition, Multimap<String, TagInstance> affectedTagInstancesByCollectionId) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : affectedTagInstancesByCollectionId.keySet()) {
				removeTagInstances(collectionId, affectedTagInstancesByCollectionId.get(collectionId));
				addCollectionToStaged(collectionId);
			}

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
			String projectRevision = gitTagsetHandler.removeTagsetDefinition(tagsetDefinition);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String createOrUpdateTag(String tagsetId, TagDefinition tagDefinition, String commitMsg) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			if (tagDefinition.getPropertyDefinition(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()) == null) {
				PropertyDefinition authorPropertyDefinition = new PropertyDefinition(
						idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()),
						PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
						Collections.singleton(user.getIdentifier())
				);
				tagDefinition.addSystemPropertyDefinition(authorPropertyDefinition);
			}

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitTagsetHandler.createOrUpdateTagDefinition(tagsetId, tagDefinition, commitMsg);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String removeTagAndAnnotations(TagDefinition tagDefinition, Multimap<String, TagInstance> tagInstancesByCollectionId) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : tagInstancesByCollectionId.keySet()) {
				removeTagInstances(collectionId, tagInstancesByCollectionId.get(collectionId));
				addCollectionToStaged(collectionId);
			}

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
			String projectRevision = gitTagsetHandler.removeTagDefinition(tagDefinition);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String removePropertyDefinition(
			PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition,
			Set<String> affectedCollectionIds
	) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : affectedCollectionIds) {
				addCollectionToStaged(collectionId);
			}

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitTagsetHandler.removePropertyDefinition(tagsetDefinition, tagDefinition, propertyDefinition);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	// collection operations
	public List<AnnotationCollectionReference> getCollectionReferences() {
		return resourceProvider.getCollectionReferences();
	}

	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary,
			ProgressListener progressListener,
			boolean withOrphansHandling
	) throws IOException {
		return resourceProvider.getCollections(tagLibrary, progressListener, withOrphansHandling);
	}

	public AnnotationCollection getCollection(String collectionId, TagLibrary tagLibrary) throws IOException {
		return resourceProvider.getCollection(collectionId, tagLibrary);
	}

	public String createAnnotationCollection(
			String collectionId,
			String name,
			String description,
			String sourceDocumentId,
			String forkedFromCommitURL,
			boolean withPush
	) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
					localGitRepoManager,
					projectPath,
					projectId,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			File collectionDirectory = Paths.get(
					projectPath.getAbsolutePath(),
					ANNOTATION_COLLECTIONS_DIRECTORY_NAME,
					collectionId
			).toFile();

			// create the collection
			String projectRevisionHash = gitAnnotationCollectionHandler.create(
					collectionDirectory,
					collectionId,
					name,
					description,
					sourceDocumentId,
					forkedFromCommitURL
			);

			if (withPush) {
				localGitRepoManager.push(jGitCredentialsManager);
			}

			return projectRevisionHash;
		}
	}

	public String updateCollection(AnnotationCollectionReference annotationCollectionReference) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
					localGitRepoManager,
					projectPath,
					projectId,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitAnnotationCollectionHandler.updateCollection(annotationCollectionReference);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String removeCollection(AnnotationCollectionReference annotationCollectionReference) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
					localGitRepoManager,
					projectPath,
					projectId,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitAnnotationCollectionHandler.removeCollection(annotationCollectionReference);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public void addTagReferencesToCollection(String collectionId, Collection<TagReference> tagReferences, TagLibrary tagLibrary) throws IOException {
		GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
				localGitRepositoryManager,
				projectPath,
				projectId,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		Multimap<TagInstance, TagReference> tagReferencesByTagInstance = Multimaps.index(tagReferences, TagReference::getTagInstance);

		List<Pair<JsonLdWebAnnotation, TagInstance>> annotationTagInstanceMap = Lists.newArrayList(); // TODO: can we use a map here?

		for (TagInstance tagInstance : tagReferencesByTagInstance.keySet()) {
			Collection<TagReference> tagReferencesForTagInstance = tagReferencesByTagInstance.get(tagInstance);
			JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
					tagReferencesForTagInstance,
					tagLibrary,
					tagInstance.getPageFilename()
			);
			annotationTagInstanceMap.add(new Pair<>(annotation, tagInstance));
		}

		gitAnnotationCollectionHandler.createTagInstances(collectionId, annotationTagInstanceMap);
	}

	public void updateTagInstance(
			String collectionId,
			TagInstance tagInstance,
			Collection<TagReference> tagReferences,
			TagLibrary tagLibrary
	) throws IOException {
		GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
				localGitRepositoryManager,
				projectPath,
				projectId,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
				tagReferences,
				tagLibrary,
				tagInstance.getPageFilename()
		);

		if (tagInstance.getPageFilename() == null) {
			throw new IOException(
					String.format(
							"Tag instance with ID %s for collection with ID %s has a null page filename!",
							tagInstance.getUuid(),
							collectionId
					)
			);
		}

		gitAnnotationCollectionHandler.updateTagInstance(collectionId, annotation);
	}

	public void removeTagInstances(String collectionId, Collection<TagInstance> deletedTagInstances) throws IOException {
		GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
				localGitRepositoryManager,
				projectPath,
				projectId,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		gitAnnotationCollectionHandler.removeTagInstances(collectionId, deletedTagInstances);
	}

	private void addCollectionToStaged(String collectionId) throws IOException {
		Path relativePath = Paths.get(ANNOTATION_COLLECTIONS_DIRECTORY_NAME, collectionId);
		localGitRepositoryManager.add(relativePath.toFile());
	}

	public String addCollectionsToStagedAndCommit(
			Set<String> collectionIds,
			String commitMsg,
			boolean force,
			boolean withPush
	) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : collectionIds) {
				addCollectionToStaged(collectionId);
			}

			String projectRevision = localGitRepoManager.commit(
					commitMsg,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					force
			);

			if (withPush) {
				localGitRepoManager.push(jGitCredentialsManager);
			}

			return projectRevision;
		}
	}

	/**
	 * Checks for collections that are for documents that were deleted by another user.
	 * Any orphaned collections are deleted too.
	 */
	public void verifyCollections() throws IOException {
		List<AnnotationCollectionReference> collectionRefs = getCollectionReferences();
		Set<String> documentIds = getDocuments().stream().map(SourceDocument::getUuid).collect(Collectors.toSet());

		Set<AnnotationCollectionReference> staleCollectionCandidates = new HashSet<>();

		for (AnnotationCollectionReference collectionRef : collectionRefs) {
			String documentId = collectionRef.getSourceDocumentId();
			if (!documentIds.contains(documentId)) {
				staleCollectionCandidates.add(collectionRef);
			}
		}

		if (staleCollectionCandidates.isEmpty()) {
			return;
		}

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			Set<String> verifiedDeletedDocuments = localGitRepoManager.verifyDeletedResourcesViaLog(
					DOCUMENTS_DIRECTORY_NAME,
					"document",
					staleCollectionCandidates.stream()
							.map(AnnotationCollectionReference::getSourceDocumentId)
							.collect(Collectors.toSet())
			);

			// removeCollection below calls open, so we need to detach
			localGitRepoManager.detach();

			Set<AnnotationCollectionReference> collectionsToDelete = collectionRefs.stream()
					.filter(collectionRef -> verifiedDeletedDocuments.contains(collectionRef.getSourceDocumentId()))
					.collect(Collectors.toSet());

			for (AnnotationCollectionReference collectionRef : collectionsToDelete) {
				logger.info(
						String.format(
								"Deleting stale collection \"%1$s\" with ID %2$s due to deletion of corresponding document with ID %3$s",
								collectionRef.getName(),
								collectionRef.getId(),
								collectionRef.getSourceDocumentId()
						)
				);
				removeCollection(collectionRef);
			}
		}
	}

	// document operations
	public List<SourceDocument> getDocuments() {
		return resourceProvider.getDocuments();
	}

	public SourceDocument getDocument(String documentId) throws IOException {
		return resourceProvider.getDocument(documentId);
	}

	public Map getDocumentIndex(String documentId) throws IOException {
		return resourceProvider.getDocumentIndex(documentId);
	}

	/**
	 * Creates a new document within the project.
	 *
	 * @param documentId ID of the document to create
	 * @param originalSourceDocumentStream an {@link InputStream} for the original, unmodified document
	 * @param originalSourceDocumentFileName file name of the original, unmodified document
	 * @param convertedSourceDocumentStream an {@link InputStream} for the converted, UTF-8 encoded document
	 * @param convertedSourceDocumentFileName file name of the converted, UTF-8 encoded document
	 * @param terms the collection of terms extracted from the converted document
	 * @param tokenizedSourceDocumentFileName name of the file within which the terms/tokens should be stored in serialized form
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object
	 * @return the revision hash
	 * @throws IOException if an error occurs when creating the document
	 */
	public String createSourceDocument(
			String documentId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			Map<String, List<TermInfo>> terms, String tokenizedSourceDocumentFileName,
			SourceDocumentInfo sourceDocumentInfo
	) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					localGitRepoManager, 
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			File documentDirectory = Paths.get(
					projectPath.getAbsolutePath(),
					DOCUMENTS_DIRECTORY_NAME,
					documentId
			).toFile();

			sourceDocumentInfo.getTechInfoSet().setResponsibleUser(user.getIdentifier());

			// create the document within the project
			String projectRevision = gitSourceDocumentHandler.create(
					documentDirectory, documentId,
					originalSourceDocumentStream, originalSourceDocumentFileName,
					convertedSourceDocumentStream, convertedSourceDocumentFileName,
					terms, tokenizedSourceDocumentFileName,
					sourceDocumentInfo
			);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String updateSourceDocument(SourceDocumentReference sourceDocumentReference) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitSourceDocumentHandler.update(sourceDocumentReference);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public String removeDocument(SourceDocumentReference sourceDocumentReference) throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
					localGitRepoManager,
					projectPath,
					projectId,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			for (AnnotationCollectionReference collectionRef : sourceDocumentReference.getUserMarkupCollectionRefs()) {
				gitAnnotationCollectionHandler.removeCollectionWithoutCommit(collectionRef);
			}

			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					localGitRepoManager,
					projectPath,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);

			String projectRevision = gitSourceDocumentHandler.removeDocument(sourceDocumentReference);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	// comment operations
	public List<Comment> getComments(String documentId) throws IOException {
		return remoteGitServerManager.getComments(projectReference, documentId);
	}

	public List<Reply> getCommentReplies(Comment comment) throws IOException {
		return remoteGitServerManager.getCommentReplies(projectReference, comment);
	}

	public List<Comment> getCommentsWithReplies(List<String> documentIds) throws IOException {
		List<Comment> comments = new ArrayList<>();

		for (String documentId : documentIds) {
			comments.addAll(remoteGitServerManager.getComments(projectReference, documentId));
		}

		for (Comment comment : comments) {
			if (comment.getReplyCount() > 0) {
				getCommentReplies(comment);
			}
		}

		return comments;
	}

	public void addComment(Comment comment) throws IOException {
		remoteGitServerManager.addComment(projectReference, comment);
	}

	public void updateComment(Comment comment) throws IOException {
		remoteGitServerManager.updateComment(projectReference, comment);
	}

	public void removeComment(Comment comment) throws IOException {
		remoteGitServerManager.removeComment(projectReference, comment);
	}

	public void addReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.addReply(projectReference, comment, reply);
	}

	public void updateReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.updateReply(projectReference, comment, reply);
	}

	public void removeReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.removeReply(projectReference, comment, reply);
	}

	// member, role and permissions related things
	public Set<Member> getProjectMembers() throws IOException {
		return remoteGitServerManager.getProjectMembers(projectReference);
	}

	public boolean hasPermission(RBACRole role, RBACPermission permission) {
		return remoteGitServerManager.hasPermission(role, permission);
	}

	public RBACRole getRoleOnProject(RBACSubject subject) throws IOException {
		return remoteGitServerManager.getRoleOnProject(subject, projectReference);
	}

	public RBACSubject assignOnProject(RBACSubject subject, RBACRole role, LocalDate expiresAt) throws IOException {
		return remoteGitServerManager.assignOnProject(subject, role, projectReference, expiresAt);
	}
	
	public SharedGroup assignOnProject(SharedGroup group, RBACRole role, LocalDate expiresAt, boolean reassign) throws IOException {
		return remoteGitServerManager.assignOnProject(group, role, projectReference, expiresAt, reassign);
	}


	public void unassignFromProject(RBACSubject subject) throws IOException {
		remoteGitServerManager.unassignFromProject(subject, projectReference);
	}
	

	public void unassignFromProject(SharedGroup sharedGroup) throws IOException {
		remoteGitServerManager.unassignFromProject(sharedGroup, projectReference);
	}


	// synchronization related things
	public boolean isReadOnly() {
		return resourceProvider.isReadOnly();
	}

	public void setResourceProvider(GitProjectResourceProviderFactory resourceProviderFactory) {
		resourceProvider = resourceProviderFactory.createResourceProvider(
				projectId,
				projectReference,
				projectPath,
				localGitRepositoryManager,
				remoteGitServerManager,
				jGitCredentialsManager
		);
	}

	public void ensureUserBranch() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), true);
		}
	}

	public String getRootRevisionHash() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			return localGitRepoManager.getRevisionHash();
		}
	}

	public Status getStatus() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			return localGitRepoManager.getStatus();
		}
	}

	public boolean hasUntrackedChanges() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			return localGitRepoManager.hasUntrackedChanges();
		}
	}

	public boolean hasUncommittedChanges() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			return localGitRepoManager.hasUncommittedChanges();
		}
	}

	public boolean hasConflicts() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			Status projectStatus = localGitRepoManager.getStatus();

			if (!projectStatus.getConflicting().isEmpty()) {
				StringBuilder builder = new StringBuilder();
				StatusPrinter.print("Project #" + projectId, projectStatus, builder);
				logger.warning(builder.toString());
				return true;
			}
		}

		return false;
	}

	public String commitAndPushProject(String commitMessage) throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			if (!localGitRepoManager.hasUncommittedChanges() && !localGitRepoManager.hasUntrackedChanges()) {
				return localGitRepoManager.getRevisionHash();
			}

			String projectRevision = localGitRepoManager.addAllAndCommit(
					commitMessage,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					false
			);

			localGitRepoManager.push(jGitCredentialsManager);

			return projectRevision;
		}
	}

	public boolean synchronizeWithRemote() throws IOException {
		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			boolean pushedAlready = false;

			// if there are uncommitted changes we perform an auto commit and push
			if (localGitRepoManager.hasUncommittedChanges() || localGitRepoManager.hasUntrackedChanges()) {
				localGitRepoManager.addAllAndCommit(
						"Auto-committing changes before synchronization",
						remoteGitServerManager.getUsername(),
						remoteGitServerManager.getEmail(),
						false
				);
				localGitRepoManager.push(jGitCredentialsManager);
				pushedAlready = true;
			}

			// fetch latest commits
			// we are interested in updating the local origin/master here
			localGitRepoManager.fetch(jGitCredentialsManager);

			// do we have commits in the user branch that are not present in the master branch? 
			// userBranch -> origin/master
			List<CommitInfo> ourUnpublishedChanges = localGitRepoManager.getOurUnpublishedChanges();

			if (!ourUnpublishedChanges.isEmpty()) {
				logger.info(
						String.format(
								"Commits that need to be merged from \"%s\" -> origin/master:\n%s",
								user.getIdentifier(),
								ourUnpublishedChanges
										.stream()
										.map(CommitInfo::toString)
										.collect(Collectors.joining("\n"))
						)
				);

				// make sure everything is pushed
				if (!pushedAlready) {
					localGitRepoManager.push(jGitCredentialsManager);
					pushedAlready = true;
				}

				// check for existing merge requests origin/userBranch -> origin/master
				List<MergeRequestInfo> openMergeRequests = remoteGitServerManager.getOpenMergeRequests(projectReference);

				if (openMergeRequests.isEmpty()) {
					// create and merge a merge request origin/userBranch -> origin/master
					MergeRequestInfo mergeRequestInfo = remoteGitServerManager.createMergeRequest(projectReference);
					mergeRequestInfo = refreshMergeRequestInfo(mergeRequestInfo);
					logger.info(String.format("Created %s", mergeRequestInfo));

					// merge_status can be 'unchecked', 'checking', 'can_be_merged', 'cannot_be_merged' or 'cannot_be_merged_recheck'
					// TODO: merge_status is deprecated, use detailed_merge_status instead
					if (!mergeRequestInfo.canBeMerged()) {
						return false;
					}

					MergeRequestInfo result = remoteGitServerManager.mergeMergeRequest(mergeRequestInfo);
					logger.info(String.format("Attempted merge of %s", result));

					if (!result.isMerged()) {
						return false;
					}
				}
				else {
					logger.info(
						String.format(
							"Existing merge requests for origin/%s -> origin/master:\n%s",
							user.getIdentifier(),
							openMergeRequests
									.stream()
									.map(MergeRequestInfo::toString)
									.collect(Collectors.joining("\n"))
						)
					);

					// merge all open merge requests origin/userBranch -> origin/master
					for (MergeRequestInfo mergeRequestInfo : openMergeRequests) {
						mergeRequestInfo = refreshMergeRequestInfo(mergeRequestInfo);

						// state can be 'opened', 'closed', 'merged' or 'locked'
						if (!mergeRequestInfo.isOpen()) {
							continue; // assume 'closed' or 'merged' ('locked' is apparently a transitive state that we hopefully won't have to deal with)
						}

						// merge_status can be 'unchecked', 'checking', 'can_be_merged', 'cannot_be_merged' or 'cannot_be_merged_recheck'
						// TODO: merge_status is deprecated, use detailed_merge_status instead
						if (!mergeRequestInfo.canBeMerged()) {
							return false;
						}

						MergeRequestInfo result = remoteGitServerManager.mergeMergeRequest(mergeRequestInfo);
						logger.info(String.format("Attempted merge of %s", result));

						if (!result.isMerged()) {
							return false;
						}
					}
				}
			}

			// fetch latest commits
			// we are interested in updating the local origin/master here
			localGitRepoManager.fetch(jGitCredentialsManager);

			// get commits that need to be merged into the local user branch
			// origin/master -> userBranch
			List<CommitInfo> theirPublishedChanges = localGitRepoManager.getTheirPublishedChanges();

			if (!theirPublishedChanges.isEmpty()) {
				logger.info(
						String.format(
								"Commits that need to be merged from origin/master -> \"%s\":\n%s",
								user.getIdentifier(),
								theirPublishedChanges
										.stream()
										.map(CommitInfo::toString)
										.collect(Collectors.joining("\n"))
						)
				);

				// can we merge origin/master -> userBranch?
				if (localGitRepoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)) {
					MergeResult mergeWithOriginMasterResult = localGitRepoManager.merge(
							Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER
					);

					// push if merge was successful, otherwise abort the merge
					if (mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
						localGitRepoManager.push(jGitCredentialsManager);
					}
					else {
						localGitRepoManager.abortMerge();
						return false;
					}
				}
				else {
					return false;
				}
			}

			return true;
		}
	}

	private MergeRequestInfo refreshMergeRequestInfo(MergeRequestInfo mergeRequestInfo) throws IOException {
		int tries = 10;
		while (tries > 0 && mergeRequestInfo.isMergeStatusCheckInProgress()) {
			try {
				Thread.sleep(1000);
				mergeRequestInfo = remoteGitServerManager.getMergeRequest(projectReference, mergeRequestInfo.getIid());
				tries--;
			}
			catch (InterruptedException e) {
				break;
			}
		}

		return mergeRequestInfo;
	}

	public Set<LatestContribution> getLatestContributions(List<String> branches) throws IOException {
		Set<LatestContribution> latestContributions = new HashSet<>();

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.fetch(jGitCredentialsManager);

			List<String> availableBranches = localGitRepoManager.getRemoteBranches();
			List<String> filteredBranches = branches.stream().filter(availableBranches::contains).collect(Collectors.toList());

			for (String branch : filteredBranches) {
				LatestContribution latestContribution = new LatestContribution(branch);

				Set<String> modifiedOrAddedPaths = localGitRepoManager.getAdditiveBranchDifferences(branch);

				for (String path : modifiedOrAddedPaths) {
					// we are only interested in new or modified annotations, hence the 2nd condition
					if (path.startsWith(ANNOTATION_COLLECTIONS_DIRECTORY_NAME) && path.contains(GitAnnotationCollectionHandler.ANNNOTATIONS_DIR)) {
						latestContribution.addCollectionId(
								path.substring(
										ANNOTATION_COLLECTIONS_DIRECTORY_NAME.length() + 1,
										path.indexOf('/', ANNOTATION_COLLECTIONS_DIRECTORY_NAME.length() + 1)
								)
						);
					}
					else if (path.startsWith(DOCUMENTS_DIRECTORY_NAME)) {
						latestContribution.addDocumentId(
								path.substring(
										DOCUMENTS_DIRECTORY_NAME.length() + 1,
										path.indexOf('/', DOCUMENTS_DIRECTORY_NAME.length() + 1)
								)
						);
					}
					// we are only interested in new or modified tags, hence the 2nd condition
					else if (path.startsWith(TAGSETS_DIRECTORY_NAME) && path.contains("propertydefs")) {
						latestContribution.addTagsetId(
								path.substring(
										TAGSETS_DIRECTORY_NAME.length() + 1,
										path.indexOf('/', TAGSETS_DIRECTORY_NAME.length() + 1)
								)
						);
					}
				}

				if (!latestContribution.isEmpty()) {
					latestContributions.add(latestContribution);
				}
			}
		}

		return latestContributions;
	}
}
