package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

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
import de.catma.indexer.TermInfo;
import de.catma.project.CommitInfo;
import de.catma.project.ProjectReference;
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
	public static final String ANNOTATION_COLLECTIONS_DIRECTORY_NAME = "collections";
	public static final String DOCUMENTS_DIRECTORY_NAME = "documents";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final User user;
	private final String projectId;

	private final IDGenerator idGenerator = new IDGenerator();
	private final CredentialsProvider credentialsProvider;

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
			//localGitRepoManager.open(projectId, GitProjectsManager.getProjectRootRepositoryName(projectId));

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
					ANNOTATION_COLLECTIONS_DIRECTORY_NAME,
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
				ANNOTATION_COLLECTIONS_DIRECTORY_NAME)
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
				ANNOTATION_COLLECTIONS_DIRECTORY_NAME)
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
		Path relativePath = Paths.get(ANNOTATION_COLLECTIONS_DIRECTORY_NAME, collectionId);
		this.localGitRepositoryManager.add(relativePath);
	}
	

	public String addCollectionsToStagedAndCommit(
			Set<String> collectionIds, 
			String commitMsg, 
			boolean force, boolean withPush) throws IOException {
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
			for (String collectionId : collectionIds) {
				addCollectionToStaged(collectionId);
			}
			
			String projectRevision = localRepoManager.commit(
				commitMsg,
				this.remoteGitServerManager.getUsername(),
				this.remoteGitServerManager.getEmail(), force);
			
			if (withPush) {
				localRepoManager.push(credentialsProvider);
			}
			
			return projectRevision;
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
		
		
		tagReferenceList.stream().collect(
				Collectors.toMap(
						TagReference::getTagInstance, 
						Function.identity()));
		
		Multimap<TagInstance, TagReference> tagInstances = 
				Multimaps.index(tagReferenceList, TagReference::getTagInstance);
		
		List<Pair<JsonLdWebAnnotation, TagInstance>> annotationToTagInstanceMapping =
				Lists.newArrayList();
		
		for (TagInstance tagInstance : tagInstances.keys()) {
			
			 Collection<TagReference> references = tagInstances.get(tagInstance);
			 
			JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
					references,
					tagLibrary,
					tagInstance.getPageFilename());
			annotationToTagInstanceMapping.add(new Pair<>(annotation, tagInstance));
		}
		
		
		gitMarkupCollectionHandler.createTagInstances(collectionId, annotationToTagInstanceMapping);
		
	}
	
	public void addOrUpdate(
			String collectionId, TagInstance tagInstance, Collection<TagReference> tagReferenceList, 
			TagLibrary tagLibrary) throws IOException {
		
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);
			
		JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
			tagReferenceList,
			tagLibrary,
			tagInstance.getPageFilename());
		
		if (tagInstance.getPageFilename() == null) {			
			gitMarkupCollectionHandler.createTagInstances(
				collectionId, Collections.singletonList(new Pair<>(annotation, tagInstance)));
		}
		else {
			gitMarkupCollectionHandler.updateTagInstance(collectionId, annotation);
			
		}
		
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

	public String removeCollection(AnnotationCollectionReference userMarkupCollectionReference) throws IOException {
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
				
			String projectRevision = gitMarkupCollectionHandler.removeCollection(
					userMarkupCollectionReference);
			
			localGitRepoManager.push(credentialsProvider);

			return projectRevision;
		}		
	}

	public String updateCollection(AnnotationCollectionReference userMarkupCollectionReference) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
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
					DOCUMENTS_DIRECTORY_NAME,
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

	public Path getSourceDocumentPath(String sourceDocumentId) {
		return Paths.get(
			user.getIdentifier(), 
			projectReference.getNamespace(), 
			projectReference.getProjectId(), 
			DOCUMENTS_DIRECTORY_NAME,
			sourceDocumentId);
	}

	public List<SourceDocument> getDocuments() {
		ArrayList<SourceDocument> documents = new ArrayList<>();
		
		File documentsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				DOCUMENTS_DIRECTORY_NAME)
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
			
			String revisionHash = localGitRepoManager.addAllAndCommit(
					msg, 
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(), 
					false);
			
			localGitRepoManager.push(credentialsProvider);
			
			return revisionHash;
		}
	}
	


	public String removeDocument(SourceDocument sourceDocument) throws Exception {
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
				
			for (AnnotationCollectionReference collectionRef : new HashSet<>(sourceDocument.getUserMarkupCollectionRefs())) {
				gitMarkupCollectionHandler.removeCollectionWithoutCommit(collectionRef);
			}

			GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
					localGitRepoManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());

			String revisionHash = 
					gitSourceDocumentHandler.removeDocument(sourceDocument);
			
			localGitRepoManager.push(credentialsProvider);
			
			return revisionHash;
		}
	}

	public Set<Member> getProjectMembers() throws IOException {
		return remoteGitServerManager.getProjectMembers(Objects.requireNonNull(projectReference));
	}


	public boolean hasUncommittedChanges() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project repo
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
			return localGitRepoManager.hasUncommitedChanges();
		}
	}

	public List<CommitInfo> getUnsynchronizedChanges() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project repo
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			return localGitRepoManager.getUnsynchronizedChanges();
		}		
	}

	public Status getStatus() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project repo
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			return localGitRepoManager.getStatus();
		}		
	}
	
	public boolean hasConflicts() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());

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


	public void synchronizeWithRemote() throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
			localGitRepoManager.fetch(credentialsProvider);
			
			if (localGitRepoManager.hasRef(
					Constants.DEFAULT_REMOTE_NAME + "/" + remoteGitServerManager.getUsername())) {
				
				if (localGitRepoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)) {					
					MergeResult mergeWithOriginMasterResult = 
						localGitRepoManager.merge(
								Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
					
					if (mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
						localGitRepoManager.push(credentialsProvider);
					}
					else {
						localGitRepoManager.revert(mergeWithOriginMasterResult);
					}
				}
				
			}
			else if (!localGitRepoManager.getRevisionHash().equals(ILocalGitRepositoryManager.NO_COMMITS_YET)) {
				localGitRepoManager.push(credentialsProvider);
			}
		}	
	}

	public void ensureUserBranch() throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), true);
		}
	}
	
	public boolean hasPermission(RBACRole role, RBACPermission permission) {
		return remoteGitServerManager.hasPermission(role, permission);
	}
	
	public boolean isAuthorizedOnProject(RBACPermission permission) {
		return remoteGitServerManager.isAuthorizedOnProject(
				remoteGitServerManager.getUser(), permission, this.projectReference);
	}
	
	public RBACSubject assignOnProject(RBACSubject subject, RBACRole role) throws IOException {
		return remoteGitServerManager.assignOnProject(subject, role, projectReference);
	}
	
	public void unassignFromProject(RBACSubject subject) throws IOException {
		remoteGitServerManager.unassignFromProject(subject, projectReference);
	}
		
	public List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException {
		return remoteGitServerManager.findUser(usernameOrEmail, offset, limit);
	}

	public RBACRole getRoleOnProject(RBACSubject subject) throws IOException {
		return remoteGitServerManager.getRoleOnProject(subject, projectReference);
	}

	/**
	 * Checks whether a Document of an AnnotationCollection got deleted by 
	 * another user. The orphan Collection gets deleted as well then.
	 * 
	 * @throws Exception
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
		if (!staleCollectionCandidates.isEmpty()) {
			try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
				localGitRepoManager.open(
						 projectReference.getNamespace(),
						 projectReference.getProjectId());
				
				Set<String> verifiedDeleted = 
					localGitRepoManager.getDeletedResourcesFromLog(
						staleCollectionCandidates.stream()
						.map(AnnotationCollectionReference::getSourceDocumentId)
						.collect(Collectors.toSet()),
						DOCUMENTS_DIRECTORY_NAME);
				
				localGitRepoManager.detach();

				for (AnnotationCollectionReference collectionRef : 
						collectionRefs.stream()
						.filter(collectionRef -> verifiedDeleted.contains(collectionRef.getSourceDocumentId()))
						.collect(Collectors.toSet())) {
					
					logger.info(String.format(
						"Removing stale Collection %1$s with ID %2$s "
						+ "due to removal of corresp. Document with ID %3$s",
						collectionRef.getName(), 
						collectionRef.getId(),
						collectionRef.getSourceDocumentId()));
					removeCollection(collectionRef);
				}
			}
		}
	}
	
	// Comment operations  
	
	public List<Comment> getComments(String documentId) throws IOException {
		return remoteGitServerManager.getComments(projectReference, documentId);
	}

	public void addComment(Comment comment) throws IOException {
		remoteGitServerManager.addComment(projectReference, comment);
	}

	public void removeComment(Comment comment) throws IOException {
		remoteGitServerManager.removeComment(projectReference, comment);
	}

	public void updateComment(Comment comment) throws IOException {
		remoteGitServerManager.updateComment(projectReference, comment);
	}

	public void addReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.addReply(projectReference, comment, reply);
	}

	public List<Reply> getCommentReplies(Comment comment) throws IOException {
		return remoteGitServerManager.getCommentReplies(projectReference, comment);
	}
	
	public void updateReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.updateReply(projectReference, comment, reply);
	}
	
	public void removeReply(Comment comment, Reply reply) throws IOException {
		remoteGitServerManager.removeReply(projectReference, comment, reply);
	}

	public List<Comment> getCommentsWithReplies(List<String> documentIdList) throws IOException {
		List<Comment> comments = new ArrayList<Comment>();
		if (documentIdList.isEmpty()) {
			return comments;
		}
		
		if (documentIdList.size() > 10) {
			comments.addAll(remoteGitServerManager.getComments(this.projectReference)
					.stream()
					.filter(comment -> documentIdList.contains(comment.getDocumentId()))
					.collect(Collectors.toList()));
		}
		else {
			for (String documentId : documentIdList) {
				comments.addAll(
					remoteGitServerManager.getComments(projectReference, documentId));
			}
		}
		
		for (Comment comment : comments) {
			if (comment.getReplyCount() > 0) {
				getCommentReplies(comment);
			}
		}
		
		return comments;
	}


	public boolean hasUntrackedChanges() throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project repo
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
			return localGitRepoManager.hasUntrackedChanges();
		}
	}

}