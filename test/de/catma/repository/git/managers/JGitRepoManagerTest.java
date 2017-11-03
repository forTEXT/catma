package de.catma.repository.git.managers;

import de.catma.repository.db.DBUser;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import helpers.Randomizer;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.CommitsApi;
import org.gitlab4j.api.models.Commit;
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

public class JGitRepoManagerTest {
	private Properties catmaProperties;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

	public JGitRepoManagerTest() throws Exception {
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
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			// the other tests test that the response is true when it should be
			assertFalse(jGitRepoManager.isAttached());
		}
	}

	@Test
	public void getRepositoryWorkTree() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			assertNull(jGitRepoManager.getRepositoryWorkTree());

			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);
			assertEquals(testRepoPath, jGitRepoManager.getRepositoryWorkTree());
		}
	}

	@Test
	public void getRemoteUrl() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			StoredConfig config = jGitRepoManager.getGitApi().getRepository().getConfig();
			config.setString(
				"remote", "origin", "url", "http://fake.it/till/you/make/it"
			);
			config.setString(
				"remote", "other", "url", "http://fake.it/till/you/make/it/other"
			);

			assertEquals(
				"http://fake.it/till/you/make/it", jGitRepoManager.getRemoteUrl(null)
			);
			assertEquals(
				"http://fake.it/till/you/make/it/other", jGitRepoManager.getRemoteUrl("other")
			);
		}
	}

	@Test
	public void init() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), "Test Description");

			assert jGitRepoManager.isAttached();
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
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			// create a bare repository that will act as the remote
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			try (Git gitApi = Git.init().setDirectory(testRepoPath).setBare(true).call()) {}
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			// clone it
			File clonedRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), "cloned");

			String repoName = jGitRepoManager.clone(
				testRepoPath.toURI().toString(), clonedRepoPath, null, null
			);

			assert jGitRepoManager.isAttached();
			assertEquals(repoName, clonedRepoPath.getName());
			assert clonedRepoPath.exists();
			assert clonedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(clonedRepoPath);
			assert Arrays.asList(clonedRepoPath.list()).contains(".git");
		}
	}

	@Test
	public void cloneGitLabRepoWithAuthentication() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager
		DBUser catmaUser = Randomizer.getDbUser();

		GitLabServerManager gitLabServerManager = new GitLabServerManager(
			this.catmaProperties, catmaUser
		);
		gitLabServerManager.replaceGitLabServerUrl = true;

		String randomRepoName = Randomizer.getRepoName();

		IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
				gitLabServerManager.createRepository(randomRepoName, null);

		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			String repoName = jGitRepoManager.clone(
				createRepositoryResponse.repositoryHttpUrl,
				null,
				gitLabServerManager.getGitLabUser().getUsername(),
				gitLabServerManager.getGitLabUserImpersonationToken()
			);

			assert jGitRepoManager.isAttached();
			assertEquals(repoName, randomRepoName);

			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), repoName);
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);
			assert Arrays.asList(testRepoPath.list()).contains(".git");
		}

		// cleanup (these are not handled by tearDown)
		gitLabServerManager.deleteRepository(createRepositoryResponse.repositoryId);
		await().until(
			() -> gitLabServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
		);

		// see GitLabServerManagerTest tearDown() for more info
		User user = gitLabServerManager.getGitLabUser();
		gitLabServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
		GitLabServerManagerTest.awaitUserDeleted(
			gitLabServerManager.getAdminGitLabApi().getUserApi(), user.getId()
		);
	}

	@Test
	public void open() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			jGitRepoManager.detach();  // can't call open on an attached instance

			jGitRepoManager.open(testRepoPath.getName());

			assert jGitRepoManager.isAttached();
		}
	}

	@Test
	public void add() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath, originalSourceDocument.getName());

			jGitRepoManager.add(targetFile, originalSourceDocumentBytes);

			Git gitApi = jGitRepoManager.getGitApi();
			Status status = gitApi.status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(originalSourceDocument.getName());
		}
	}

	@Test
	public void addAndCommit() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath, originalSourceDocument.getName());

			jGitRepoManager.addAndCommit(
				targetFile, originalSourceDocumentBytes,
				"Test Committer", "testcommitter@catma.de"
			);

			Git gitApi = jGitRepoManager.getGitApi();
			Status status = gitApi.status().call();

			assert status.isClean();
			assertFalse(status.hasUncommittedChanges());

			Iterable<RevCommit> commits = jGitRepoManager.getGitApi().log().all().call();
			@SuppressWarnings("unchecked")
			List<RevCommit> commitsList = IteratorUtils.toList(commits.iterator());

			assertEquals(1, commitsList.size());
			assertEquals("Test Committer" , commitsList.get(0).getCommitterIdent().getName());
			assertEquals("testcommitter@catma.de" , commitsList.get(0).getCommitterIdent().getEmailAddress());
		}
	}

	@Test
	public void commit() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath, originalSourceDocument.getName());

			jGitRepoManager.add(targetFile, originalSourceDocumentBytes);

			Git gitApi = jGitRepoManager.getGitApi();
			Status status = gitApi.status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(originalSourceDocument.getName());

			jGitRepoManager.commit(
				String.format("Adding %s", targetFile.getName()),
				"Test Committer", "testcommitter@catma.de"
			);

			status = gitApi.status().call();
			assert status.isClean();
			assertFalse(status.hasUncommittedChanges());

			Iterable<RevCommit> commits = jGitRepoManager.getGitApi().log().all().call();
			@SuppressWarnings("unchecked")
			List<RevCommit> commitsList = IteratorUtils.toList(commits.iterator());

			assertEquals(1, commitsList.size());
			assertEquals("Test Committer" , commitsList.get(0).getCommitterIdent().getName());
			assertEquals("testcommitter@catma.de" , commitsList.get(0).getCommitterIdent().getEmailAddress());
		}
	}

	@Test
	public void addSubmodule() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			File containerRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), "container");
			File submoduleRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), "submodule");

			// init container repo
			jGitRepoManager.init(containerRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert containerRepoPath.exists();
			assert containerRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(containerRepoPath);

			jGitRepoManager.detach();  // can't call init on an attached instance

			// init another repo which will be a submodule in the container repo
			jGitRepoManager.init(submoduleRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert submoduleRepoPath.exists();
			assert submoduleRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(submoduleRepoPath);

			jGitRepoManager.detach();  // can't call open on an attached instance

			// re-attach the repo manager to the container repo
			jGitRepoManager.open(containerRepoPath.getName());

			// add a submodule to the container repo
			jGitRepoManager.addSubmodule(
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
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			// create a bare repository that will act as the remote
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			try (Git gitApi = Git.init().setDirectory(testRepoPath).setBare(true).call()) {}
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			// clone it
			File clonedRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), "cloned");

			String repoName = jGitRepoManager.clone(
				testRepoPath.toURI().toString(), clonedRepoPath, null, null
			);

			assert jGitRepoManager.isAttached();
			assertEquals(repoName, clonedRepoPath.getName());
			assert clonedRepoPath.exists();
			assert clonedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(clonedRepoPath);

			jGitRepoManager.detach();  // can't call open on an attached instance

			// open the cloned repo, add and commit a file, then push
			jGitRepoManager.open(clonedRepoPath.getName());

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(clonedRepoPath, originalSourceDocument.getName());

			jGitRepoManager.addAndCommit(
				targetFile, originalSourceDocumentBytes,
				"Test Committer", "testcommitter@catma.de"
			);

			jGitRepoManager.push(null, null);

			jGitRepoManager.detach();  // can't call open on an attached instance

			// re-open the bare repo and assert that the push worked by inspecting the Git log
			jGitRepoManager.open(testRepoPath.getName());
			Iterable<RevCommit> commits = jGitRepoManager.getGitApi().log().all().call();
			@SuppressWarnings("unchecked")
			List<RevCommit> commitsList = IteratorUtils.toList(commits.iterator());

			assertEquals(1, commitsList.size());
			assertEquals("Adding rose_for_emily.pdf" , commitsList.get(0).getFullMessage());
		}
	}

	@Test
	public void pushToGitLabRepoWithAuthentication() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager
		DBUser catmaUser = Randomizer.getDbUser();

		GitLabServerManager gitLabServerManager = new GitLabServerManager(
			this.catmaProperties, catmaUser
		);
		gitLabServerManager.replaceGitLabServerUrl = true;

		// create a repository
		IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
				gitLabServerManager.createRepository(Randomizer.getRepoName(), null);

		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			// clone it
			String repoName = jGitRepoManager.clone(
				createRepositoryResponse.repositoryHttpUrl,
				null,
				gitLabServerManager.getGitLabUser().getUsername(),
				gitLabServerManager.getGitLabUserImpersonationToken()
			);

			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), repoName);
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			jGitRepoManager.detach();  // can't call open on an attached instance

			// open the cloned repo, add and commit a file, then push
			jGitRepoManager.open(testRepoPath.getName());

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(testRepoPath, originalSourceDocument.getName());

			jGitRepoManager.addAndCommit(
				targetFile, originalSourceDocumentBytes,
				"Test Committer", "testcommitter@catma.de"
			);

			jGitRepoManager.push(
				gitLabServerManager.getGitLabUser().getUsername(),
				gitLabServerManager.getGitLabUserImpersonationToken()
			);

			// assert that the push worked by looking at the commits on the server
			CommitsApi commitsApi = gitLabServerManager.getUserGitLabApi().getCommitsApi();
			List<Commit> commits = commitsApi.getCommits(createRepositoryResponse.repositoryId);
			assertEquals(1, commits.size());
			assertEquals("Adding rose_for_emily.pdf", commits.get(0).getMessage());

			// cleanup (these are not handled by tearDown)
			gitLabServerManager.deleteRepository(createRepositoryResponse.repositoryId);
			await().until(
				() -> gitLabServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
			);

			// see GitLabServerManagerTest tearDown() for more info
			User user = gitLabServerManager.getGitLabUser();
			gitLabServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
			GitLabServerManagerTest.awaitUserDeleted(
				gitLabServerManager.getAdminGitLabApi().getUserApi(), user.getId()
			);
		}
	}

	@Test
	public void fetch() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			// create a bare repository that will act as the remote
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			try (Git gitApi = Git.init().setDirectory(testRepoPath).setBare(true).call()) {}
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			// clone it
			File clonedRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), "cloned");

			String repoName = jGitRepoManager.clone(
				testRepoPath.toURI().toString(), clonedRepoPath, null, null
			);

			assert jGitRepoManager.isAttached();
			assertEquals(repoName, clonedRepoPath.getName());
			assert clonedRepoPath.exists();
			assert clonedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(clonedRepoPath);

			jGitRepoManager.detach();  // can't call open on an attached instance

			// open the cloned repo, add and commit a file, then push
			jGitRepoManager.open(clonedRepoPath.getName());

			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

			File targetFile = new File(clonedRepoPath, originalSourceDocument.getName());

			jGitRepoManager.addAndCommit(
				targetFile, originalSourceDocumentBytes,
				"Test Committer", "testcommitter@catma.de"
			);

			jGitRepoManager.push(null, null);

			jGitRepoManager.detach();  // can't call init on an attached instance

			File fetchRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), "testfetch");

			// init a new repository, add the remote and do a fetch
			jGitRepoManager.init(fetchRepoPath.getName(), null);
			this.directoriesToDeleteOnTearDown.add(fetchRepoPath);

			RemoteAddCommand remoteAddCommand = jGitRepoManager.getGitApi().remoteAdd();
			remoteAddCommand.setName("origin");
			remoteAddCommand.setUri(new URIish("../" + testRepoPath.getName()));
			remoteAddCommand.call();

			jGitRepoManager.fetch(null, null);

			// assert that the commit was fetched by inspecting the Git log
			Iterable<RevCommit> commits = jGitRepoManager.getGitApi().log().all().call();
			@SuppressWarnings("unchecked")
			List<RevCommit> commitsList = IteratorUtils.toList(commits.iterator());

			assertEquals(1, commitsList.size());
			assertEquals("Adding rose_for_emily.pdf" , commitsList.get(0).getFullMessage());

			// TODO: figure out why we need to call close on the repository explicitly
			// this is supposed to happen automatically as all repos are opened by static factory methods on
			// org.eclipse.jgit.api.Git (see the documentation for the close method on the Git class)
			// JGitRepoManager init, clone and open methods all use the static factory methods
			// lots of discussion on this should you be interested:
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=474093
			// https://www.google.com/search?q=jgit+not+releasing+pack+file+handle
			jGitRepoManager.getGitApi().getRepository().close();
		}
	}

	@Test
	public void checkout() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties)) {
			// init a new repository and make 2 commits
			File testRepoPath = new File(jGitRepoManager.getRepositoryBasePath(), Randomizer.getRepoName());

			jGitRepoManager.init(testRepoPath.getName(), null);

			assert jGitRepoManager.isAttached();
			assert testRepoPath.exists();
			assert testRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(testRepoPath);

			File file1 = new File("testdocs/rose_for_emily.txt");
			byte[] file1Bytes = Files.readAllBytes(file1.toPath());
			File targetFile1 = new File(testRepoPath, file1.getName());

			File file2 = new File("testdocs/kafka_utf8.txt");
			byte[] file2Bytes = Files.readAllBytes(file2.toPath());
			File targetFile2 = new File(testRepoPath, file2.getName());

			jGitRepoManager.addAndCommit(
				targetFile1, file1Bytes, "Test Committer", "testcommitter@catma.de"
			);
			jGitRepoManager.addAndCommit(
				targetFile2, file2Bytes, "Test Committer", "testcommitter@catma.de"
			);

			// get the commit hashes by inspecting the Git log
			Iterable<RevCommit> commits = jGitRepoManager.getGitApi().log().all().call();
			@SuppressWarnings("unchecked")
			List<RevCommit> commitsList = IteratorUtils.toList(commits.iterator());

			assertEquals(2, commitsList.size());

			// commitsList is in descending order by date
			String file1CommitHash = commitsList.get(1).getName();
			String file2CommitHash = commitsList.get(0).getName();

			// assert that HEAD is pointing to the commit of the 2nd file
			assertEquals(
				file2CommitHash, jGitRepoManager.getGitApi().getRepository().resolve(Constants.HEAD).getName()
			);

			// assert that we ARE NOT in a detached head state
			List<Ref> refs = jGitRepoManager.getGitApi().branchList().call();
			assertEquals(1, refs.size());

			// check out the 1st commit
			jGitRepoManager.checkout(file1CommitHash);

			// assert that HEAD is pointing to the commit of the 1st file
			assertEquals(
				file1CommitHash, jGitRepoManager.getGitApi().getRepository().resolve(Constants.HEAD).getName()
			);

			// assert that we ARE in a detached head state
			refs = jGitRepoManager.getGitApi().branchList().call();
			assertEquals(2, refs.size());

			// assert that the file from the 2nd commit does not appear in the worktree
			assert targetFile1.exists();
			assertFalse(targetFile2.exists());
		}
	}
}