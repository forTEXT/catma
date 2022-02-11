package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.collect.Multimap;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.indexer.TermInfo;
import de.catma.project.CommitInfo;
import de.catma.project.ProjectReference;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.DeletedResourceConflict;
import de.catma.project.conflict.Resolution;
import de.catma.project.conflict.SourceDocumentConflict;
import de.catma.project.conflict.TagConflict;
import de.catma.project.conflict.TagsetConflict;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.StatusPrinter;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class GitProjectHandler {

	private final Logger logger = Logger.getLogger(GitProjectHandler.class.getName());
	
	public static final String TAGSETS_DIRECTORY_NAME = "tagsets";
	public static final String ANNOTATION_COLLECTION_DIRECTORY_NAME = "collections";
	public static final String SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME = "documents";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final User user;
	private final String projectId;

	private final IDGenerator idGenerator = new IDGenerator();
	private final CredentialsProvider credentialsProvider;

	@Deprecated
	private Map<String, RBACRole> rolesPerResource;

	private final ProjectReference projectReference;
	private final File projectPath;
	
	public GitProjectHandler(
			User user, ProjectReference projectReference, 
			File projectPath,
			ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager) {
		super();
		this.user = user;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.projectId = projectReference.getProjectId();
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = new UsernamePasswordCredentialsProvider("oauth2", remoteGitServerManager.getPassword());
	}
	
	/**
	 * Loads the roles per resources
	 * @return true if we encountered changes and a graph reload is appropriate
	 * @throws Exception
	 */
	@Deprecated
	public boolean loadRolesPerResource() throws Exception {
		return false;
	}

	// Tagset operations
	public String createTagset(String tagsetId,
							   String name,
							   String description,
							   String forkedFromCommitURL) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
							localGitRepoManager, 
							this.projectPath,
							this.remoteGitServerManager.getUsername(),
							this.remoteGitServerManager.getEmail());
			
			File tagsetFolder = Paths.get(
					this.projectPath.getAbsolutePath(),
					TAGSETS_DIRECTORY_NAME,
					tagsetId
			).toFile();


			// create the tagset
			String projectRevisionHash = 
				gitTagsetHandler.create(
						tagsetFolder, tagsetId, name, description, forkedFromCommitURL);

			localGitRepoManager.push(credentialsProvider);


			return projectRevisionHash;
		}
	}
	
	@Deprecated
	public Pair<TagsetDefinition, String> cloneAndAddTagset(String tagsetId, String name, String commitMsg) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
							localGitRepoManager, 
							this.projectPath,
							this.remoteGitServerManager.getUsername(),
							this.remoteGitServerManager.getEmail());


			String tagsetRepoRemoteUrl = 
					CATMAPropertyKey.GitLabServerUrl.getValue() + "/" + projectId + "/" + tagsetId + ".git";

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					TAGSETS_DIRECTORY_NAME,
					tagsetId
			).toFile();

			// submodule files and the changed .gitmodules file are automatically staged
			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					tagsetRepoRemoteUrl,
					credentialsProvider
			);
			
			String rootRevisionHash = localGitRepoManager.commit(
					commitMsg,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					false);

			localGitRepoManager.detach(); 
			
