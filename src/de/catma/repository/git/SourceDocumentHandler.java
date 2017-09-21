package de.catma.repository.git;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.TechInfoSet;
import de.catma.repository.ISourceDocumentHandler;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.managers.LocalGitRepositoryManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Properties;

public class SourceDocumentHandler implements ISourceDocumentHandler {
    private Properties catmaProperties;
	private String repositoryBasePath;
    private LocalGitRepositoryManager localGitRepositoryManager;

    public SourceDocumentHandler(Properties catmaProperties) {
        this.catmaProperties = catmaProperties;
		this.repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");
        this.localGitRepositoryManager = new LocalGitRepositoryManager(catmaProperties);
    }

    @Override
    public void insert(byte[] originalSourceDocumentBytes, SourceDocument sourceDocument,
					   @Nullable Integer groupId)
            throws SourceDocumentHandlerException {

    	// TODO: do something with groupId

		TechInfoSet techInfoSet = sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				.getTechInfoSet();

		File targetFile = new File(this.repositoryBasePath + "/" +
				techInfoSet.getFileName());

        try {
			this.localGitRepositoryManager.init(sourceDocument.getID(), null);
			this.localGitRepositoryManager.addAndCommit(targetFile, originalSourceDocumentBytes);
		}
		catch (LocalGitRepositoryManagerException e) {
        	throw new SourceDocumentHandlerException("Failed to insert source document", e);
		}
    }
}
