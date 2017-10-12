package de.catma.repository.git.managers;

import de.catma.repository.db.DBUser;
import de.catma.repository.git.GitLabAuthenticationHelper;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import static org.awaitility.Awaitility.await;
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
		this.testRepoPath = new File(this.repoManager.getRepositoryBasePath(), "test-repo");
	}

	@After
	public void tearDown() throws Exception {
		if (this.repoManager != null) {
			this.repoManager.close();
			this.repoManager = null;
		}
		if (this.testRepoPath != null) {
			FileUtils.deleteDirectory(this.testRepoPath);
			this.testRepoPath = null;
		}
	}

	@Test
	public void isAttached() throws Exception {
		// init(), open() and clone() test that the response is true when it should be
		assertFalse(this.repoManager.isAttached());
	}

	@Test
	public void getRepositoryWorkTree() throws Exception {
		assertNull(this.repoManager.getRepositoryWorkTree());

		this.repoManager.init(this.testRepoPath.getName(), null);

		assertEquals(this.testRepoPath, this.repoManager.getRepositoryWorkTree());
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
			"Test Description\n",
			new String(Files.readAllBytes(gitDescriptionFile.toPath()))
		);
	}

	@Test
	public void cloneRepo() throws Exception {
		String repoName = this.repoManager.clone("https://github.com/maltem-za/tiny.git",
			null, null
		);

		this.testRepoPath = new File(this.repoManager.getRepositoryBasePath(), "tiny");

		assert this.repoManager.isAttached();
		assertEquals(repoName, "tiny");
		assert this.testRepoPath.exists();
		assert this.testRepoPath.isDirectory();
		assert Arrays.asList(this.testRepoPath.list()).contains(".git");
		assert Arrays.asList(this.testRepoPath.list()).contains("README.md");
	}

	@Test
	public void cloneGitLabRepoWithAuthentication() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		DBUser catmaUser = new DBUser(
			1, "catma-testuser", false, false, false
		);

		RemoteGitServerManager remoteGitServerManager = new RemoteGitServerManager(
			this.catmaProperties, catmaUser
		);
		remoteGitServerManager.replaceGitLabServerUrl = true;

		IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
				remoteGitServerManager.createRepository("test-repo", null);

		String authenticatedRepositoryUrl = GitLabAuthenticationHelper
				.buildAuthenticatedRepositoryUrl(
					createRepositoryResponse.repositoryHttpUrl,
					remoteGitServerManager.getGitLabUserImpersonationToken()
				);

		String repoName = this.repoManager.clone(
			authenticatedRepositoryUrl,
			remoteGitServerManager.getGitLabUser().getUsername(),
			remoteGitServerManager.getGitLabUserImpersonationToken()
		);

		assert this.repoManager.isAttached();
		assertEquals(repoName, "test-repo");
		assert this.testRepoPath.exists();
		assert this.testRepoPath.isDirectory();
		assert Arrays.asList(this.testRepoPath.list()).contains(".git");

		// cleanup
		remoteGitServerManager.deleteRepository(createRepositoryResponse.repositoryId);
		await().until(
			() -> remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
		);

		// see RemoteGitServerManagerTest tearDown() for more info
		User user = remoteGitServerManager.getGitLabUser();
		remoteGitServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
		RemoteGitServerManagerTest.awaitUserDeleted(
			remoteGitServerManager.getAdminGitLabApi().getUserApi(), user.getId()
		);

		// tearDown() will take care of deleting the this.testRepoPath directory
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

	@Test
	public void addSubModule() throws Exception {
		// init container repo
		try (LocalGitRepositoryManager repoMan = this.repoManager) {
			repoMan.init(this.testRepoPath.getName(), null);
		}

		// init another repo which will be a submodule in the container repo
		File testSubmoduleRepoPath = new File(this.repoManager.getRepositoryBasePath(), "test-repo-2");

		try (LocalGitRepositoryManager repoMan = this.repoManager) {
			this.repoManager.init(testSubmoduleRepoPath.getName(), null);
		}

		// re-attach the repo manager to the container repo
		this.repoManager.open(this.testRepoPath.getName());

		// add a submodule to the container repo
		this.repoManager.addSubModule(
			new File(this.testRepoPath, testSubmoduleRepoPath.getName()), "../test-repo-2"
		);

		assert Arrays.asList(this.testRepoPath.list()).contains(".gitmodules");
		assert Arrays.asList(this.testRepoPath.list()).contains("test-repo-2");

		File gitModulesFilePath = new File(this.testRepoPath, ".gitmodules");
		String expectedGitModulesFileContents = "" +
				"[submodule \"test-repo-2\"]\n" +
				"\tpath = test-repo-2\n" +
				"\turl = ../test-repo-2\n";
		assertEquals(
			expectedGitModulesFileContents, FileUtils.readFileToString(gitModulesFilePath, StandardCharsets.UTF_8)
		);

		File subModulePath = new File(this.testRepoPath, "test-repo-2");
		assert Arrays.asList(subModulePath.list()).contains(".git");

		File subModuleGitFilePath = new File(subModulePath, ".git");
		// example: "gitdir: C:\Code\catma\dev\repo\git\test-repo\.git\modules\test-repo-2"
		String expectedSubModuleGitFileContents = String.format(
			"gitdir: %s", new File(this.testRepoPath, ".git/modules/test-repo-2").toString()
		);
		assertEquals(
			expectedSubModuleGitFileContents, FileUtils.readFileToString(subModuleGitFilePath, StandardCharsets.UTF_8)
		);

		FileUtils.deleteDirectory(testSubmoduleRepoPath);
	}
}
