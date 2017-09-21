package de.catma.repository.git;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.TechInfoSet;
import de.catma.repository.ISourceDocumentHandler;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;

import javax.annotation.Nullable;
import java.io.File;

public class SourceDocumentHandler implements ISourceDocumentHandler {
    private ILocalGitRepositoryManager localGitRepositoryManager;

    public SourceDocumentHandler(ILocalGitRepositoryManager localGitRepositoryManager) {
        this.localGitRepositoryManager = localGitRepositoryManager;
    }

    @Override
    public void insert(byte[] originalSourceDocumentBytes, SourceDocument sourceDocument,
					   @Nullable Integer groupId)
            throws SourceDocumentHandlerException {

    	// TODO: do something with groupId
		// TODO: write the SourceDocumentInfo to header.json

		TechInfoSet techInfoSet = sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				.getTechInfoSet();

		File targetFile = new File(
			this.localGitRepositoryManager.getRepositoryWorkTree(), techInfoSet.getFileName()
		);

        try {
			this.localGitRepositoryManager.init(sourceDocument.getID(), null);
			this.localGitRepositoryManager.addAndCommit(targetFile, originalSourceDocumentBytes);
		}
		catch (LocalGitRepositoryManagerException e) {
        	throw new SourceDocumentHandlerException("Failed to insert source document", e);
		}
    }
}
