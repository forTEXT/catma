package de.catma.repository.git;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.IGitSourceDocumentHandler;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.GitSourceDocumentHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GitSourceDocumentHandler implements IGitSourceDocumentHandler {
    private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private static final String SOURCEDOCUMENT_REPOSITORY_NAME_FORMAT = "%s_sourcedocument";

	public static String getSourceDocumentRepositoryName(String sourceDocumentId) {
		return String.format(SOURCEDOCUMENT_REPOSITORY_NAME_FORMAT, sourceDocumentId);
	}

	public GitSourceDocumentHandler(ILocalGitRepositoryManager localGitRepositoryManager,
									IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	/**
	 * Creates a new source document within the project identified by <code>projectId</code>.
	 * <p>
	 * NB: You probably don't want to call this method directly (it doesn't create the submodule in the project root
	 * repo). Instead call the <code>createSourceDocument</code> method of the {@link GitProjectHandler} class.
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
	 * @return the <code>sourceDocumentId</code> if one was provided, otherwise a new source
	 *         document ID
	 * @throws GitSourceDocumentHandlerException if an error occurs while creating the source document
	 */
	@Override
	public String create(@Nonnull String projectId, @Nullable String sourceDocumentId,
						 @Nonnull InputStream originalSourceDocumentStream,
						 @Nonnull String originalSourceDocumentFileName,
						 @Nonnull InputStream convertedSourceDocumentStream,
						 @Nonnull String convertedSourceDocumentFileName,
						 @Nonnull SourceDocumentInfo sourceDocumentInfo
	) throws GitSourceDocumentHandlerException {
		if (sourceDocumentId == null) {
			IDGenerator idGenerator = new IDGenerator();
			sourceDocumentId = idGenerator.generate();
		}

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the source document repository
			String sourceDocumentRepoName = GitSourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocumentId);

			IRemoteGitServerManager.CreateRepositoryResponse response = 
				this.remoteGitServerManager.createRepository(
					sourceDocumentRepoName, sourceDocumentRepoName, projectId
				);

			// clone the repository locally

			localGitRepoManager.clone(
				projectId,
				response.repositoryHttpUrl,
				null,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getPassword()
			);

			// write the original and converted source document files into the local repo
			File targetOriginalSourceDocumentFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				originalSourceDocumentFileName
			);
			File targetConvertedSourceDocumentFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				convertedSourceDocumentFileName
			);

			byte[] bytes = IOUtils.toByteArray(originalSourceDocumentStream);
			localGitRepoManager.add(targetOriginalSourceDocumentFile, bytes);
			bytes = IOUtils.toByteArray(convertedSourceDocumentStream);
			localGitRepoManager.add(targetConvertedSourceDocumentFile, bytes);

			// write header.json into the local repo
			File targetHeaderFile = new File(
				localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);
			GitSourceDocumentInfo gitSourceDocumentInfo = new GitSourceDocumentInfo(sourceDocumentInfo);
			String serializedGitSourceDocumentInfo = new SerializationHelper<GitSourceDocumentInfo>()
					.serialize(gitSourceDocumentInfo);
			localGitRepoManager.add(
				targetHeaderFile, serializedGitSourceDocumentInfo.getBytes(StandardCharsets.UTF_8)
			);

			// commit newly added files
			String commitMessage = String.format("Adding %s, %s and %s", originalSourceDocumentFileName,
					convertedSourceDocumentFileName, targetHeaderFile.getName());
			localGitRepoManager.commit(
				commitMessage,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException|IOException e) {
			throw new GitSourceDocumentHandlerException("Failed to create source document", e);
		}

		return sourceDocumentId;
	}

	@Override
	public void delete(@Nonnull String projectId, @Nonnull String sourceDocumentId)
			throws GitSourceDocumentHandlerException {
    	throw new GitSourceDocumentHandlerException("Not implemented");
	}

	@Override
	public SourceDocument open(@Nonnull String projectId, @Nonnull String sourceDocumentId)
			throws GitSourceDocumentHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			String projectRootRepositoryName = GitProjectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String sourceDocumentSubmoduleName = String.format(
					"%s/%s", GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			File sourceDocumentSubmodulePath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					sourceDocumentSubmoduleName
			);

			File headerFile = new File(
					sourceDocumentSubmodulePath, "header.json"
			);

			String serializedHeaderFile = FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8);
			GitSourceDocumentInfo gitSourceDocumentInfo = new SerializationHelper<GitSourceDocumentInfo>()
					.deserialize(
							serializedHeaderFile,
							GitSourceDocumentInfo.class
					);

			// need to use the catma-core SourceDocumentHandler to decide on the correct SourceContentHandler based on
			// filetype
			de.catma.document.source.SourceDocumentHandler handler =
					new de.catma.document.source.SourceDocumentHandler();

			SourceDocumentInfo sourceDocumentInfo = gitSourceDocumentInfo.getSourceDocumentInfo();
			SourceDocument sourceDocument = handler.loadSourceDocument(sourceDocumentId, sourceDocumentInfo);

			String sourceDocumentRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					sourceDocumentSubmoduleName
			);
			sourceDocument.setRevisionHash(sourceDocumentRevisionHash);

			return sourceDocument;
		}
		catch (LocalGitRepositoryManagerException|IOException|IllegalAccessException|InstantiationException e) {
			throw new GitSourceDocumentHandlerException("Failed to open source document", e);
		}
	}
}
