package de.catma.repository.git;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.ISourceDocumentHandler;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
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

public class SourceDocumentHandler implements ISourceDocumentHandler {
    private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private static final String SOURCEDOCUMENT_REPOSITORY_NAME_FORMAT = "%s_sourcedocument";

	public static String getSourceDocumentRepositoryName(String sourceDocumentId) {
		return String.format(SOURCEDOCUMENT_REPOSITORY_NAME_FORMAT, sourceDocumentId);
	}

	public SourceDocumentHandler(ILocalGitRepositoryManager localGitRepositoryManager,
								 IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

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
	 * @param gitSourceDocumentInfo a {@link GitSourceDocumentInfo} wrapper object
	 * @return the <code>sourceDocumentId</code> if one was provided, otherwise a new source
	 *         document ID
	 * @throws SourceDocumentHandlerException if an error occurs while creating the source document
	 */
	@Override
	public String create(@Nonnull String projectId, @Nullable String sourceDocumentId,
						 @Nonnull InputStream originalSourceDocumentStream,
						 @Nonnull String originalSourceDocumentFileName,
						 @Nonnull InputStream convertedSourceDocumentStream,
						 @Nonnull String convertedSourceDocumentFileName,
						 @Nonnull GitSourceDocumentInfo gitSourceDocumentInfo
	) throws SourceDocumentHandlerException {
		if (sourceDocumentId == null) {
			IDGenerator idGenerator = new IDGenerator();
			sourceDocumentId = idGenerator.generate();
		}

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the source document repository
			IRemoteGitServerManager.CreateRepositoryResponse response;

			String sourceDocumentRepoName = SourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocumentId);

			response = this.remoteGitServerManager.createRepository(
					sourceDocumentRepoName, sourceDocumentRepoName, projectId
			);

			// clone the repository locally
			GitLabServerManager gitLabServerManager =
					(GitLabServerManager)this.remoteGitServerManager;
			User gitLabUser = gitLabServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = gitLabServerManager
					.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
				response.repositoryHttpUrl,
				null,
				gitLabUser.getUsername(),
				gitLabUserImpersonationToken
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
				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
				gitLabUser.getEmail()
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException|IOException e) {
			throw new SourceDocumentHandlerException("Failed to create source document", e);
		}

		return sourceDocumentId;
	}

	@Override
	public void remove(String sourceDocumentId) throws SourceDocumentHandlerException {
    	throw new SourceDocumentHandlerException("Not implemented");
	}

	@Override
	public SourceDocument open(String sourceDocumentId, String projectId) throws SourceDocumentHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			localGitRepoManager.open(SourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocumentId));

			File repositoryWorkTreeFile = localGitRepoManager.getRepositoryWorkTree();
			File targetHeaderFile = new File(
					repositoryWorkTreeFile, "header.json"
			);

			String serialized = FileUtils.readFileToString(targetHeaderFile, StandardCharsets.UTF_8);
			GitSourceDocumentInfo gitSourceDocumentInfo = new SerializationHelper<GitSourceDocumentInfo>()
					.deserialize(
							serialized,
							GitSourceDocumentInfo.class
					);

			// Need to use SourceDocumentHandler to decide on the correct SourceContentHandler based on filetype
			de.catma.document.source.SourceDocumentHandler handler = new de.catma.document.source.SourceDocumentHandler();

			SourceDocumentInfo sourceDocumentInfo = gitSourceDocumentInfo.getSourceDocumentInfo();
			SourceDocument sourceDocument = handler.loadSourceDocument(sourceDocumentId, sourceDocumentInfo);

			return sourceDocument;
		}
		catch (LocalGitRepositoryManagerException | IOException | IllegalAccessException | InstantiationException e) {
			throw new SourceDocumentHandlerException("Failed to open the SourceDocument repo", e);
		}
	}
}