//			gitTagsetHandler.checkout(
//				projectId, tagsetId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);

			if (!rolesPerResource.containsKey(tagsetId)) {
				rolesPerResource.put(tagsetId, RBACRole.OWNER);
			}

			return new Pair<>(gitTagsetHandler.getTagset(tagsetId), rootRevisionHash);
		}
	}
	
	public String createOrUpdateTag(
			String tagsetId, TagDefinition tagDefinition, String commitMsg) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			
			if (tagDefinition.getPropertyDefinition(
					PropertyDefinition.SystemPropertyName.catma_markupauthor.name()) == null) {
				PropertyDefinition authorPropertyDefinition = 
						new PropertyDefinition(
							idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()),
							PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
							Collections.singleton(user.getIdentifier()));
				tagDefinition.addSystemPropertyDefinition(authorPropertyDefinition);
			}
			
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

			String projectRevision = 
				gitTagsetHandler.createOrUpdateTagDefinition(
						tagsetId, tagDefinition, commitMsg);

			localGitRepoManager.push(credentialsProvider);

			return projectRevision;
		}
	}
	
	public String removeTagAndAnnotations(
			TagDefinition tagDefinition, Multimap<String, String> annotationIdsByCollectionId) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : annotationIdsByCollectionId.keySet()) {
				removeTagInstances(
					collectionId, annotationIdsByCollectionId.get(collectionId));

				addCollectionToStaged(collectionId);
			}
			
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

			String projectRevision = 
				gitTagsetHandler.removeTagDefinition(tagDefinition);

			localGitRepoManager.push(credentialsProvider);
	
			return projectRevision;
		}
	}
	

	public String removePropertyDefinition(
			PropertyDefinition propertyDefinition, TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition, Set<String> affectedCollectionIds) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : affectedCollectionIds) {
				addCollectionToStaged(collectionId);
			}
			
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

			String projectRevision = 
				gitTagsetHandler.removePropertyDefinition(
						tagsetDefinition, tagDefinition, propertyDefinition);
			
			localGitRepoManager.push(credentialsProvider);

			return projectRevision;
		}
	}

	public String updateTagset(TagsetDefinition tagsetDefinition) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

			String projectRevision = 
				gitTagsetHandler.updateTagsetDefinition(tagsetDefinition);
			
			localGitRepoManager.push(credentialsProvider);

			
			return projectRevision;
		}
	}
	
	public String removeTagset(
			TagsetDefinition tagset, 
			Multimap<String, String> affectedAnnotationIdsByCollectionId) throws Exception {
		
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			for (String collectionId : affectedAnnotationIdsByCollectionId.keySet()) {
				removeTagInstances(
						collectionId, affectedAnnotationIdsByCollectionId.get(collectionId));
				
				addCollectionToStaged(collectionId);
			}

			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());


			String projectRevision = 
					gitTagsetHandler.removeTagsetDefinition(tagset);
			
			localGitRepoManager.push(credentialsProvider);

			return projectRevision;		
		}
	}

	public List<TagsetDefinition> getTagsets() {
		ArrayList<TagsetDefinition> result = new ArrayList<>();
		File tagsetsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				TAGSETS_DIRECTORY_NAME)
			.toFile();
		
		if (!tagsetsDir.exists()) {
			return result;
		}
		
		File[] tagsetDirs = tagsetsDir.listFiles(file -> file.isDirectory());			
		
		GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					this.localGitRepositoryManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

		 for (File tagsetDir : tagsetDirs) {
			 
			 try {
				String tagsetId = tagsetDir.getName();
				TagsetDefinition tagset = gitTagsetHandler.getTagset(tagsetId);						 
				result.add(tagset);
			 }
			 catch (Exception e) {
				logger.log(
					Level.SEVERE,
					String.format(
						"error loading Tagset %1$s for project %2$s",
						tagsetDir,
						projectId), 
					e);
			 }
			 
		 }
		return result;
	}

	
	// AnnotationCollection operations
	public String createAnnotationCollection(String collectionId,
										 String name,
										 String description,
										 String sourceDocumentId,
										 String forkedFromCommitURL
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

			GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
					new GitAnnotationCollectionHandler(
							localGitRepoManager, 
							this.projectPath,
							this.projectId,
							this.remoteGitServerManager.getUsername(),
							this.remoteGitServerManager.getEmail()
			);
			File collectionFolder = Paths.get(
					this.projectPath.getAbsolutePath(),
					ANNOTATION_COLLECTION_DIRECTORY_NAME,
					collectionId
			).toFile();

			// create the AnnotationCollection
			String projectRevisionHash = gitMarkupCollectionHandler.create(
					collectionFolder,
					collectionId,
					name,
					description,
					sourceDocumentId,
					forkedFromCommitURL
			);

			localGitRepoManager.push(credentialsProvider);


			return projectRevisionHash;
		}
	}
	
	public List<AnnotationCollectionReference> getCollectionReferences() {
		ArrayList<AnnotationCollectionReference> collectionReferences = new ArrayList<>();
		
		File collectionsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				ANNOTATION_COLLECTION_DIRECTORY_NAME)
			.toFile();
		
		if (!collectionsDir.exists()) {
			return collectionReferences;
		}
		
		File[] collectionDirs = 
				collectionsDir.listFiles(file -> file.isDirectory());
		
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);

		for (File collectionDir : collectionDirs) {
			String collectionId = collectionDir.getName();
			try {
				collectionReferences.add(
					gitMarkupCollectionHandler.getCollectionReference(collectionId));
			} catch (Exception e) {
				logger.log(
				Level.SEVERE, 
					String.format(
						"error loading Collection reference %1$s for project %2$s",
						collectionDir,
						projectId), 
					e);
				 
			}
		}
		return collectionReferences;
	}

	public List<AnnotationCollection> getCollections(TagLibrary tagLibrary, ProgressListener progressListener) throws IOException {
		
		ArrayList<AnnotationCollection> collections = new ArrayList<>();
		File collectionsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				ANNOTATION_COLLECTION_DIRECTORY_NAME)
			.toFile();
		
		if (!collectionsDir.exists()) {
			return collections;
		}
		
		File[] collectionDirs = 
				collectionsDir.listFiles(file -> file.isDirectory());			

		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);

		for (File collectionDir : collectionDirs) {
			String collectionId = collectionDir.getName();
			try {
				collections.add(
					gitMarkupCollectionHandler.getCollection(
							collectionId, 
							tagLibrary, 
							progressListener));
			} catch (Exception e) {
				logger.log(
				Level.SEVERE, 
					String.format(
						"error loading Collection reference %1$s for project %2$s",
						collectionDir,
						projectId), 
					e);
				 
			}
		}
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(this.projectReference.getNamespace(), this.projectId);
			localRepoManager.addAllAndCommit(
					String.format(
						"Auto committing removal of orphan Annotations "
						+ "and orphan Properties for Project %1$s", this.projectId),
					this.remoteGitServerManager.getUsername(), 
					this.remoteGitServerManager.getEmail(), 
					false);
			localRepoManager.push(credentialsProvider);
		}
		
		return collections;
	}	
	
	private void addCollectionToStaged(String collectionId) throws IOException {
		Path relativePath = Paths.get(ANNOTATION_COLLECTION_DIRECTORY_NAME, collectionId);
		this.localGitRepositoryManager.add(relativePath);
	}
	

	public String addCollectionsToStagedAndCommit(Set<String> collectionIds, String commitMsg, boolean force) throws IOException {
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {

			for (String collectionId : collectionIds) {
				addCollectionToStaged(collectionId);
			}
			
			return localRepoManager.commit(
				commitMsg,
				this.remoteGitServerManager.getUsername(),
				this.remoteGitServerManager.getEmail(), force);
		}	
		
	}
	
	
	public void addOrUpdate(
			String collectionId, Collection<TagReference> tagReferenceList, 
			TagLibrary tagLibrary) throws IOException {
		
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);
			
		JsonLdWebAnnotation annotation = 
				new JsonLdWebAnnotation(
					CATMAPropertyKey.GitLabServerUrl.getValue(), 
					projectId, 
					tagReferenceList,
					tagLibrary);
		gitMarkupCollectionHandler.createTagInstance(collectionId, annotation);
		
	}

	public void removeTagInstances(
		String collectionId, Collection<String> deletedTagInstanceIds) throws IOException {
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);
		gitMarkupCollectionHandler.removeTagInstances(
			collectionId, deletedTagInstanceIds);
	}

	public void removeTagInstance(
		String collectionId, String deletedTagInstanceId) throws IOException {
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);
		gitMarkupCollectionHandler.removeTagInstances(
			collectionId, Collections.singleton(deletedTagInstanceId));
	}

	public String removeCollection(AnnotationCollectionReference userMarkupCollectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
					new GitAnnotationCollectionHandler(
							localGitRepoManager, 
							this.projectPath,
							this.projectId,
							this.remoteGitServerManager.getUsername(),
							this.remoteGitServerManager.getEmail()
			);
				
			String projectRevision = gitMarkupCollectionHandler.removeCollection(
					userMarkupCollectionReference);
			
			localGitRepoManager.push(credentialsProvider);

			return projectRevision;
		}		
	}

	public String updateCollection(AnnotationCollectionReference userMarkupCollectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitAnnotationCollectionHandler collectionHandler = 
					new GitAnnotationCollectionHandler(
							localGitRepoManager, 
							this.projectPath,
							this.projectId,
							this.remoteGitServerManager.getUsername(),
							this.remoteGitServerManager.getEmail()
			);
			String collectionRevision = 
				collectionHandler.updateCollection(userMarkupCollectionReference);

			localGitRepoManager.push(credentialsProvider);

			return collectionRevision;
		}		
	}



	// Document operations

	/**
	 * Creates a new Document within the Project.
	 *
	 * @param documentId the ID of the Document to create.
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original, unmodified document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted, UTF-8 encoded Document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded Document
	 * @param terms the collection of terms extracted from the converted Document
	 * @param tokenizedSourceDocumentFileName name of the file within which the terms/tokens should be stored in serialized form
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object
	 * @return the revisionHash
	 * @throws IOException if an error occurs while creating the Document
	 */
	public String createSourceDocument(
			String documentId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			Map<String, List<TermInfo>> terms, String tokenizedSourceDocumentFileName,
			SourceDocumentInfo sourceDocumentInfo
	) throws IOException {
		
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());


			GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

			File documentFolder = Paths.get(
					this.projectPath.getAbsolutePath(),
					SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
					documentId
			).toFile();
			
			// create the Document within the project
			String revisionHash = gitSourceDocumentHandler.create(
					documentFolder, documentId,
					originalSourceDocumentStream, originalSourceDocumentFileName,
					convertedSourceDocumentStream, convertedSourceDocumentFileName,
					terms, tokenizedSourceDocumentFileName,
					sourceDocumentInfo
			);

			localGitRepoManager.push(credentialsProvider);

			sourceDocumentInfo.getTechInfoSet().setURI(
					Paths.get(
							documentFolder.getAbsolutePath(), 
							convertedSourceDocumentFileName).toUri()
			);


			String name = sourceDocumentInfo.getContentInfoSet().getTitle();
			if ((name == null) || name.isEmpty()) {
				name = "N/A";
			}

			return revisionHash;
		}
	}

	public String updateSourceDocument(SourceDocument sourceDocument) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());	
		
			GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());
			String revisionHash = gitSourceDocumentHandler.update(sourceDocument);
			
			localGitRepoManager.push(credentialsProvider);
			return revisionHash;
		}
	}

	public String getRootRevisionHash() throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(
				projectReference.getNamespace(),
				projectReference.getProjectId());
			return localRepoManager.getRevisionHash();
		}
	}

	public Path getSourceDocumentSubmodulePath(String sourceDocumentId) {
		return Paths.get(
			user.getIdentifier(), 
			projectReference.getNamespace(), 
			projectReference.getProjectId(), 
			SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
			sourceDocumentId);
	}

	public List<SourceDocument> getDocuments() {
		ArrayList<SourceDocument> documents = new ArrayList<>();
		
		File documentsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME)
			.toFile();
		
		if (!documentsDir.exists()) {
			return documents;
		}

		File[] documentDirs = 
				documentsDir.listFiles(file -> file.isDirectory());			

		GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
				this.localGitRepositoryManager, 
				this.projectPath,
				this.remoteGitServerManager.getUsername(),
				this.remoteGitServerManager.getEmail());

		for (File documentDir : documentDirs) {
			String sourceDocumentId = documentDir.getName().toString();
			try {
				documents.add(gitSourceDocumentHandler.open(sourceDocumentId));
			} catch (Exception e) {
				logger.log(
					Level.SEVERE,
					String.format(
						"error loading Document %1$s for Project %2$s",
						documentDir,
						projectId), 
					e);					
			}
		}
		return documents;
	}

	
	public String commitProject(String msg) throws IOException{
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			// open the project root repo
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
			String revisionHash = localGitRepoManager.commit(
					msg, 
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(), 
					false);
			
			localGitRepoManager.push(credentialsProvider);
			
			return revisionHash;
		}
	}

	public Set<Member> getProjectMembers() throws IOException {
		return remoteGitServerManager.getProjectMembers(Objects.requireNonNull(projectId));
	}


	public void removeDocument(SourceDocument sourceDocument) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String documentId = sourceDocument.getUuid();

			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					localGitRepoManager, remoteGitServerManager, credentialsProvider
			);

			MergeResult mergeResult = gitSourceDocumentHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId,
					documentId,
					hasPermission(getRoleForDocument(documentId), RBACPermission.DOCUMENT_WRITE)
			);
			// TODO: handle merge result -> take theirs

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// remove the submodule only!!!
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
					documentId
			).toFile();

			localGitRepoManager.removeSubmodule(
				targetSubmodulePath,
				String.format(
					"Removed Document %1$s with ID %2$s", 
					sourceDocument.toString(), 
					sourceDocument.getUuid()), 
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail());
		}
	}

	public String addCollectionSubmoduleToStagedAndCommit(
			String collectionId, String commitMsg, boolean force) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(ANNOTATION_COLLECTION_DIRECTORY_NAME, collectionId);
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			localGitRepoManager.add(relativePath);
			
			return localGitRepoManager.commit(
					commitMsg, 
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					force);			
		}	
	}
	
	public String addTagsetSubmoduleToStagedAndCommit(String tagsetId, String commitMsg) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(TAGSETS_DIRECTORY_NAME, tagsetId);
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			localGitRepoManager.add(relativePath);
			
			return localGitRepoManager.commit(
					commitMsg, 
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					false);
		}		
	}


	public void ensureDevBranches() throws Exception {
		logger.info(String.format("Ensuring dev branches for project %1$s", projectId));

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// tagsets
			GitTagsetHandler gitTagsetHandler =	new GitTagsetHandler(
					localGitRepoManager,
					this.remoteGitServerManager,
					this.credentialsProvider
			);

			for (TagsetDefinition tagset : getTagsets()) {
				logger.info(
						String.format(
								"Checking out dev branch for tagset \"%1$s\" with ID %2$s",
								tagset.getName(),
								tagset.getUuid()
						)
				);
				gitTagsetHandler.checkout(
						projectId, tagset.getUuid(), ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true
				);
			}

			// collections
			GitAnnotationCollectionHandler collectionHandler = new GitAnnotationCollectionHandler(
					localGitRepoManager,
					this.remoteGitServerManager,
					this.credentialsProvider
			);

			for (AnnotationCollectionReference collectionReference : getCollectionReferences()) {
				logger.info(
						String.format(
								"Checking out dev branch for collection \"%1$s\" with ID %2$s",
								collectionReference.getName(),
								collectionReference.getId()
						)
				);
				collectionHandler.checkout(
					projectId, collectionReference.getId(),	ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true
				);
			}

			// documents
			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					localGitRepoManager,
					remoteGitServerManager,
					credentialsProvider
			);

			for (SourceDocument sourceDocument : getDocuments()) {
				logger.info(
						String.format(
								"Checking out dev branch for document \"%s\" with ID %s",
								sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getTitle(),
								sourceDocument.getUuid()
						)
				);
				gitSourceDocumentHandler.checkout(
						projectId, sourceDocument.getUuid(), ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true
				);
			}
		}
	}

	public boolean hasUncommittedChanges() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitAnnotationCollectionHandler collectionHandler = 
					new GitAnnotationCollectionHandler(
							localGitRepoManager, 
							this.remoteGitServerManager,
							this.credentialsProvider);
			//TODO: better get collectionRefs from graph
			for (AnnotationCollectionReference collectionRef : getCollectionReferences()) {
				
				if (collectionHandler.hasUncommittedChanges(projectId, collectionRef.getId())) {
					return true;
				}
			}
			
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			return localGitRepoManager.hasUncommitedChangesWithSubmodules(
					getReadableSubmodules(localGitRepoManager));
		}
	}

	public List<CommitInfo> getUnsynchronizedChanges() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			return localGitRepoManager.getUnsynchronizedChanges();
		}		
	}

	public void synchronizeTagsetWithRemote(String tagsetId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			@SuppressWarnings("unused")
			MergeResult mergeResult = gitTagsetHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, tagsetId, hasPermission(
								getRoleForTagset(tagsetId), RBACPermission.TAGSET_WRITE));
			// mergeResult is handled after all resources have been synchronized
		}
	}

	public Status getStatus() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			return localGitRepoManager.getStatus();
		}		
	}
	
	public Status getStatus(AnnotationCollectionReference collectionReference) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitAnnotationCollectionHandler collectionHandler = 
					new GitAnnotationCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			return collectionHandler.getStatus(projectId, collectionReference.getId());
		}
	}
	
	public Status getStatus(TagsetDefinition tagset) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler tagsetHandler = 
					new GitTagsetHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			return tagsetHandler.getStatus(projectId, tagset.getUuid());
		}
	}

	public boolean hasConflicts() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			Path tagsetsDirPath = Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(TAGSETS_DIRECTORY_NAME);
			Path collectionDirPath = Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(ANNOTATION_COLLECTION_DIRECTORY_NAME);
			Path sourceDocumentsDirPath = Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME);

			Status topLevelStatus = localGitRepoManager.getStatus(true);
			boolean pushTopLevelConflictResolution = false;
			if (!topLevelStatus.getConflicting().isEmpty()) {
				if (topLevelStatus.getConflicting().contains(Constants.DOT_GIT_MODULES)) {
					logger.info(
							String.format(
									"Found conflicts in %1$s of project %2$s, trying auto resolution",
									Constants.DOT_GIT_MODULES,
									projectId
							)
					);

					localGitRepoManager.resolveGitSubmoduleFileConflicts();
					pushTopLevelConflictResolution = true;
				}
			}

			Status projectStatus = localGitRepoManager.getStatus();
			localGitRepoManager.detach();

			if (tagsetsDirPath.toFile().exists()) {
				GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
						localGitRepoManager,
						this.remoteGitServerManager,
						this.credentialsProvider
				);

				List<Path> paths = Files
						.walk(tagsetsDirPath, 1)
						.filter(tagsetPath -> !tagsetsDirPath.equals(tagsetPath))
						.collect(Collectors.toList());

				for (Path tagsetPath : paths) {
					// empty directories are submodules not yet initialized or deleted
					if (tagsetPath.toFile().list() != null && tagsetPath.toFile().list().length > 0) {
						String tagsetId = tagsetPath.getFileName().toString();
						Status status = gitTagsetHandler.getStatus(projectId, tagsetId);
						if (!status.getConflicting().isEmpty()) {
							StatusPrinter.print("Tagset #" + tagsetId, status, System.out);
							return true;
						}
					}
				}
			}

			if (collectionDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());

				GitAnnotationCollectionHandler gitCollectionHandler = new GitAnnotationCollectionHandler(
						localGitRepoManager,
						this.remoteGitServerManager,
						this.credentialsProvider
				);

				for (Path collectionPath : paths) {
					// empty directories are submodules not yet initialized or deleted
					if (collectionPath.toFile().list() != null && collectionPath.toFile().list().length > 0) {
						String collectionId = collectionPath.getFileName().toString();
						Status status = gitCollectionHandler.getStatus(projectId, collectionId);
						if (!status.getConflicting().isEmpty()) {
							StatusPrinter.print("Collection #" + collectionId, status, System.out);
							return true;
						}
					}
				}
			}

			if (sourceDocumentsDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(sourceDocumentsDirPath, 1)
						.filter(sourceDocumentPath -> !sourceDocumentsDirPath.equals(sourceDocumentPath))
						.collect(Collectors.toList());

				GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
						localGitRepoManager,
						remoteGitServerManager,
						credentialsProvider
				);

				for (Path sourceDocumentPath : paths) {
					// empty directories are submodules not yet initialized or deleted
					if (sourceDocumentPath.toFile().list() != null && sourceDocumentPath.toFile().list().length > 0) {
						String sourceDocumentId = sourceDocumentPath.getFileName().toString();
						Status status = gitSourceDocumentHandler.getStatus(projectId, sourceDocumentId);
						if (!status.getConflicting().isEmpty()) {
							StatusPrinter.print("Document #" + sourceDocumentId, status, System.out);
							return true;
						}
					}
				}
			}

			if (!projectStatus.getConflicting().isEmpty()) {
				StatusPrinter.print("Project #" + projectId, projectStatus, System.out);
				return true;
			}

			if (pushTopLevelConflictResolution) {
				if (projectStatus.hasUncommittedChanges()) {
					localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
					localGitRepoManager.push(credentialsProvider);
				}
			}
		} 

		return false;
	}

	public List<CollectionConflict> getCollectionConflicts() throws Exception {
		
		ArrayList<CollectionConflict> collectionConflicts = new ArrayList<>();
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
				 projectId,
				 GitProjectManager.getProjectRootRepositoryName(projectId));
			 
			Path collectionDirPath = 
					Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(ANNOTATION_COLLECTION_DIRECTORY_NAME);

			localGitRepoManager.detach();

			if (collectionDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());
				
				GitAnnotationCollectionHandler gitCollectionHandler = 
						new GitAnnotationCollectionHandler(
								localGitRepoManager, 
								this.remoteGitServerManager,
								this.credentialsProvider);

				for (Path collectionPath : paths) {
					if (collectionDirPath.resolve(collectionPath).resolve(Constants.DOT_GIT).toFile().exists()) {
						String collectionId = collectionPath.getFileName().toString();
						Status status = gitCollectionHandler.getStatus(projectId, collectionId);
						if (!status.getConflicting().isEmpty()) {
							collectionConflicts.add(
									gitCollectionHandler.getCollectionConflict(
											projectId, collectionId));
						}
					}
				}
				
			}
		}
		
		return collectionConflicts;
	}

	public void resolveAnnotationConflict(
			String collectionId, 
			AnnotationConflict annotationConflict,
			TagLibrary tagLibrary) throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitAnnotationCollectionHandler gitMarkupCollectionHandler = new GitAnnotationCollectionHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			if (annotationConflict.getResolvedTagReferences().isEmpty()) {
				TagInstance tagInstance = 
					annotationConflict.getDismissedTagInstance();
				gitMarkupCollectionHandler.removeTagInstances(
					projectId, collectionId, Collections.singleton(tagInstance.getUuid()));
			}
			else {
				JsonLdWebAnnotation annotation = 
						new JsonLdWebAnnotation(
							CATMAPropertyKey.GitLabServerUrl.getValue(), 
							projectId, 
							annotationConflict.getResolvedTagReferences(),
							tagLibrary);
				gitMarkupCollectionHandler.createTagInstance(projectId, collectionId, annotation);
			}
		}		
	}

	public void synchronizeCollectionWithRemote(String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitAnnotationCollectionHandler collectionHandler = 
					new GitAnnotationCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			@SuppressWarnings("unused")
			MergeResult mergeResult = collectionHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, collectionId,
					hasPermission(getRoleForCollection(collectionId), RBACPermission.COLLECTION_WRITE));
			// mergeResult is handled after all resources have been synchronized
		}
	}

	public Collection<DeletedResourceConflict> resolveRootConflicts() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectId, GitProjectManager.getProjectRootRepositoryName(projectId)
			);

			Status status = localGitRepoManager.getStatus();

			StatusPrinter.print("Project status", status, System.out);

			Collection<DeletedResourceConflict> deletedResourceConflicts = localGitRepoManager.resolveRootConflicts(
					projectId, this.credentialsProvider
			);

			if (deletedResourceConflicts.isEmpty()) {
				localGitRepoManager.addAllAndCommit(
						"Auto-committing merged changes (resolveRootConflicts)",
						remoteGitServerManager.getUsername(),
						remoteGitServerManager.getEmail(),
						true
				);

				localGitRepoManager.push(credentialsProvider);
			}
			else {
				localGitRepoManager.detach();

				for (DeletedResourceConflict deletedResourceConflict : deletedResourceConflicts) {
					if (deletedResourceConflict.getRelativeModulePath().startsWith(ANNOTATION_COLLECTION_DIRECTORY_NAME)) {
						try {
							GitAnnotationCollectionHandler collectionHandler = new GitAnnotationCollectionHandler(
									localGitRepoManager, this.remoteGitServerManager, this.credentialsProvider
							);
							String collectionId = deletedResourceConflict.getRelativeModulePath().substring(
									ANNOTATION_COLLECTION_DIRECTORY_NAME.length() + 1
							);

							deletedResourceConflict.setResourceId(collectionId);

							if (deletedResourceConflict.isDeletedByThem()) {
								ContentInfoSet contentInfoSet = collectionHandler.getContentInfoSet(
										projectId, collectionId
								);
								deletedResourceConflict.setContentInfoSet(contentInfoSet);
							}
							else {
								deletedResourceConflict.setContentInfoSet(new ContentInfoSet("N/A"));
							}

							deletedResourceConflict.setResourceType(DeletedResourceConflict.ResourceType.ANNOTATION_COLLECTION);
						}
						finally {
							localGitRepoManager.detach();
						}
					}
					else if (deletedResourceConflict.getRelativeModulePath().startsWith(TAGSETS_DIRECTORY_NAME)) {
						try {
							GitTagsetHandler tagsetHandler = new GitTagsetHandler(
									localGitRepoManager, this.remoteGitServerManager, this.credentialsProvider
							);
							String tagsetId = deletedResourceConflict.getRelativeModulePath().substring(
									TAGSETS_DIRECTORY_NAME.length() + 1
							);

							deletedResourceConflict.setResourceId(tagsetId);

							if (deletedResourceConflict.isDeletedByThem()) {
								ContentInfoSet contentInfoSet = tagsetHandler.getContentInfoSet(
										projectId, tagsetId
								);
								deletedResourceConflict.setContentInfoSet(contentInfoSet);
							}
							else {
								deletedResourceConflict.setContentInfoSet(new ContentInfoSet("N/A"));
							}

							deletedResourceConflict.setResourceType(DeletedResourceConflict.ResourceType.TAGSET);
						}
						finally {
							localGitRepoManager.detach();
						}
					}
					else if (deletedResourceConflict.getRelativeModulePath().startsWith(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME)) {
						try {
							GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
									localGitRepoManager, remoteGitServerManager, credentialsProvider
							);
							String sourceDocumentId = deletedResourceConflict.getRelativeModulePath().substring(
									SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME.length() + 1
							);

							deletedResourceConflict.setResourceId(sourceDocumentId);

							if (deletedResourceConflict.isDeletedByThem()) {
								ContentInfoSet contentInfoSet = gitSourceDocumentHandler.getContentInfoSet(
										projectId, sourceDocumentId
								);
								deletedResourceConflict.setContentInfoSet(contentInfoSet);
							}
							else {
								deletedResourceConflict.setContentInfoSet(new ContentInfoSet("N/A"));
							}

							deletedResourceConflict.setResourceType(DeletedResourceConflict.ResourceType.SOURCE_DOCUMENT);
						}
						finally {
							localGitRepoManager.detach();
						}
					}
				}
			}

			return deletedResourceConflicts;
		}
	}


	// TODO: refactoring - see hasConflicts, almost identical code (same for tagsets and collections)
	//       also: are we unnecessarily opening the root repo here?
	public List<SourceDocumentConflict> getSourceDocumentConflicts() throws Exception {
		ArrayList<SourceDocumentConflict> sourceDocumentConflicts = new ArrayList<>();

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			Path sourceDocumentsDirPath = Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME);
			localGitRepoManager.detach();

			if (sourceDocumentsDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(sourceDocumentsDirPath, 1)
						.filter(sourceDocumentPath -> !sourceDocumentsDirPath.equals(sourceDocumentPath))
						.collect(Collectors.toList());

				GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
						localGitRepoManager, remoteGitServerManager, credentialsProvider
				);

				for (Path sourceDocumentPath : paths) {
					if (sourceDocumentsDirPath.resolve(sourceDocumentPath).resolve(Constants.DOT_GIT).toFile().exists()) {
						String sourceDocumentId = sourceDocumentPath.getFileName().toString();
						Status status = gitSourceDocumentHandler.getStatus(projectId, sourceDocumentId);
						if (!status.getConflicting().isEmpty()) {
							sourceDocumentConflicts.add(
									gitSourceDocumentHandler.getSourceDocumentConflict(projectId, sourceDocumentId)
							);
						}
					}
				}
			}
		}

		return sourceDocumentConflicts;
	}

	public void synchronizeSourceDocumentWithRemote(String sourceDocumentId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(localGitRepoManager, remoteGitServerManager, credentialsProvider);

			@SuppressWarnings("unused")
			MergeResult mergeResult = gitSourceDocumentHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId,
					sourceDocumentId,
					hasPermission(getRoleForDocument(sourceDocumentId), RBACPermission.DOCUMENT_WRITE)
			);
			// mergeResult is handled after all resources have been synchronized
		}
	}

	public void synchronizeWithRemote() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
				 projectId,
				 GitProjectManager.getProjectRootRepositoryName(projectId));
			
			localGitRepoManager.fetch(credentialsProvider);
			
			if (localGitRepoManager.hasRef(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)) {
				MergeResult mergeWithOriginMasterResult = 
					localGitRepoManager.mergeWithDeletedByThemWorkaroundStrategyRecursive(
							Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
				
				if (mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
					localGitRepoManager.push(credentialsProvider);
				}
			}
			else if (!localGitRepoManager.getRevisionHash().equals(ILocalGitRepositoryManager.NO_COMMITS_YET)) {
				localGitRepoManager.push(credentialsProvider);
			}
		}	
	}

	public void checkoutCollectionDevBranchAndRebase(String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitAnnotationCollectionHandler collectionHandler = 
					new GitAnnotationCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			collectionHandler.checkout(projectId, collectionId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, false);
			collectionHandler.rebaseToMaster(projectId, collectionId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH);
		}
	}

	public void checkoutTagsetDevBranchAndRebase(String tagsetId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			gitTagsetHandler.checkout(projectId, tagsetId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, false);
			gitTagsetHandler.rebaseToMaster(projectId, tagsetId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH);
		}		
	}

	public void initAndUpdateSubmodules() throws Exception {
		logger.info(String.format("Init and update submodules for Project %1$s", projectId));
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			Status projectStatus = localGitRepoManager.getStatus(true);
			Set<String> conflicting = projectStatus.getConflicting();
			Set<String> readableSubmodules = getReadableSubmodules(localGitRepoManager);
			Set<String> validSubmodules = new HashSet<>(readableSubmodules);
			validSubmodules.removeAll(conflicting);
			localGitRepoManager.initAndUpdateSubmodules(credentialsProvider, validSubmodules);
		}		
		
	}
	
	private Set<String> getReadableSubmodules(ILocalGitRepositoryManager localRepoManager) throws IOException {
		Set<String> readableSubmodules = new HashSet<>();
		
			
		List<String> relativeSubmodulePaths = localRepoManager.getSubmodulePaths();
		
		for (String relativeSubmodulePath : relativeSubmodulePaths) {
			if (relativeSubmodulePath != null) {
				if (relativeSubmodulePath.startsWith(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME)) {
					String documentId = Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
						 .resolve(relativeSubmodulePath)
						 .getFileName()
						 .toString();
					RBACRole resourceRole = rolesPerResource.get(documentId);
					if ((resourceRole != null) && hasPermission(resourceRole, RBACPermission.DOCUMENT_READ)) {
						readableSubmodules.add(relativeSubmodulePath);
					}
				}
				else if (relativeSubmodulePath.startsWith(ANNOTATION_COLLECTION_DIRECTORY_NAME)) {
					String collectionId = Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
						 .resolve(relativeSubmodulePath)
						 .getFileName()
						 .toString();
					RBACRole resourceRole = rolesPerResource.get(collectionId);
					if ((resourceRole != null) && hasPermission(resourceRole, RBACPermission.COLLECTION_READ)) {
						readableSubmodules.add(relativeSubmodulePath);
					}
				}
				else if (relativeSubmodulePath.startsWith(TAGSETS_DIRECTORY_NAME)) {
					String tagsetId = Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
						 .resolve(relativeSubmodulePath)
						 .getFileName()
						 .toString();
					RBACRole resourceRole = rolesPerResource.get(tagsetId);
					if ((resourceRole != null) && hasPermission(resourceRole, RBACPermission.TAGSET_READ)) {
						readableSubmodules.add(relativeSubmodulePath);
					}
				}
			}
		}

		return readableSubmodules;
	}

	/**
	 * Returns a mapping of submodule directories that exist on disk. Note that these are not necessarily valid submodules, as opposed to what
	 * {@link de.catma.repository.git.managers.JGitRepoManager#getSubmodulePaths()} returns.
	 *
	 * @param localRepoManager an {@link ILocalGitRepositoryManager} instance
	 * @param containerDirectory should be one of <code>SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME</code>,
	 *                           <code>ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME</code>, or <code>TAGSET_SUBMODULES_DIRECTORY_NAME</code>
	 * @return a mapping of relative paths to {@link Path} objects
	 */
	private Map<String, Path> getSubmodulePaths(ILocalGitRepositoryManager localRepoManager, String containerDirectory) throws IOException {
		Path submoduleDirPath = Paths.get(localRepoManager.getRepositoryWorkTree().toURI()).resolve(containerDirectory);
		Map<String, Path> relativePathToPathMap = new HashMap<>();

		if (!submoduleDirPath.toFile().exists()) {
			return relativePathToPathMap;
		}

		List<Path> paths = Files.walk(submoduleDirPath, 1).filter(submodulePath -> !submoduleDirPath.equals(submodulePath))
				.collect(Collectors.toList());

		for (Path submodulePath : paths) {
			Path relativeSubmodulePath = Paths.get(containerDirectory, submodulePath.getFileName().toString());
			String relativeSubmodulePathUnix = FilenameUtils.separatorsToUnix(relativeSubmodulePath.toString());
			relativePathToPathMap.put(relativeSubmodulePathUnix, submodulePath);
		}

		return relativePathToPathMap;
	}

	/**
	 * Returns a list of paths of stale submodules, i.e. submodules that exist on disk but are no longer registered as submodules.
	 */
	private List<Path> getStaleSubmodulePaths() throws IOException {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// note that this collection contains null entries for invalid submodules
			List<String> validSubmodulePaths = localRepoManager.getSubmodulePaths();

			ArrayList<Path> stalePaths = new ArrayList<>();

			Map<String, Path> submodulePathMapping = getSubmodulePaths(localRepoManager, ANNOTATION_COLLECTION_DIRECTORY_NAME);
			submodulePathMapping.putAll(getSubmodulePaths(localRepoManager, SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME));
			submodulePathMapping.putAll(getSubmodulePaths(localRepoManager, TAGSETS_DIRECTORY_NAME));

			for (Map.Entry<String, Path> entry : submodulePathMapping.entrySet()) {
				if (!validSubmodulePaths.contains(entry.getKey())) {
					stalePaths.add(entry.getValue());
				}
			}

			return stalePaths;
		}
	}

	public void removeStaleSubmoduleDirectories() throws Exception {
		logger.info(String.format("Removing stale submodule directories for Project %1$s", projectId));

		try {
			List<Path> staleSubmodulePaths = getStaleSubmodulePaths();
			for (Path path : staleSubmodulePaths) {
				FileUtils.deleteDirectory(path.toFile());
			}
		}
		catch (IOException e) {
			throw new Exception("Failed to remove stale submodule directories", e);
		}
	}

	public RBACRole getRoleForDocument(String documentId) {
		return rolesPerResource.get(documentId);
	}

	public RBACRole getRoleForCollection(String collectionId) {
		return rolesPerResource.get(collectionId);
	}
	
	public RBACRole getRoleForTagset(String tagsetId) {
		return rolesPerResource.get(tagsetId);
	}

	public boolean hasPermission(RBACRole role, RBACPermission permission) {
		return remoteGitServerManager.hasPermission(role, permission);
	}
	
	public boolean isAuthorizedOnProject(RBACPermission permission) {
		return remoteGitServerManager.isAuthorizedOnProject(remoteGitServerManager.getUser(), permission, this.projectReference);
	}
	
	public RBACSubject assignOnProject(RBACSubject subject, RBACRole role) throws IOException {
		return remoteGitServerManager.assignOnProject(subject, role, projectId);
	}
	
	public void unassignFromProject(RBACSubject subject) throws IOException {
		remoteGitServerManager.unassignFromProject(subject, projectId);
	}
	
	public RBACSubject assignOnResource(RBACSubject subject, RBACRole role, String resourceId) throws IOException {
		return remoteGitServerManager.assignOnResource(subject, role, projectId, resourceId);
	}
	
	public void unassignFromResource(RBACSubject subject, String resourceId) throws IOException {
		remoteGitServerManager.unassignFromResource(subject, projectId, resourceId);
	}

	public List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException {
		return remoteGitServerManager.findUser(usernameOrEmail, offset, limit);
	}

	public Set<Member> getResourceMembers(String resourceId) throws IOException {
		return remoteGitServerManager.getResourceMembers(projectId, resourceId);
	}
	
	public RBACRole getRoleOnProject(RBACSubject subject) throws IOException {
		return remoteGitServerManager.getRoleOnProject(subject, projectId);
	}

	public void resolveDeletedResourceConflicts(Collection<DeletedResourceConflict> deletedResourceConflicts) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					 projectId,
					 GitProjectManager.getProjectRootRepositoryName(projectId));
	
			 for (DeletedResourceConflict deletedResourceConflict : deletedResourceConflicts) {
				 if (deletedResourceConflict.isDeletedByThem()) {
					 if (deletedResourceConflict.getResolution().equals(Resolution.MINE)) {
						 // keep mine
						String relativeModulePath = deletedResourceConflict.getRelativeModulePath();
						String submoduleId = relativeModulePath.substring(relativeModulePath.lastIndexOf('/')+1);
						String submoduleUri = 
							CATMAPropertyKey.GitLabServerUrl.getValue() + "/" + projectId + "/" + submoduleId + ".git";
						
						localGitRepoManager.keepSubmodule(relativeModulePath, submoduleUri);
					 }
					 else {
						 // delete mine
						String relativeModulePath = deletedResourceConflict.getRelativeModulePath();
						Path collectionDirPath = 
								 Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
								 .resolve(relativeModulePath);
						localGitRepoManager.remove(collectionDirPath.toFile());
					 }
					 
				 }
				 else { // deleted by us
					if (deletedResourceConflict.getResolution().equals(Resolution.MINE)) {
						 // delete theirs
						String relativeModulePath = deletedResourceConflict.getRelativeModulePath();
						Path collectionDirPath = 
								 Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
								 .resolve(relativeModulePath);
						localGitRepoManager.remove(collectionDirPath.toFile());						 
					}
					else {
						// keep theirs
						String relativeModulePath = deletedResourceConflict.getRelativeModulePath();
						String submoduleId = relativeModulePath.substring(relativeModulePath.lastIndexOf('/')+1);
						String submoduleUri = 
							CATMAPropertyKey.GitLabServerUrl.getValue() + "/" + projectId + "/" + submoduleId + ".git";
						
						Path collectionDirPath = 
								 Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
								 .resolve(relativeModulePath);
						localGitRepoManager.remove(collectionDirPath.toFile());
						
						File targetSubmodulePath = Paths.get(
								localGitRepoManager.getRepositoryWorkTree().getAbsolutePath(),
								relativeModulePath
						).toFile();
						
						localGitRepoManager.addSubmodule(targetSubmodulePath, submoduleUri, credentialsProvider);
					}				
				 }
			 }
			 
			 localGitRepoManager.commit(
					 "Auto-committing merged changes (resolveDeletedResourceConflicts)",
					 remoteGitServerManager.getUsername(),
					 remoteGitServerManager.getEmail(), true);

			 localGitRepoManager.push(credentialsProvider); //push may fail, we'll check that later up the chain
		}
	}

	List<String> getResourceIds() throws IOException {
		return remoteGitServerManager.getGroupRepositoryNames(projectId);
	}

	public void verifyCollections() throws Exception {
		List<AnnotationCollectionReference> collectionRefs = getCollectionReferences();
		Set<String> documentIds = getDocuments().stream().map(SourceDocument::getUuid).collect(Collectors.toSet());
		Set<AnnotationCollectionReference> staleCollectionCandidates = new HashSet<>();
		for (AnnotationCollectionReference collectionRef : collectionRefs) {
			String documentId = collectionRef.getSourceDocumentId();
			
			if (!documentIds.contains(documentId)) {
				staleCollectionCandidates.add(collectionRef);
			}
		}
		if (!staleCollectionCandidates.isEmpty()) {
			try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
				localGitRepoManager.open(
						 projectId,
						 GitProjectManager.getProjectRootRepositoryName(projectId));
				
				Set<String> verifiedDeleted = 
					localGitRepoManager.verifyDeletedResources(
						staleCollectionCandidates.stream().map(AnnotationCollectionReference::getSourceDocumentId).collect(Collectors.toSet()));
				
				localGitRepoManager.detach();

				// TODO: unnecessary to loop over all collections when only the stale candidates can end up in verifiedDeleted
				for (AnnotationCollectionReference collectionRef : collectionRefs) {
					String collectionId = collectionRef.getId();
					RBACRole collectionRole = rolesPerResource.get(collectionId);
					if (collectionRole != null && hasPermission(collectionRole, RBACPermission.COLLECTION_DELETE_OR_EDIT)) {
						if (verifiedDeleted.contains(collectionRef.getSourceDocumentId())) {
							logger.info(String.format(
								"Removing stale Collection %1$s with ID %2$s due to removal of corresp. Document with ID %3$s",
								collectionRef.getName(), 
								collectionRef.getId(),
								collectionRef.getSourceDocumentId()));
							removeCollection(collectionRef);
						}
					}
				}
				
			}
		}
	}
	
	// Comment operations  
	
	public List<Comment> getComments(String documentId) throws IOException {
		return remoteGitServerManager.getComments(projectId, documentId);
	}

	public void addComment(Comment comment) throws IOException {
		remoteGitServerManager.addComment(projectId, comment);
	}

	public void removeComment(Comment comment) throws IOException {
		remoteGitServerManager.removeComment(projectId, comment);
	}

	public void updateComment(Comment comment) throws IOException {
		remoteGitServerManager.updateComment(projectId, comment);
	}

	public void addReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.addReply(projectId, comment, reply);
	}

	public List<Reply> getCommentReplies(Comment comment) throws IOException {
		return remoteGitServerManager.getCommentReplies(projectId, comment);
	}
	
	public void updateReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.updateReply(projectId, comment, reply);
	}
	
	public void removeReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.removeReply(projectId, comment, reply);
	}

	public List<Comment> getCommentsWithReplies(List<String> documentIdList) throws IOException {
		List<Comment> comments = new ArrayList<Comment>();
		if (documentIdList.isEmpty()) {
			return comments;
		}
		
		if (documentIdList.size() > 10) {
			comments.addAll(remoteGitServerManager.getComments(this.projectId)
					.stream()
					.filter(comment -> documentIdList.contains(comment.getDocumentId()))
					.collect(Collectors.toList()));
		}
		else {
			for (String documentId : documentIdList) {
				comments.addAll(
					remoteGitServerManager.getComments(projectId, documentId));
			}
		}
		
		for (Comment comment : comments) {
			if (comment.getReplyCount() > 0) {
				getCommentReplies(comment);
			}
		}
		
		return comments;
	}
	
}