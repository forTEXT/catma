package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import de.catma.document.source.SourceDocumentInfo;
import de.catma.indexer.TermInfo;
import de.catma.project.ProjectReference;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.tag.TagsetDefinition;

public class GitProjectHandler {

	public static final String TAGSET_SUBMODULES_DIRECTORY_NAME = "tagsets";
	public static final String MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME = "collections";
	public static final String SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME = "documents";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	public GitProjectHandler(ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitServerManager remoteGitServerManager) {
		super();
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	// tagset operations
	public String createTagset(String projectId,
							   String tagsetId,
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

			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					tagsetRepoRemoteUrl,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
			);

			return tagsetRevisionHash;
		}
	}

	// markup collection operations
	public String createMarkupCollection(String projectId,
										 String markupCollectionId,
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

			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					markupCollectionRepoRemoteUrl,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
			);

			return revisionHash;
		}
	}

	// source document operations

	/**
	 * Creates a new source document within the project identified by <code>projectId</code>.
	 *
	 * @param projectId the ID of the project within which the source document must be created
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
			String projectId, String sourceDocumentId,
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

			localRepoManager.addSubmodule(
				targetSubmodulePath, remoteUri,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getPassword()
			);
		
			return revisionHash;
		}

	}

	public String getRootRevisionHash(ProjectReference projectReference) throws Exception {
		try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
			localRepoManager.open(
				projectReference.getProjectId(),
				GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()));
			
			return localRepoManager.getRootRevisionHash();
		}
	}

	public Path getSourceDocumentSubmodulePath(GitUser user, ProjectReference projectReference, String sourceDocumentId) {
		return Paths.get(
			user.getIdentifier(), 
			projectReference.getProjectId(), 
			GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()), 
			SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
			sourceDocumentId);
	}
}