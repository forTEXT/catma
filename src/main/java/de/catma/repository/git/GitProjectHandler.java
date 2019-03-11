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

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.TermInfo;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
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
	private IDGenerator idGenerator = new IDGenerator();

	public GitProjectHandler(GitUser user, String projectId, ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager) {
		super();
		this.user = user;
		this.projectId = projectId;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	// tagset operations
	public String createTagset(String tagsetId,
							   String name,
							   String description
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			// create the tagset
			String tagsetRevisionHash = gitTagsetHandler.create(projectId, tagsetId, name, description);

			localGitRepoManager.open(projectId, GitTagsetHandler.getTagsetRepositoryName(tagsetId));
			localGitRepoManager.push(remoteGitServerManager.getUsername(), remoteGitServerManager.getPassword());
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
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
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
				new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			String tagsetRevision = 
				gitTagsetHandler.createOrUpdateTagDefinition(projectId, tagsetId, tagDefinition, commitMsg);

			return tagsetRevision;
		}
	}
	
	public String removeTag(TagsetDefinition tagsetDefinition, TagDefinition tagDefinition) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

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
			
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localGitRepoManager, this.remoteGitServerManager
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
			localGitRepoManager.push(remoteGitServerManager.getUsername(), remoteGitServerManager.getPassword());
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
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
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
			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
				localRepoManager, this.remoteGitServerManager
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
			localRepoManager.push(remoteGitServerManager.getUsername(), remoteGitServerManager.getPassword());

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
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getPassword()
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
			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					localRepoManager, this.remoteGitServerManager);
			
			localRepoManager.open(
					projectId,
					GitProjectManager.getProjectRootRepositoryName(projectId));
			 
			Path sourceDirPath = 
					Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					.resolve(SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME);
			localRepoManager.detach();
			if (!sourceDirPath.toFile().exists()) {
				return documents;
			}
			 
			 
			List<Path> paths = Files
				 .walk(sourceDirPath, 1)
				 .filter(sourceDocPath -> !sourceDocPath.equals(sourceDirPath))
				 .collect(Collectors.toList());
			for (Path sourceDocPath : paths) {
				String sourceDocumentId = sourceDocPath.getFileName().toString();
				try {
					documents.add(gitSourceDocumentHandler.open(projectId, sourceDocumentId));
				} catch (Exception e) {
					logger.log(
						Level.SEVERE,
						String.format(
							"error loading Document %1$s for project %2$s",
							sourceDirPath,
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

	public List<UserMarkupCollectionReference> getCollectionReferences() {
		ArrayList<UserMarkupCollectionReference> collectionReferences = new ArrayList<>();
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, this.remoteGitServerManager);
			try {
				localRepoManager.open(
					projectId,
					GitProjectManager.getProjectRootRepositoryName(projectId));
				 
				Path collectionDirPath = 
					 Paths.get(localRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME);
				localRepoManager.detach();
				 
				if (!collectionDirPath.toFile().exists()) {
					return collectionReferences;
				}
				List<Path> paths = Files
						.walk(collectionDirPath, 1)
						.filter(collectionPath -> !collectionDirPath.equals(collectionPath))
						.collect(Collectors.toList());
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
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, this.remoteGitServerManager);
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
					localRepoManager, this.remoteGitServerManager);
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
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, this.remoteGitServerManager);
			
			return gitMarkupCollectionHandler.removeTagInstancesAndCommit(
					projectId, collectionId, deletedTagInstanceIds, commitMsg);
		}	
	}

	public void removeTagInstance(
		String collectionId, String deletedTagInstanceId) throws IOException {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, this.remoteGitServerManager);
			gitMarkupCollectionHandler.removeTagInstances(projectId, collectionId, Collections.singleton(deletedTagInstanceId));
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
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localRepoManager, this.remoteGitServerManager);
			String revisionHash = gitMarkupCollectionHandler.addAndCommitChanges(
				projectId, collectionId, commitMsg);
			
			
			return revisionHash;
		}	
		
	}

	public String removePropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			String tagsetRevision = 
				gitTagsetHandler.removePropertyDefinition(projectId, tagsetDefinition, tagDefinition, propertyDefinition);
			
			return tagsetRevision;
		}
	}
	
	public List<User> getProjectMembers() throws Exception {
		return remoteGitServerManager.getProjectMembers(Objects.requireNonNull(projectId));
	}

	public void removeTagset(TagsetDefinition tagsetDefinition) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String tagsetId = tagsetDefinition.getUuid();
			
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(localGitRepositoryManager, remoteGitServerManager);

			gitTagsetHandler.synchronizeBranchWithRemoteMaster(
				ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
				projectId, tagsetId);
			
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
				new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			String tagsetRevision = 
				gitTagsetHandler.updateTagsetDefinition(projectId, tagsetDefinition);
			
			
			return tagsetRevision;
		}
	}

	public String removeCollection(UserMarkupCollectionReference userMarkupCollectionReference) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String collectionId = userMarkupCollectionReference.getId();
			
			GitMarkupCollectionHandler collectionHandler = 
					new GitMarkupCollectionHandler(localGitRepoManager, remoteGitServerManager);

			collectionHandler.synchronizeBranchWithRemoteMaster(
					ILocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH,
					projectId, collectionId);
				
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
	
	
	public String addToStagedAndCommit(TagsetDefinition tagsetDefinition, String commitMsg) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			Path relativePath = Paths.get(TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetDefinition.getUuid());
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
				new GitMarkupCollectionHandler(localGitRepoManager, remoteGitServerManager);
			String collectionRevision = 
				collectionHandler.updateCollection(projectId, userMarkupCollectionReference);
			return collectionRevision;
		}		
	}

	public List<TagsetDefinition> getTagsets() {
		ArrayList<TagsetDefinition> result = new ArrayList<>();
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			GitTagsetHandler gitTagsetHandler = 
					new GitTagsetHandler(localGitRepositoryManager, remoteGitServerManager);
			localGitRepoManager.open(
					 projectId,
					 GitProjectManager.getProjectRootRepositoryName(projectId));
			 
			 Path tagsetsDirPath = 
					 Paths.get(localGitRepoManager.getRepositoryWorkTree().toURI())
					 .resolve(TAGSET_SUBMODULES_DIRECTORY_NAME);
			 localGitRepoManager.detach();
			 
			 if (!tagsetsDirPath.toFile().exists()) {
				 return result;
			 }			
			 
			 List<Path> paths = Files
			 .walk(tagsetsDirPath, 1)
			 .filter(tagsetPath -> !tagsetsDirPath.equals(tagsetPath))
			 .collect(Collectors.toList());
			 
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
				new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			for (TagsetDefinition tagset : getTagsets()) {
				gitTagsetHandler.checkout(
					projectId, tagset.getUuid(), 
					JGitRepoManager.DEFAULT_LOCAL_DEV_BRANCH, true);
			}
			
			GitMarkupCollectionHandler collectionHandler = 
				new GitMarkupCollectionHandler(localGitRepoManager, remoteGitServerManager);
			
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
					new GitMarkupCollectionHandler(localGitRepoManager, remoteGitServerManager);

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

}