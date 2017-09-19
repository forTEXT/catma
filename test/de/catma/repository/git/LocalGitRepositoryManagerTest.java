package de.catma.repository.git;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class LocalGitRepositoryManagerTest {
	private Properties catmaProperties;
	private LocalGitRepositoryManager repoManager;
	private File testRepoPath;

	public LocalGitRepositoryManagerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		this.repoManager = new LocalGitRepositoryManager(this.catmaProperties);
		this.testRepoPath = new File(
			this.repoManager.getRepositoryBasePath() + "/test-repo"
		);
	}

	@After
	public void tearDown() throws Exception {
		if (this.repoManager != null) {
			this.repoManager.close();
		}
		if (this.testRepoPath != null) {
			FileUtils.deleteDirectory(this.testRepoPath);
		}
	}

	@Test
	public void init() throws Exception {
		this.repoManager.init(this.testRepoPath.getName(), "Test Description");

		assert this.repoManager.isAttached();
		assert this.testRepoPath.exists();
		assert this.testRepoPath.isDirectory();
		assert Arrays.asList(this.testRepoPath.list()).contains(".git");

		File gitDescriptionFile = new File(this.testRepoPath, ".git/description");
		assertEquals(
			"Test Description", new String(Files.readAllBytes(gitDescriptionFile.toPath()))
		);
	}

	@Test
	public void open() throws Exception {
		// we use a separate LocalGitRepositoryManager instance to init the repo as we can't call
		// open on an attached instance
		try (LocalGitRepositoryManager repoManager = new LocalGitRepositoryManager(
			this.catmaProperties
		)) {
			repoManager.init(this.testRepoPath.getName(), null);
		}

		this.repoManager.open(this.testRepoPath.getName());

		assert this.repoManager.isAttached();
	}

	@Test
	public void add() throws Exception {
		this.repoManager.init(this.testRepoPath.getName(), null);

		File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
		byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

		File targetFile = new File(
			this.testRepoPath + "/" + originalSourceDocument.getName()
		);

		this.repoManager.add(targetFile, originalSourceDocumentBytes);

		Git gitApi = this.repoManager.getGitApi();
		Status status = gitApi.status().call();
		Set<String> added = status.getAdded();

		assert status.hasUncommittedChanges();
		assert added.contains(originalSourceDocument.getName());
	}

	@Test
	public void addAndCommit() throws Exception {
		this.repoManager.init(this.testRepoPath.getName(), null);

		File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
		byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

		File targetFile = new File(
			this.testRepoPath + "/" + originalSourceDocument.getName()
		);

		this.repoManager.addAndCommit(targetFile, originalSourceDocumentBytes);

		Git gitApi = this.repoManager.getGitApi();
		Status status = gitApi.status().call();

		assert status.isClean();
		assertFalse(status.hasUncommittedChanges());
	}

	@Test
	public void commit() throws Exception {
		this.repoManager.init(this.testRepoPath.getName(), null);

		File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
		byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

		File targetFile = new File(
			this.testRepoPath + "/" + originalSourceDocument.getName()
		);

		this.repoManager.add(targetFile, originalSourceDocumentBytes);

		Git gitApi = this.repoManager.getGitApi();
		Status status = gitApi.status().call();
		Set<String> added = status.getAdded();

		assert status.hasUncommittedChanges();
		assert added.contains(originalSourceDocument.getName());

		this.repoManager.commit(String.format("Adding %s", targetFile.getName()));

		status = gitApi.status().call();
		assert status.isClean();
		assertFalse(status.hasUncommittedChanges());
	}
}
