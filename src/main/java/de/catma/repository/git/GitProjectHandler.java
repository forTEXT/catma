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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.indexer.TermInfo;
import de.catma.project.TagsetConflict;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.TagConflict;
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
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.IDGenerator;

public class GitProjectHandler {

	private Logger logger = Logger.getLogger(GitProjectHandler.class.getName());
	
	public static final String TAGSET_SUBMODULES_DIRECTORY_NAME = "tagsets";
	public static final String ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME = "collections";
	public static final String SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME = "documents";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final User user;
	private final String projectId;

	private final IDGenerator idGenerator = new IDGenerator();
	private final CredentialsProvider credentialsProvider;

	private Map<String, RBACRole> rolesPerResource;
	
	public GitProjectHandler(User user, String projectId, ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager) {
		super();
		this.user = user;
		this.projectId = projectId;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = new UsernamePasswordCredentialsProvider("oauth2", remoteGitServerManager.getPassword());
	}
	
	public void loadRolesPerResource() throws Exception {
		this.rolesPerResource = remoteGitServerManager.getRolesPerResource(projectId);
	}

	// tagset operations
	public String createTagset(String tagsetId,
							   String name,
							   String description
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
							localGitRepoManager, 
							this.remoteGitServerManager,
							this.credentialsProvider);

			// create the tagset
			String tagsetRevisionHash = gitTagsetHandler.create(projectId, tagsetId, name, description);

