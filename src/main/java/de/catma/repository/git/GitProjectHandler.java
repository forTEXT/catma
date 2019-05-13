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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.TermInfo;
import de.catma.project.TagsetConflict;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.project.conflict.TagConflict;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.JGitRepoManager;
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
	public static final String MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME = "collections";
	public static final String SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME = "documents";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final User user;
	private final String projectId;

	private final IDGenerator idGenerator = new IDGenerator();
	private final CredentialsProvider credentialsProvider;
	
	public GitProjectHandler(User user, String projectId, ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager) {
		super();
		this.user = user;
		this.projectId = projectId;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = new UsernamePasswordCredentialsProvider("oauth2", remoteGitServerManager.getPassword());
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

			localGitRepoManager.open(projectId, GitTagsetHandler.getTagsetRepositoryName(tagsetId));
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
					remoteGitServerManager.getEmail());

			localGitRepoManager.detach(); 
			
			gitTagsetHandler.checkout(
				projectId, tagsetId, ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);

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
	public String createMarkupCollection(String markupCollectionId,
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
					markupCollectionId,
					name,
					description,
					sourceDocumentId,
					sourceDocumentVersion
			);

			// push the newly created markup collection repo to the server in preparation for adding it to the project
			// root repo as a submodule

			localGitRepoManager.open(projectId, GitMarkupCollectionHandler.getMarkupCollectionRepositoryName(markupCollectionId));
			localGitRepoManager.push(credentialsProvider);
			String markupCollectionRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					markupCollectionId
			).toFile();

			// submodule files and the changed .gitmodules file are automatically staged
			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					markupCollectionRepoRemoteUrl,
					credentialsProvider
			);
			
			localGitRepoManager.commit(
				String.format("Added Collection %1$s with ID %2$s", name, markupCollectionId),
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail());
			localGitRepoManager.detach(); 
			
			gitMarkupCollectionHandler.checkout(
				projectId, markupCollectionId, 
				ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH, true);			

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
			
			localRepoManager.open(projectId, GitSourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocumentId));
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
				remoteGitServerManager.getEmail());
			
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
						 .resolve(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME)
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
	
	//NEXT: - 
	//      - deletion handler
	//		- configurable smptp

	public List<UserMarkupCollectionReference> getCollectionReferences() {
		ArrayList<UserMarkupCollectionReference> collectionReferences = new ArrayList<>();
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {

			try {
				localRepoManager.open(
						projectId,
						GitProjectManager.getProjectRootRepositoryName(projectId));
			
				List<Path> paths = localRepoManager.getSubmodulePaths()
					.stream()
					.filter(path -> path.startsWith(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME))
					.map(path -> 
						Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
							 .resolve(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME)
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

	public List<UserMarkupCollection> getCollections(TagLibrary tagLibrary) {
		ArrayList<UserMarkupCollection> collections = new ArrayList<>();
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			try {
				localRepoManager.open(
					projectId,
					GitProjectManager.getProjectRootRepositoryName(projectId));
				 
				Path collectionDirPath = 
					 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME);
				localRepoManager.detach();
				 
				if (!collectionDirPath.toFile().exists()) {
					return collections;
				}
				GitMarkupCollectionHandler gitMarkupCollectionHandler = 
						new GitMarkupCollectionHandler(
								localRepoManager, 
								this.remoteGitServerManager,
								this.credentialsProvider);
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());
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
						RepositoryPropertyKey.GitLabServerUrl.getValue(), 
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
	
	public String commitProject(String msg, boolean all) throws IOException{
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			return localGitRepoManager.commit(msg, remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(), all);
		}
	}

	public String addAndCommitCollection(String collectionId, String commitMsg) throws IOException {
		
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = 
				new GitMarkupCollectionHandler(
					localRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			String revisionHash = gitMarkupCollectionHandler.addAndCommitChanges(
				projectId, collectionId, commitMsg);
			
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
	
	public List<Member> getProjectMembers() throws Exception {
		return remoteGitServerManager.getProjectMembers(Objects.requireNonNull(projectId));
	}

	public void removeTagset(TagsetDefinition tagsetDefinition) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String tagsetId = tagsetDefinition.getUuid();
			
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);

			MergeResult mergeResult = gitTagsetHandler.synchronizeBranchWithRemoteMaster(
				ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
				projectId, tagsetId);
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
						tagsetDefinition.getName(), 
						tagsetDefinition.getUuid()),
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

	public String removeCollection(UserMarkupCollectionReference userMarkupCollectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String collectionId = userMarkupCollectionReference.getId();
			
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
							localGitRepoManager, 
							remoteGitServerManager,
							this.credentialsProvider);

			MergeResult mergeResult = collectionHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, collectionId);
			//TODO: handle merge result
			
			localGitRepoManager.detach();			
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
	
			// remove the submodule only!!!
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
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
			String documentId = sourceDocument.getID();
			
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
					sourceDocument.getID()), 
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail());
		}	
	}

	public void addCollectionToStaged(String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, collectionId);
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			localGitRepoManager.add(relativePath);
		}	
	}
	
	public String addToStagedAndCommit(UserMarkupCollectionReference collectionRef, String commitMsg) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, collectionRef.getId());
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			localGitRepoManager.add(relativePath);
			
			return localGitRepoManager.commit(
					commitMsg, 
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail());			
		}	
	}
	
	public String addTagsetToStagedAndCommit(String tagsetId, String commitMsg) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			return gitTagsetHandler.addAllAndCommit(projectId, tagsetId, commitMsg);
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
					remoteGitServerManager.getEmail());
		}		
	}

	public String updateCollection(UserMarkupCollectionReference userMarkupCollectionReference) throws Exception {
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
						 .resolve(TAGSET_SUBMODULES_DIRECTORY_NAME)
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
					JGitRepoManager.DEFAULT_LOCAL_DEV_BRANCH, true);
			}
			
			GitMarkupCollectionHandler collectionHandler = 
				new GitMarkupCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			for (UserMarkupCollectionReference collectionReference : getCollectionReferences()) {
				collectionHandler.checkout(
					projectId, 
					collectionReference.getId(), 
					JGitRepoManager.DEFAULT_LOCAL_DEV_BRANCH, true);
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

			for (UserMarkupCollectionReference collectionRef : getDocuments().stream()
					.flatMap(doc -> doc.getUserMarkupCollectionRefs().stream())
					.collect(Collectors.toList())) {
				
				if (collectionHandler.hasUncommittedChanges(projectId, collectionRef.getId())) {
					return true;
				}
			}
			
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			return localGitRepoManager.hasUncommitedChanges();
		}
	}

	public void synchronizeWithRemote(TagsetDefinition tagset) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);
			
			MergeResult mergeResult = gitTagsetHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, tagset.getUuid());
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
	
	public Status getStatus(UserMarkupCollectionReference collectionReference) throws IOException {
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
					.resolve(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME);

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
					.resolve(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME);

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
						RepositoryPropertyKey.GitLabServerUrl.getValue(), 
						projectId, 
						annotationConflict.getResolvedTagReferences(),
						tagLibrary);
			gitMarkupCollectionHandler.createTagInstance(projectId, collectionId, annotation);
		}		
	}

	public void synchronizeWithRemote(UserMarkupCollectionReference collectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(
						localGitRepoManager, 
						this.remoteGitServerManager,
						this.credentialsProvider);
			
			MergeResult mergeResult = collectionHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, collectionReference.getId());
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
					remoteGitServerManager.getEmail());
			
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
			
			MergeResult mergeWithOriginMasterResult = 
				localGitRepoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
			
			if (mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
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
			collectionHandler.checkout(collectionId, collectionId, JGitRepoManager.DEFAULT_LOCAL_DEV_BRANCH, false);
			collectionHandler.rebaseToMaster();
		}
	}

	public void checkoutTagsetDevBranchAndRebase(String tagsetId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepoManager, 
					this.remoteGitServerManager,
					this.credentialsProvider);

			gitTagsetHandler.checkout(projectId, tagsetId, JGitRepoManager.DEFAULT_LOCAL_DEV_BRANCH, false);
			gitTagsetHandler.rebaseToMaster(projectId, tagsetId, JGitRepoManager.DEFAULT_LOCAL_DEV_BRANCH);
		}		
	}

	public void initAndUpdateSubmodules() throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			localGitRepoManager.initAndUpdateSubmodules(credentialsProvider);
		}		
		
	}

	public void removeStaleSubmoduleDirectories() throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
			
			List<String> validSubmodulePaths = localRepoManager.getSubmodulePaths();

			Path collectionDirPath = 
				 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
				 .resolve(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME);
			 
			if (collectionDirPath.toFile().exists()) {
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());
				
				for (Path collectionPath : paths) {
					Path relCollectionPath = 
						Paths.get(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, collectionPath.getFileName().toString());
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

}