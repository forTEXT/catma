package de.catma.repository.git.managers;

import de.catma.repository.db.DBUser;
import de.catma.repository.git.GitLabAuthenticationHelper;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class LocalGitRepositoryManagerTest {
	private Properties catmaProperties;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

	public LocalGitRepositoryManagerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
		if (this.directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : this.directoriesToDeleteOnTearDown) {
				FileUtils.deleteDirectory(dir);
			}
			this.directoriesToDeleteOnTearDown.clear();
		}
	}

	@Test
	public void isAttached() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			// the other tests test that the response is true when it should be
			assertFalse(localGitRepoManager.isAttached());
		}
	}

	@Test
	public void getRepositoryWorkTree() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			assertNull(localGitRepoManager.getRepositoryWorkTree());

			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			localGitRepoManager.init(testRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);
			assertEquals(testRepoPath, localGitRepoManager.getRepositoryWorkTree());
		}
	}

	@Test
	public void init() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			localGitRepoManager.init(testRepoPath.getName(), "Test Description");

			assert localGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);
			assert Arrays.asList(testRepoPath.list()).contains(".git");

			File gitDescriptionFile = new File(testRepoPath, ".git/description");
			assertEquals(
				"Test Description\n",
				new String(Files.readAllBytes(gitDescriptionFile.toPath()))
			);
		}
	}

	@Test
	public void cloneRepo() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			String repoName = localGitRepoManager.clone(
				"https://github.com/maltem-za/tiny.git", null, null
			);

			assert localGitRepoManager.isAttached();
			assertEquals(repoName, "tiny");

			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "tiny");
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);
			assert Arrays.asList(testRepoPath.list()).contains(".git");
			assert Arrays.asList(testRepoPath.list()).contains("README.md");
		}
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

		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			String repoName = localGitRepoManager.clone(
				authenticatedRepositoryUrl,
				remoteGitServerManager.getGitLabUser().getUsername(),
				remoteGitServerManager.getGitLabUserImpersonationToken()
			);

			assert localGitRepoManager.isAttached();
			assertEquals(repoName, "test-repo");

			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), repoName);
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);
			assert Arrays.asList(testRepoPath.list()).contains(".git");
		}

		// cleanup (these are not handled by tearDown)
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
	}

	@Test
	public void open() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			localGitRepoManager.init(testRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			localGitRepoManager.detach();  // can't call open on an attached instance

			localGitRepoManager.open(testRepoPath.getName());

			assert localGitRepoManager.isAttached();
		}
	}

	@Test
	public void add() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			localGitRepoManager.init(testRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath + "/" + originalSourceDocument.getName());

			localGitRepoManager.add(targetFile, originalSourceDocumentBytes);

			Git gitApi = localGitRepoManager.getGitApi();
			Status status = gitApi.status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(originalSourceDocument.getName());
		}
	}

	@Test
	public void addAndCommit() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			localGitRepoManager.init(testRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath + "/" + originalSourceDocument.getName());

			localGitRepoManager.addAndCommit(targetFile, originalSourceDocumentBytes);

			Git gitApi = localGitRepoManager.getGitApi();
			Status status = gitApi.status().call();

			assert status.isClean();
			assertFalse(status.hasUncommittedChanges());
		}
	}

	@Test
	public void commit() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			localGitRepoManager.init(testRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath + "/" + originalSourceDocument.getName());

			localGitRepoManager.add(targetFile, originalSourceDocumentBytes);

			Git gitApi = localGitRepoManager.getGitApi();
			Status status = gitApi.status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(originalSourceDocument.getName());

			localGitRepoManager.commit(String.format("Adding %s", targetFile.getName()));

			status = gitApi.status().call();
			assert status.isClean();
			assertFalse(status.hasUncommittedChanges());
		}
	}

	@Test
	public void addSubmodule() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			File containerRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "container");
			File submoduleRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "submodule");

			// init container repo
			localGitRepoManager.init(containerRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert containerRepoPath.exists();
			assert containerRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(containerRepoPath);

			localGitRepoManager.detach();  // can't call init on an attached instance

			// init another repo which will be a submodule in the container repo
			localGitRepoManager.init(submoduleRepoPath.getName(), null);

			assert localGitRepoManager.isAttached();
			assert submoduleRepoPath.exists();
			assert submoduleRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(submoduleRepoPath);

			localGitRepoManager.detach();  // can't call open on an attached instance

			// re-attach the repo manager to the container repo
			localGitRepoManager.open(containerRepoPath.getName());

			// add a submodule to the container repo
			localGitRepoManager.addSubmodule(
				new File(containerRepoPath, submoduleRepoPath.getName()),
				String.format("../%s", submoduleRepoPath.getName()),
				null, null
			);

			assert Arrays.asList(containerRepoPath.list()).contains(".gitmodules");
			assert Arrays.asList(containerRepoPath.list()).contains(submoduleRepoPath.getName());

			File gitModulesFilePath = new File(containerRepoPath, ".gitmodules");
			String expectedGitModulesFileContents = String.format("" +
					"[submodule \"%s\"]\n" +
					"\tpath = %s\n" +
					"\turl = ../%s\n",
					submoduleRepoPath.getName(), submoduleRepoPath.getName(), submoduleRepoPath.getName());
			assertEquals(
				expectedGitModulesFileContents, FileUtils.readFileToString(gitModulesFilePath, StandardCharsets.UTF_8)
			);

			File subModulePath = new File(containerRepoPath, submoduleRepoPath.getName());
			assert Arrays.asList(subModulePath.list()).contains(".git");

			File subModuleGitFilePath = new File(subModulePath, ".git");
			String expectedSubmoduleGitFileContents = String.format(
				"gitdir: %s",
				new File(containerRepoPath, String.format(".git/modules/%s", submoduleRepoPath.getName())).toString()
			);
			assertEquals(
				expectedSubmoduleGitFileContents,
				FileUtils.readFileToString(subModuleGitFilePath, StandardCharsets.UTF_8)
			);
		}
	}

	@Test
	public void push() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			// create a bare repository that will act as the remote
			File testRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "test-repo");

			try (Git gitApi = Git.init().setDirectory(testRepoPath).setBare(true).call()) {}
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			// clone it
			File clonedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), "cloned");

			CloneCommand cloneCommand = Git.cloneRepository().setURI(testRepoPath.toURI().toString()).setDirectory(
				clonedRepoPath
			);
			try (Git gitApi = cloneCommand.call()) {}
			this.directoriesToDeleteOnTearDown.add(clonedRepoPath);

			localGitRepoManager.detach();  // can't call open on an attached instance

			// open the cloned repo, add and commit a file, then push
			localGitRepoManager.open("cloned");

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(clonedRepoPath, originalSourceDocument.getName());

			localGitRepoManager.addAndCommit(targetFile, originalSourceDocumentBytes);

			localGitRepoManager.push(null, null);

			localGitRepoManager.detach();  // can't call open on an attached instance

			// re-open the bare repo and assert that the push worked by inspecting the Git log
			localGitRepoManager.open(testRepoPath.getName());
			Iterable<RevCommit> commits = localGitRepoManager.getGitApi().log().all().call();
			@SuppressWarnings("unchecked")
			List<RevCommit> commitsList = IteratorUtils.toList(commits.iterator());

			assertEquals(1, commitsList.size());
			assertEquals("Adding rose_for_emily.pdf" , commitsList.get(0).getFullMessage());
		}
	}
}
