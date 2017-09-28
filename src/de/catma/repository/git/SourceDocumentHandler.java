package de.catma.repository.git;

import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.ISourceDocumentHandler;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.util.IDGenerator;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SourceDocumentHandler implements ISourceDocumentHandler {
    private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

    public SourceDocumentHandler(ILocalGitRepositoryManager localGitRepositoryManager,
								 IRemoteGitServerManager remoteGitServerManager) {
        this.localGitRepositoryManager = localGitRepositoryManager;
        this.remoteGitServerManager = remoteGitServerManager;
    }

	/**
	 * Inserts a new source document.
	 *
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original,
	 *                                     unmodified source document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified source
	 *                                       document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted,
	 *                                      UTF-8 encoded source document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded source
	 *                                        document
	 * @param sourceDocumentId the ID of the source document to insert. If none is provided, a new
	 *                         ID will be generated.
	 * @param projectId the ID of the project that the source document must be inserted into
	 * @return the <code>sourceDocumentId</code> if one was provided, otherwise a new source
	 *         document ID
	 * @throws SourceDocumentHandlerException if an error occurs while inserting the source document
	 */
	@Override
	public String insert(InputStream originalSourceDocumentStream,
						 String originalSourceDocumentFileName,
						 InputStream convertedSourceDocumentStream,
						 String convertedSourceDocumentFileName,
						 @Nullable String sourceDocumentId,
						 @Nullable String projectId) throws SourceDocumentHandlerException {
		if (sourceDocumentId == null) {
			IDGenerator idGenerator = new IDGenerator();
			sourceDocumentId = idGenerator.generate();
		}

		// TODO: create header.json and write the SourceDocumentInfo into it

		try {
			// create the source document repository
			IRemoteGitServerManager.CreateRepositoryResponse response;

			if (projectId == null) {
				response = this.remoteGitServerManager.createRepository(
					sourceDocumentId, sourceDocumentId
				);
			} else {
				response = this.remoteGitServerManager.createRepository(
					sourceDocumentId, sourceDocumentId, projectId
				);
			}

			// clone the repository locally
			this.localGitRepositoryManager.clone(response.repositoryHttpUrl);

			// write the original and converted source document files into the local repo
			File targetOriginalSourceDocumentFile = new File(
				this.localGitRepositoryManager.getRepositoryWorkTree(),
				originalSourceDocumentFileName
			);
			File targetConvertedSourceDocumentFile = new File(
				this.localGitRepositoryManager.getRepositoryWorkTree(),
				convertedSourceDocumentFileName
			);

			byte[] bytes = IOUtils.toByteArray(originalSourceDocumentStream);
			this.localGitRepositoryManager.add(targetOriginalSourceDocumentFile, bytes);
			bytes = IOUtils.toByteArray(convertedSourceDocumentStream);
			this.localGitRepositoryManager.add(targetConvertedSourceDocumentFile, bytes);

			// commit newly added files
			String commitMessage = String.format("Adding %s and %s", originalSourceDocumentFileName,
					convertedSourceDocumentFileName);
			this.localGitRepositoryManager.commit(commitMessage);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException|IOException e) {
			throw new SourceDocumentHandlerException("Failed to insert source document", e);
		}

		return sourceDocumentId;
	}

	@Override
	public void remove(String sourceDocumentId) throws SourceDocumentHandlerException {
    	throw new SourceDocumentHandlerException("Not implemented");
	}
}
