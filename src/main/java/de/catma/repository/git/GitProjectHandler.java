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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;

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
	
	private IGitProjectResourceProvider resourceProvider;
	
	public GitProjectHandler(
			final User user, final ProjectReference projectReference, 
			final File projectPath,
			final ILocalGitRepositoryManager localGitRepositoryManager,
			final IRemoteGitManagerRestricted remoteGitServerManager) {
		super();
		this.user = user;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.projectId = projectReference.getProjectId();
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = new UsernamePasswordCredentialsProvider("oauth2", remoteGitServerManager.getPassword());
		this.resourceProvider = new SynchronizedResourceProvider(
				projectId, projectReference, projectPath, 
				localGitRepositoryManager, remoteGitServerManager, credentialsProvider);
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
		return this.resourceProvider.getTagsets();
	}

	
	public Set<LatestContribution> getLatestContributions(List<String> branches) throws IOException {
		Set<LatestContribution> latestContributions = new HashSet<LatestContribution>();
		
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			localGitRepoManager.fetch(credentialsProvider);

			List<String> availableBranches = localGitRepoManager.getRemoteBranches();
			branches = branches
					.stream()
					.filter(branch -> availableBranches.contains(branch))
					.collect(Collectors.toList());
			for (String branch : branches) {
				Set<String> modifiedOrAddedPaths = 
					localGitRepoManager.getAdditiveBranchDifferences(branch);
				LatestContribution latestContribution = new LatestContribution(branch);
				for (String path : modifiedOrAddedPaths) {
					if (path.startsWith(ANNOTATION_COLLECTIONS_DIRECTORY_NAME) 
							&& path.contains("annotations")) { // we are only interested in additional or modified Annotations
						latestContribution.addCollectionId(
							path.substring(
									ANNOTATION_COLLECTIONS_DIRECTORY_NAME.length()+1, 
									path.indexOf('/', 
											ANNOTATION_COLLECTIONS_DIRECTORY_NAME.length()+1)));
					}
					else if (path.startsWith(DOCUMENTS_DIRECTORY_NAME)) {
						latestContribution.addDocumentId(
							path.substring(
									DOCUMENTS_DIRECTORY_NAME.length()+1, 
									path.indexOf('/', 
											DOCUMENTS_DIRECTORY_NAME.length()+1)));
					}
					else if (path.startsWith(TAGSETS_DIRECTORY_NAME) 
							&& path.contains("propertydefs")) { // we are only interested in additional or modified Tags
						latestContribution.addTagsetId(
							path.substring(
									TAGSETS_DIRECTORY_NAME.length()+1, 
									path.indexOf('/', 
											TAGSETS_DIRECTORY_NAME.length()+1)));
					}

				}
				
				if (!latestContribution.isEmpty()) {
					latestContributions.add(latestContribution);
				}
			}
		
		}
		
		return latestContributions;
	}
	
	// AnnotationCollection operations
	public String createAnnotationCollection(String collectionId,
										 String name,
										 String description,
										 String sourceDocumentId,
										 String forkedFromCommitURL,
										 boolean withPush
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

			if (withPush) {
				localGitRepoManager.push(credentialsProvider);
			}


			return projectRevisionHash;
		}
	}
	
	public List<AnnotationCollectionReference> getCollectionReferences() {
		return this.resourceProvider.getCollectionReferences();
	}

	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary, ProgressListener progressListener, 
			boolean withOrphansHandling) throws IOException {
		
		return this.resourceProvider.getCollections(
				tagLibrary, progressListener, withOrphansHandling);
	}	
	
	public AnnotationCollection getCollection(
			String collectionId, 
			TagLibrary tagLibrary) throws IOException {
		return this.resourceProvider.getCollection(collectionId, tagLibrary);
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

	public String updateSourceDocument(SourceDocumentReference sourceDocument) throws IOException {
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
		return this.resourceProvider.getDocuments();
	}
	
	public SourceDocument getDocument(String documentId) throws IOException {
		return this.resourceProvider.getDocument(documentId);
	}

	
	public String commitProject(String msg) throws IOException{
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			// open the project root repo
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			if (localGitRepoManager.hasUncommitedChanges() || localGitRepoManager.hasUntrackedChanges()) {
	
				String revisionHash = localGitRepoManager.addAllAndCommit(
						msg, 
						remoteGitServerManager.getUsername(),
						remoteGitServerManager.getEmail(), 
						false);
				
				localGitRepoManager.push(credentialsProvider);
				return revisionHash;
			}
			else {
				return localGitRepoManager.getRevisionHash();
			}
		}
	}
	


	public String removeDocument(SourceDocumentReference sourceDocument) throws Exception {
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
	
	private MergeRequestInfo refreshMergeRequestInfo(MergeRequestInfo mi) throws IOException {
		int tries = 10;
		while (tries > 0 && mi.isMergeStatusCheckInProgress()) {
			try {
				Thread.sleep(1000);
				mi = this.remoteGitServerManager.getMergeRequest(projectReference, mi.getIid());
				tries--;
			} catch (InterruptedException e) {
				break;
			}
		}
		
		return mi;
	}

	public boolean synchronizeWithRemote() throws IOException {
		
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			localGitRepoManager.open(
					projectReference.getNamespace(), projectReference.getProjectId());
			
			boolean pushedAlready = false;
			
			// everything commmitted?
			if (localGitRepoManager.hasUncommitedChanges()) {
				// we perfom an auto commit and push everything
				commitProject("Auto-committing changes");
				localGitRepoManager.push(credentialsProvider);
				pushedAlready = true;
			}
			
			// do we have commits in the user branch that are not present in the master branch? 
			// user branch -> origin/master
			List<CommitInfo> ourUnpublishedChanges = 
					localGitRepoManager.getOurUnpublishedChanges();

			if (!ourUnpublishedChanges.isEmpty()) {
				logger.info(
					String.format(
						"Commits that need to be merged %1$s -> origin/master: %2$s",
						this.user.getIdentifier(),
						ourUnpublishedChanges
							.stream()
							.map(CommitInfo::toString)
							.collect(Collectors.joining(System.lineSeparator()))
					)
				);
				
				// make sure everyting is pushed
				if (!pushedAlready) {
					localGitRepoManager.push(credentialsProvider);
					pushedAlready = true;
				}
				
				// check for existing merge requests origin/userBranch -> origin/master
				List<MergeRequestInfo> openMergeRequests =  
						this.remoteGitServerManager.getOpenMergeRequests(projectReference);
				
				if (openMergeRequests.isEmpty()) {
					// create and merge a merge request origin/userBranch -> origin/master
					MergeRequestInfo mi = 
						this.remoteGitServerManager.createMergeRequest(projectReference);
					mi = refreshMergeRequestInfo(mi);
					logger.info(
							String.format("Created Merge Request %1$s", mi));
					MergeRequestInfo result = 
							this.remoteGitServerManager.mergeMergeRequest(mi);
					logger.info(
							String.format("Merged Merge Request %1$s", result));
					
					if (!result.isMerged()) {
						return false;
					}
				}
				else {
					logger.info(
						String.format(
							"Merge requests origin/%1$s -> origin/master: %2$s",
							this.user.getIdentifier(),
							openMergeRequests
								.stream()
								.map(MergeRequestInfo::toString)
								.collect(Collectors.joining(System.lineSeparator()))
						)
					);
					// merge all open merge requests origin/userBranch -> origin/master
					for (MergeRequestInfo mi : openMergeRequests) {
						mi = refreshMergeRequestInfo(mi);
						if (mi.isOpen() && mi.canBeMerged()) {
							MergeRequestInfo result = 
									this.remoteGitServerManager.mergeMergeRequest(mi);
							logger.info(
									String.format("Merged Merge Request %1$s", result));
							if (!result.isMerged()) {
								return false;
							}
						}
					}
				}
			}
			
			// fetch latest commits 
			// we are interested in updating the local origin/master here
			localGitRepoManager.fetch(credentialsProvider);
			
			
			// get commits that need to be merged to the local user branch 
			// origin/master -> userBranch
			List<CommitInfo> theirPublishedChanges = localGitRepoManager.getTheirPublishedChanges();

			if (!theirPublishedChanges.isEmpty()) {
				logger.info(
					String.format(
						"Commits that need to be merged origin/master -> %1$s: %2$s",
						this.user.getIdentifier(),
						theirPublishedChanges
							.stream()
							.map(CommitInfo::toString)
							.collect(Collectors.joining(System.lineSeparator()))
					)
				);				
				// did we already push the local user branch?
				if (localGitRepoManager.hasRef(
						Constants.DEFAULT_REMOTE_NAME + "/" + remoteGitServerManager.getUsername())) {
					
					// can we merge origin/master -> userBranch?
					if (localGitRepoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)) {					
						MergeResult mergeWithOriginMasterResult = 
								localGitRepoManager.merge(
										Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
						
						// push merge commit if successfull
						if (mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
							localGitRepoManager.push(credentialsProvider);
						}
						else { // or abort
							localGitRepoManager.abortMerge(mergeWithOriginMasterResult);
							return false;
						}
					}
					else {
						return false;
					}
					
				}
			}

			return true;
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

	public Map getDocumentIndex(String documentId, Path tokensPath) throws IOException {
		return new Gson().fromJson(FileUtils.readFileToString(tokensPath.toFile(), "UTF-8"), Map.class);
	}

	public void setResourceProvider(IGitProjectResourceProviderFactory resourceProviderFactory) {
		this.resourceProvider = resourceProviderFactory.createResourceProvider(
				projectId, projectReference, projectPath, 
				localGitRepositoryManager, remoteGitServerManager, credentialsProvider);
	}
	
	public boolean isReadOnly() {
		return this.resourceProvider.isReadOnly();
	}

}