package de.catma.repository.git;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.TechInfoSet;
import de.catma.repository.ISourceDocumentHandler;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SourceDocumentHandler implements ISourceDocumentHandler {

    private Properties catmaProperties;

    public SourceDocumentHandler(Properties catmaProperties) {
        this.catmaProperties = catmaProperties;
    }

    @Override
    public void insert(SourceDocument sourceDocument) throws IOException, GitAPIException {
        int repoIndex = 1; // assume that the first configured repo is the local repo

        // TODO: remove hardcoding of repo name - this will probably just be a UUID or similar for tagset, markup collection and source document repos
		// but we need to think about how to handle this for the CATMA project repo, whose name/path need to be unique (if we don't want to track the association externally)
		// for the GitLab groups it looks like you can set the group path independently from the name, so we could maybe just make the group path something unique and allow names
		// to be reused? see https://docs.gitlab.com/ce/user/group/index.html#create-a-new-group
        String repoPath = RepositoryPropertyKey.RepositoryFolderPath.getProperty(this.catmaProperties, repoIndex) + "/" + "test-project" + "/";

        TechInfoSet techInfoSet = sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet();
        File targetFile = new File(repoPath + techInfoSet.getFileName());

        // TODO: factor out repo creation and file addition
        try (Git git = Git.init().setDirectory(new File(repoPath)).call(); FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
            fileOutputStream.write(sourceDocument.getContent().getBytes(techInfoSet.getCharset()));
            git.add().addFilepattern(targetFile.getAbsolutePath());
            git.commit().setMessage(String.format("Adding %s", targetFile.getName())).call();
        }
    }
}
