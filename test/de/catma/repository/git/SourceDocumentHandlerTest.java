package de.catma.repository.git;

import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.models.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class SourceDocumentHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private SourceDocumentHandler sourceDocumentHandler;

	private File createdRepositoryPath = null;
	private String insertedSourceDocumentId = null;

    public SourceDocumentHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
    }

    @Before
	public void setUp() throws Exception {
		this.remoteGitServerManager = new RemoteGitServerManager(this.catmaProperties);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.sourceDocumentHandler = new SourceDocumentHandler(
			this.localGitRepositoryManager, this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {
    	if (this.createdRepositoryPath != null) {
			FileUtils.deleteDirectory(this.createdRepositoryPath);
			this.createdRepositoryPath = null;
		}

		if (this.insertedSourceDocumentId != null) {
    		List<Project> projects = this.remoteGitServerManager.getGitLabApi().getProjectApi()
					.getProjects(this.insertedSourceDocumentId);
			for (Project project : projects) {
				this.remoteGitServerManager.deleteRepository(project.getId());
			}
			await().until(
				() -> this.remoteGitServerManager.getGitLabApi().getProjectApi().getProjects()
						.isEmpty()
			);
			this.insertedSourceDocumentId = null;
		}

		if (this.localGitRepositoryManager != null) {
			this.localGitRepositoryManager.close();
			this.localGitRepositoryManager = null;
		}
	}

    @Test
    public void insert() throws Exception {
    	File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
        File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

        FileInputStream originalSourceDocumentStream = new FileInputStream(originalSourceDocument);
        FileInputStream convertedSourceDocumentStream = new FileInputStream(
			convertedSourceDocument
		);

        this.insertedSourceDocumentId = this.sourceDocumentHandler.insert(
			originalSourceDocumentStream, originalSourceDocument.getName(),
			convertedSourceDocumentStream, convertedSourceDocument.getName(),
			null, null
		);

		File expectedRepoPath = new File(
			this.localGitRepositoryManager.getRepositoryBasePath(), this.insertedSourceDocumentId
		);

		assert expectedRepoPath.exists();
		assert expectedRepoPath.isDirectory();

		this.createdRepositoryPath = expectedRepoPath;

		assert Arrays.asList(expectedRepoPath.list()).contains("rose_for_emily.pdf");
		assert Arrays.asList(expectedRepoPath.list()).contains("rose_for_emily.txt");
		assert FileUtils.contentEquals(
			originalSourceDocument, new File(expectedRepoPath, "rose_for_emily.pdf")
		);
		assert FileUtils.contentEquals(
			convertedSourceDocument, new File(expectedRepoPath, "rose_for_emily.txt")
		);
	}
}
