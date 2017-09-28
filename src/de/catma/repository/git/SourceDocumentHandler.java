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

		// TODO: write the SourceDocumentInfo to header.json

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