			localGitRepoManager.open(projectId, tagsetId);
			localGitRepoManager.push(credentialsProvider);
			String tagsetRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					TAGSET_SUBMODULES_DIRECTORY_NAME,
					tagsetId
			).toFile();

			// submodule files and the changed .gitmodules file are automatically staged
			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					tagsetRepoRemoteUrl,
					credentialsProvider
			);
			
			localGitRepoManager.commit(
					String.format("Added Tagset %1$s with ID %2$s", name, tagsetId),
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					false);

			localGitRepoManager.detach(); 
			
			gitTagsetHandler.checkout(
				projectId, tagsetId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);

			rolesPerResource.put(
				tagsetId, 
				RBACRole.OWNER);
			
			return tagsetRevisionHash;
		}
	}
	
	public String createOrUpdateTag(String tagsetId, TagDefinition tagDefinition, String commitMsg) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			
			if (tagDefinition.getPropertyDefinition(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()) == null) {
				PropertyDefinition authorPropertyDefinition = 
						new PropertyDefinition(
							idGenerator.generate(),
							PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
							Collections.singleton(user.getIdentifier()));
				tagDefinition.addSystemPropertyDefinition(authorPropertyDefinition);
			}
			
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			String tagsetRevision = 
				gitTagsetHandler.createOrUpdateTagDefinition(
						projectId, tagsetId, tagDefinition, commitMsg);

			return tagsetRevision;
		}
	}
	
	public String removeTag(TagsetDefinition tagsetDefinition, TagDefinition tagDefinition) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager, 
					this.credentialsProvider);

			String tagsetRevision = 
				gitTagsetHandler.removeTagDefinition(projectId, tagsetDefinition, tagDefinition);
			
			return tagsetRevision;
		}
	}

	// markup collection operations
	public String createMarkupCollection(String collectionId,
										 String name,
										 String description,
										 String sourceDocumentId,
										 String sourceDocumentVersion
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			GitMarkupCollectionHandler gitMarkupCollectionHandler = 
					new GitMarkupCollectionHandler(
							localGitRepoManager, 
							this.remoteGitServerManager, 
							this.credentialsProvider
			);

			// create the markup collection
			String revisionHash = gitMarkupCollectionHandler.create(
					projectId,
					collectionId,
					name,
					description,
					sourceDocumentId,
					sourceDocumentVersion
			);

			// push the newly created markup collection repo to the server in preparation for adding it to the project
			// root repo as a submodule

			localGitRepoManager.open(
					projectId, 
					collectionId);
			localGitRepoManager.push(credentialsProvider);
			String markupCollectionRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					collectionId
			).toFile();

			// submodule files and the changed .gitmodules file are automatically staged
			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					markupCollectionRepoRemoteUrl,
					credentialsProvider
			);
			
			localGitRepoManager.commit(
				String.format("Added Collection %1$s with ID %2$s", name, collectionId),
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail(),
				false);
			localGitRepoManager.detach(); 
			
			gitMarkupCollectionHandler.checkout(
				projectId, collectionId, 
				ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);			

			rolesPerResource.put(
				collectionId, 
				RBACRole.OWNER);
			
			return revisionHash;
		}
	}

	// source document operations

	/**
	 * Creates a new source document within the project identified by <code>projectId</code>.
	 *
	 * @param sourceDocumentId the ID of the source document to create. If none is provided, a new
	 *                         ID will be generated.
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original,
	 *                                     unmodified source document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified source
	 *                                       document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted,
	 *                                      UTF-8 encoded source document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded source
	 *                                        document
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object
	 * @param terms 
	 * @return the revisionHash
	 * @throws IOException if an error occurs while creating the source document
	 */
	public String createSourceDocument(
			String sourceDocumentId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			Map<String, List<TermInfo>> terms, String tokenizedSourceDocumentFileName,
			SourceDocumentInfo sourceDocumentInfo
	) throws IOException {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitSourceDocumentHandler gitSourceDocumentHandler =
				new GitSourceDocumentHandler(
						localRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider
				);

			// create the source document within the project
			String revisionHash = gitSourceDocumentHandler.create(
					projectId, sourceDocumentId,
					originalSourceDocumentStream, originalSourceDocumentFileName,
					convertedSourceDocumentStream, convertedSourceDocumentFileName,
					terms, tokenizedSourceDocumentFileName,
					sourceDocumentInfo
			);
			
			localRepoManager.open(projectId, sourceDocumentId);
			localRepoManager.push(credentialsProvider);

			String remoteUri = localRepoManager.getRemoteUrl(null);
			localRepoManager.close();

			// open the project root repository
			localRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// create the submodule
			File targetSubmodulePath = Paths.get(
					localRepoManager.getRepositoryWorkTree().getAbsolutePath(),
					SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
					sourceDocumentId
			).toFile();
			
			sourceDocumentInfo.getTechInfoSet().setURI(
					Paths.get(
						targetSubmodulePath.getAbsolutePath(), 
						convertedSourceDocumentFileName)
					.toUri());

			// submodule files and the changed .gitmodules file are automatically staged
			localRepoManager.addSubmodule(
				targetSubmodulePath, remoteUri,
				credentialsProvider
			);
			
			String name = sourceDocumentInfo.getContentInfoSet().getTitle();
			if ((name == null) || name.isEmpty()) {
				name = "N/A";
			}
			localRepoManager.commit(
				String.format("Added Document %1$s with ID %2$s", name, sourceDocumentId),
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail(),
				false);
			
			rolesPerResource.put(
				sourceDocumentId, 
				RBACRole.OWNER);
			
			return revisionHash;
		}

	}

	public String getRootRevisionHash() throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(
				projectId,
				GitProjectManager.getProjectRootRepositoryName(projectId));
			return localRepoManager.getRevisionHash();
		}
	}
	
	public String publishChanges() throws Exception {
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(
				projectId,
				GitProjectManager.getProjectRootRepositoryName(projectId));
				//TODO
			// get involved tagsets
			// check for modifications
			// fetch/merge master / merge dev into master
			// push master
			// rebase dev to master
			
			// get involved collections
			// check for modifications
			// fetch/merge master / merge dev into master
			// push master
			// rebase dev to master

			
			// how to set root project submodule checksums correctly ?
			// checkout submodules on commit hashes of master HEAD 
			// add and commit changes
			
			//System.out.println(localRepoManager.getRevisionHash("tagsets/CATMA_6CF585DD-6D52-4571-B245-F94F2BC6789A"));

			
		
		
			return null;
		}
	}

	public Path getSourceDocumentSubmodulePath(String sourceDocumentId) {
		return Paths.get(
			user.getIdentifier(), 
			projectId, 
			GitProjectManager.getProjectRootRepositoryName(projectId), 
			SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
			sourceDocumentId);
	}

	public List<SourceDocument> getDocuments() {
		ArrayList<SourceDocument> documents = new ArrayList<>();
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			
			localRepoManager.open(
					projectId,
					GitProjectManager.getProjectRootRepositoryName(projectId));
		
			List<Path> paths = localRepoManager.getSubmodulePaths()
				.stream()
				.filter(path -> path.startsWith(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME))
				.map(path -> 
					Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
						 .resolve(path))
				.collect(Collectors.toList());

			localRepoManager.detach();
			
			GitSourceDocumentHandler gitSourceDocumentHandler = 
				new GitSourceDocumentHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			for (Path sourceDocPath : paths) {
				String sourceDocumentId = sourceDocPath.getFileName().toString();
				if (hasPermission(getRoleForDocument(sourceDocumentId), RBACPermission.DOCUMENT_READ)) {
					try {
						documents.add(gitSourceDocumentHandler.open(projectId, sourceDocumentId));
					} catch (Exception e) {
						logger.log(
							Level.SEVERE,
							String.format(
								"error loading Document %1$s for project %2$s",
								sourceDocPath,
								projectId), 
							e);					
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(
				Level.SEVERE,
				String.format(
					"error loading Documents for project %1$s",
					projectId), 
				e);					
		}
		
		return documents;
	}

	
	public List<AnnotationCollectionReference> getCollectionReferences() {
		ArrayList<AnnotationCollectionReference> collectionReferences = new ArrayList<>();
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {

			try {
				localRepoManager.open(
						projectId,
						GitProjectManager.getProjectRootRepositoryName(projectId));
			
				List<Path> paths = localRepoManager.getSubmodulePaths()
					.stream()
					.filter(path -> path.startsWith(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME))
					.map(path -> 
						Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
							 .resolve(path))
					.collect(Collectors.toList());

				localRepoManager.detach();	

				GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
						localRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
				
				for (Path collectionPath : paths) {
					String collectionId = collectionPath.getFileName().toString();
					try {
						collectionReferences.add(
							gitMarkupCollectionHandler.getCollectionReference(projectId, collectionId));
					} catch (Exception e) {
						logger.log(
						Level.SEVERE, 
							String.format(
								"error loading Collection reference %1$s for project %2$s",
								collectionPath,
								projectId), 
							e);
						 
					}
				}
			}
			catch (Exception e) {
				logger.log(
					Level.SEVERE, 
					"error loading Collection references for project %1$s" + projectId, e);
			}
		}
		return collectionReferences;
	}

	public List<AnnotationCollection> getCollections(TagLibrary tagLibrary) {
		ArrayList<AnnotationCollection> collections = new ArrayList<>();
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			try {
				localRepoManager.open(
					projectId,
					GitProjectManager.getProjectRootRepositoryName(projectId));
				 
				Path collectionDirPath = 
					 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME);
				 
				if (!collectionDirPath.toFile().exists()) {
					return collections;
				}
				
				List<Path> paths = localRepoManager.getSubmodulePaths()
						.stream()
						.filter(path -> path.startsWith(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME))
						.map(path -> 
							Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
								 .resolve(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME)
								 .resolve(path))
						.collect(Collectors.toList());
				localRepoManager.detach();
				
				GitMarkupCollectionHandler gitMarkupCollectionHandler = 
						new GitMarkupCollectionHandler(
								localRepoManager, 
								this.remoteGitServerManager,
								this.credentialsProvider);
				
				for (Path collectionPath : paths) {
					String collectionId = collectionPath.getFileName().toString();
					try {
						collections.add(
							gitMarkupCollectionHandler.getCollection(projectId, collectionId, tagLibrary));
					} catch (Exception e) {
						logger.log(
						Level.SEVERE, 
							String.format(
								"error loading Collection reference %1$s for project %2$s",
								collectionPath,
								projectId), 
							e);
						 
					}
				}
			}
			catch (Exception e) {
				logger.log(
					Level.SEVERE, 
					"error loading Collection references for project %1$s" + projectId, e);
			}
		}
		return collections;
	}	
	public void addOrUpdate(String collectionId, Collection<TagReference> tagReferenceList, TagLibrary tagLibrary) throws IOException {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			JsonLdWebAnnotation annotation = 
					new JsonLdWebAnnotation(
						CATMAPropertyKey.GitLabServerUrl.getValue(), 
						projectId, 
						tagReferenceList,
						tagLibrary);
			gitMarkupCollectionHandler.createTagInstance(projectId, collectionId, annotation);
		}		
	}

	public String removeTagInstancesAndCommit(
		String collectionId, Collection<String> deletedTagInstanceIds, String commitMsg) throws IOException {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = 
					new GitMarkupCollectionHandler(
						localRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			return gitMarkupCollectionHandler.removeTagInstancesAndCommit(
					projectId, collectionId, deletedTagInstanceIds, commitMsg);
		}	
	}

	public void removeTagInstance(
		String collectionId, String deletedTagInstanceId) throws IOException {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = 
				new GitMarkupCollectionHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			gitMarkupCollectionHandler.removeTagInstances(
				projectId, collectionId, Collections.singleton(deletedTagInstanceId));
		}	
	}
	
	public String commitProject(String msg) throws IOException{
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			return localGitRepoManager.commitWithSubmodules(msg, remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(), getReadableSubmodules(localGitRepoManager));
		}
	}

	public String addCollectionToStagedAndCommit(String collectionId, String commitMsg, boolean force) throws IOException {
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = 
				new GitMarkupCollectionHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			String revisionHash = gitMarkupCollectionHandler.addAndCommitChanges(
				projectId, collectionId, commitMsg, force);
			
			return revisionHash;
		}	
		
	}

	public String removePropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			String tagsetRevision = 
				gitTagsetHandler.removePropertyDefinition(projectId, tagsetDefinition, tagDefinition, propertyDefinition);
			
			return tagsetRevision;
		}
	}
	
	public Set<Member> getProjectMembers() throws IOException {
		return remoteGitServerManager.getProjectMembers(Objects.requireNonNull(projectId));
	}

	public void removeTagset(TagsetDefinition tagset) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String tagsetId = tagset.getUuid();
			
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);

			MergeResult mergeResult = gitTagsetHandler.synchronizeBranchWithRemoteMaster(
				ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
				projectId, tagsetId, hasPermission(
						getRoleForTagset(tagset.getUuid()), RBACPermission.TAGSET_WRITE));
			//TODO: handle mergeresult
			
			
			localGitRepoManager.detach();
			
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
	
			// remove the submodule only!!!
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					TAGSET_SUBMODULES_DIRECTORY_NAME,
					tagsetId
			).toFile();
	
			localGitRepoManager.removeSubmodule(
					targetSubmodulePath,
					String.format(
						"Removed Tagset %1$s with ID %2$s", 
						tagset.getName(), 
						tagset.getUuid()),
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail());
		}		
	}

	public String updateTagset(TagsetDefinition tagsetDefinition) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			String tagsetRevision = 
				gitTagsetHandler.updateTagsetDefinition(projectId, tagsetDefinition);
			
			
			return tagsetRevision;
		}
	}

	public String removeCollection(AnnotationCollectionReference userMarkupCollectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String collectionId = userMarkupCollectionReference.getId();
			
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
							localGitRepoManager, 
							remoteGitServerManager,
							this.credentialsProvider);

			MergeResult mergeResult = collectionHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, collectionId, 
					hasPermission(getRoleForCollection(collectionId), RBACPermission.COLLECTION_WRITE));
			//TODO: handle merge result
			
			localGitRepoManager.detach();			
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
	
			// remove the submodule only!!!
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					collectionId
			).toFile();
	
			return localGitRepoManager.removeSubmodule(
				targetSubmodulePath,
				String.format(
					"Removed Collection %1$s with ID %2$s", 
					userMarkupCollectionReference.getName(), 
					userMarkupCollectionReference.getId()), 
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail());
		}		
	}

	public void removeDocument(SourceDocument sourceDocument) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String documentId = sourceDocument.getUuid();
			
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

	public void addCollectionToStaged(String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME, collectionId);
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			localGitRepoManager.add(relativePath);
		}	
	}
	
	public String addCollectionSubmoduleToStagedAndCommit(
			String collectionId, String commitMsg, boolean force) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME, collectionId);
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
	
	public String addTagsetToStagedAndCommit(String tagsetId, String commitMsg) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			return gitTagsetHandler.addAllAndCommit(projectId, tagsetId, commitMsg, true);
		}		
	}
	
	public String addTagsetSubmoduleToStagedAndCommit(String tagsetId, String commitMsg) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetId);
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

	public String updateCollection(AnnotationCollectionReference userMarkupCollectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler collectionHandler = 
				new GitMarkupCollectionHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			String collectionRevision = 
				collectionHandler.updateCollection(projectId, userMarkupCollectionReference);
			return collectionRevision;
		}		
	}

	public List<TagsetDefinition> getTagsets() {
		ArrayList<TagsetDefinition> result = new ArrayList<>();
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			
			localRepoManager.open(
					 projectId,
					 GitProjectManager.getProjectRootRepositoryName(projectId));
			List<Path> paths = localRepoManager.getSubmodulePaths()
				.stream()
				.filter(path -> path.startsWith(TAGSET_SUBMODULES_DIRECTORY_NAME))
				.map(path -> 
					Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
						 .resolve(path))
				.collect(Collectors.toList());			
			 localRepoManager.detach();
			 
			 GitTagsetHandler gitTagsetHandler = 
				 new GitTagsetHandler(
						 localRepoManager, 
						 this.remoteGitServerManager,
						 this.credentialsProvider);
			 for (Path tagsetPath : paths) {
				 try {
					 TagsetDefinition tagset = gitTagsetHandler.getTagset(
							 projectId, 
							 tagsetPath.getFileName().toString());						 
					 result.add(tagset);
				 }
				 catch (Exception e) {
					logger.log(
						Level.SEVERE,
						String.format(
							"error loading Tagset %1$s for project %2$s",
							tagsetPath,
							projectId), 
						e);
				 }
				 
			 }
		}
		catch (Exception e) {
			logger.log(
				Level.SEVERE, 
				String.format("error loading Tagsets for project %1$s", projectId), 
				e);
		}
		
		return result;
	}

	public void ensureDevBranches() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			for (TagsetDefinition tagset : getTagsets()) {
				gitTagsetHandler.checkout(
					projectId, tagset.getUuid(), 
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);
			}
			
			GitMarkupCollectionHandler collectionHandler = 
				new GitMarkupCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			for (AnnotationCollectionReference collectionReference : getCollectionReferences()) {
				collectionHandler.checkout(
					projectId, 
					collectionReference.getId(), 
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);
			}
		}		
	}

	public boolean hasUncommittedChanges() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
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

	public void synchronizeTagsetWithRemote(String tagsetId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			MergeResult mergeResult = gitTagsetHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, tagsetId, hasPermission(
								getRoleForTagset(tagsetId), RBACPermission.TAGSET_WRITE));
			//TODO: handle mergeResult
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
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
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
			
			localGitRepoManager.open(
				 projectId,
				 GitProjectManager.getProjectRootRepositoryName(projectId));
			 
			Path tagsetsDirPath = 
					Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(TAGSET_SUBMODULES_DIRECTORY_NAME);
			Path collectionDirPath = 
					Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME);

			Status projectStatus = localGitRepoManager.getStatus();
			localGitRepoManager.detach();
			 
			if (tagsetsDirPath.toFile().exists()) {
				GitTagsetHandler gitTagsetHandler = 
						new GitTagsetHandler(
							localGitRepoManager, 
							this.remoteGitServerManager,
							this.credentialsProvider);
				List<Path> paths = Files
						.walk(tagsetsDirPath, 1)
						.filter(tagsetPath -> !tagsetsDirPath.equals(tagsetPath))
						.collect(Collectors.toList());
				for (Path tagsetPath : paths) {
					if (tagsetPath.toFile().list().length > 0) { // empty directories are submodules not yet initialized 
						String tagsetId = tagsetPath.getFileName().toString();						 
						Status status = gitTagsetHandler.getStatus(projectId, tagsetId);
						if (!status.getConflicting().isEmpty()) {
							StatusPrinter.print("Tagset #" +tagsetId , status, System.out); 
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
				
				GitMarkupCollectionHandler gitCollectionHandler = 
						new GitMarkupCollectionHandler(
								localGitRepoManager, 
								this.remoteGitServerManager,
								this.credentialsProvider);

				for (Path collectionPath : paths) {
					if (collectionPath.toFile().list().length > 0) { // empty directories are submodules not yet initialized 
						String collectionId = collectionPath.getFileName().toString();
						Status status = gitCollectionHandler.getStatus(projectId, collectionId);
						if (!status.getConflicting().isEmpty()) {
							StatusPrinter.print("Collection #" +collectionId , status, System.out); 
							return true;
						}					
					}
				}
				
			}
			
			if (!projectStatus.getConflicting().isEmpty()) {
				StatusPrinter.print("Project #" +projectId , projectStatus, System.out); 
				return true;				
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
					.resolve(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME);

			localGitRepoManager.detach();

			if (collectionDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());
				
				GitMarkupCollectionHandler gitCollectionHandler = 
						new GitMarkupCollectionHandler(
								localGitRepoManager, 
								this.remoteGitServerManager,
								this.credentialsProvider);

				for (Path collectionPath : paths) {
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
		
		return collectionConflicts;
	}

	public void resolveAnnotationConflict(
			String collectionId, 
			AnnotationConflict annotationConflict,
			TagLibrary tagLibrary) throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			JsonLdWebAnnotation annotation = 
					new JsonLdWebAnnotation(
						CATMAPropertyKey.GitLabServerUrl.getValue(), 
						projectId, 
						annotationConflict.getResolvedTagReferences(),
						tagLibrary);
			gitMarkupCollectionHandler.createTagInstance(projectId, collectionId, annotation);
		}		
	}

	public void synchronizeCollectionWithRemote(String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			MergeResult mergeResult = collectionHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, collectionId,
					hasPermission(getRoleForCollection(collectionId), RBACPermission.COLLECTION_WRITE));
			//TODO: handle mergeResult			
		}
	}

	public void resolveRootConflicts() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
				 projectId,
				 GitProjectManager.getProjectRootRepositoryName(projectId));
			
			Status status = localGitRepoManager.getStatus();
			
			StatusPrinter.print("Project status", status, System.out);

			localGitRepoManager.resolveRootConflicts(this.credentialsProvider);
				
			status = localGitRepoManager.getStatus();

			localGitRepoManager.addAllAndCommit(
					"Auto-committing merged changes",
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					true);
			
			localGitRepoManager.push(credentialsProvider);
			
		}
	}

	public List<TagsetConflict> getTagsetConflicts() throws Exception {
		
		ArrayList<TagsetConflict> tagsetConflicts = new ArrayList<>();
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			localGitRepoManager.open(
				 projectId,
				 GitProjectManager.getProjectRootRepositoryName(projectId));
			 
			Path tagsetDirPath = 
					Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					.resolve(TAGSET_SUBMODULES_DIRECTORY_NAME);

			localGitRepoManager.detach();

			if (tagsetDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(tagsetDirPath, 1)
						.filter(tagsetPath -> !tagsetDirPath.equals(tagsetPath))
						.collect(Collectors.toList());
				
				GitTagsetHandler gitTagsetHandler = 
						new GitTagsetHandler(
								localGitRepoManager, 
								this.remoteGitServerManager,
								this.credentialsProvider);

				for (Path tagsetPath : paths) {
					String tagsetId = tagsetPath.getFileName().toString();
					Status status = gitTagsetHandler.getStatus(projectId, tagsetId);
					if (!status.getConflicting().isEmpty()) {
						tagsetConflicts.add(
								gitTagsetHandler.getTagsetConflict(
										projectId, tagsetId));
					}					
				}
				
			}
		}
		
		return tagsetConflicts;	
		
	}

	public void resolveTagConflict(String tagsetId, TagConflict tagConflict) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			TagDefinition tagDefinition = tagConflict.getResolvedTagDefinition();
			gitTagsetHandler.createOrUpdateTagDefinition(
					projectId, tagsetId, tagDefinition);

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
					localGitRepoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
				
				if (mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
					localGitRepoManager.push(credentialsProvider);
				}
			}
			else {
				localGitRepoManager.push(credentialsProvider);
			}
		}	
	}

	public void checkoutCollectionDevBranchAndRebase(String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
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
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			localGitRepoManager.initAndUpdateSubmodules(credentialsProvider, getReadableSubmodules(localGitRepoManager));
		}		
		
	}
	
	private Set<String> getReadableSubmodules(ILocalGitRepositoryManager localRepoManager) throws IOException {
		Set<String> readableSubmodules = new HashSet<>();
		
			
		List<String> relativeSubmodulePaths = localRepoManager.getSubmodulePaths();
		
		for (String relativeSubmodulePath : relativeSubmodulePaths) {
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
			else if (relativeSubmodulePath.startsWith(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME)) {
				String collectionId = Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(relativeSubmodulePath)
					 .getFileName()
					 .toString();
				RBACRole resourceRole = rolesPerResource.get(collectionId);
				if ((resourceRole != null) && hasPermission(resourceRole, RBACPermission.COLLECTION_READ)) {
					readableSubmodules.add(relativeSubmodulePath);
				}
			}
			else if (relativeSubmodulePath.startsWith(TAGSET_SUBMODULES_DIRECTORY_NAME)) {
				String collectionId = Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(relativeSubmodulePath)
					 .getFileName()
					 .toString();
				RBACRole resourceRole = rolesPerResource.get(collectionId);
				if ((resourceRole != null) && hasPermission(resourceRole, RBACPermission.TAGSET_READ)) {
					readableSubmodules.add(relativeSubmodulePath);
				}
			}
		}

		return readableSubmodules;
	}

	public void removeStaleSubmoduleDirectories() throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			List<String> validSubmodulePaths = localRepoManager.getSubmodulePaths();

			Path collectionDirPath = 
				 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
				 .resolve(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME);
			 
			if (collectionDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());
				
				for (Path collectionPath : paths) {
					Path relCollectionPath = 
						Paths.get(ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME, collectionPath.getFileName().toString());
					String relCollectionPathUnix = FilenameUtils.separatorsToUnix(relCollectionPath.toString());
					if (!validSubmodulePaths.contains(relCollectionPathUnix)) {
						FileUtils.deleteDirectory(
								collectionPath.toFile());
					}
				}
			}
			Path docsDirPath = 
					 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME);
				 
			if (docsDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(docsDirPath, 1)
						.filter(collectionPath -> !docsDirPath.equals(collectionPath))
						.collect(Collectors.toList());
				
				for (Path docPath : paths) {
					Path relDocPath = 
						Paths.get(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, docPath.getFileName().toString());
					String relDocPathUnix = FilenameUtils.separatorsToUnix(relDocPath.toString());
					if (!validSubmodulePaths.contains(relDocPathUnix)) {
						FileUtils.deleteDirectory(
								docPath.toFile());
					}
				}
			}
			
			Path tagsetDirPath = 
					 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(TAGSET_SUBMODULES_DIRECTORY_NAME);
				 
			if (tagsetDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(tagsetDirPath, 1)
						.filter(collectionPath -> !tagsetDirPath.equals(collectionPath))
						.collect(Collectors.toList());
				
				for (Path tagsetPath : paths) {
					Path relTagsetPath = 
						Paths.get(TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetPath.getFileName().toString());
					String relTagsetPathUnix = FilenameUtils.separatorsToUnix(relTagsetPath.toString());
					if (!validSubmodulePaths.contains(relTagsetPathUnix)) {
						FileUtils.deleteDirectory(
								tagsetPath.toFile());
					}
				}
			}			
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
		return remoteGitServerManager.isAuthorizedOnProject(remoteGitServerManager.getUser(), permission, projectId);
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
}