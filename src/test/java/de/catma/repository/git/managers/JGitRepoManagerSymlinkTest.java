package de.catma.repository.git.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.catma.user.User;

// Where the test process can't create symlinks (Windows without Developer Mode or admin rights), JGit already sets core.symlinks=false when it creates
// a repository, so the clone tests pass there even if JGitRepoManager stopped disabling symlinks; only openDisablesSymlinksInExistingClones, which
// enables them explicitly, still catches a regression on such a machine.
class JGitRepoManagerSymlinkTest {
	@TempDir
	Path tempDir;

	private Path basePath;
	private User user;
	private JGitCredentialsManager credentialsManager;

	@BeforeEach
	void setUp() {
		basePath = tempDir.resolve("base");
		user = mock(User.class);
		when(user.getIdentifier()).thenReturn("user");
		credentialsManager = mock(JGitCredentialsManager.class);
	}

	@Test
	void cloneChecksOutCommittedSymlinksAsRegularFiles() throws Exception {
		Path source = tempDir.resolve("source");
		try (Git git = Git.init().setDirectory(source.toFile()).call()) {
			// staged directly rather than created on disk, as creating a symlink needs a privilege on Windows
			Repository repository = git.getRepository();
			ObjectId target;
			try (ObjectInserter inserter = repository.newObjectInserter()) {
				target = inserter.insert(Constants.OBJ_BLOB, "/etc/hostname".getBytes(StandardCharsets.UTF_8));
				inserter.flush();
			}
			DirCache index = repository.lockDirCache();
			DirCacheBuilder builder = index.builder();
			DirCacheEntry entry = new DirCacheEntry("link.txt");
			entry.setFileMode(FileMode.SYMLINK);
			entry.setObjectId(target);
			builder.add(entry);
			builder.commit();
			git.commit().setMessage("add symlink").setAuthor("test", "test@example.com").setCommitter("test", "test@example.com").call();
		}

		try (JGitRepoManager repoManager = new JGitRepoManager(basePath.toString(), user)) {
			repoManager.clone("namespace", "project", source.toUri().toString(), credentialsManager);

			StoredConfig config = repoManager.getGitApi().getRepository().getConfig();
			assertTrue(config.getNames(ConfigConstants.CONFIG_CORE_SECTION, null, false).contains(ConfigConstants.CONFIG_KEY_SYMLINKS));
			assertFalse(config.getBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_SYMLINKS, true));
		}

		Path checkedOut = basePath.resolve("user").resolve("namespace").resolve("project").resolve("link.txt");
		assertTrue(Files.isRegularFile(checkedOut, java.nio.file.LinkOption.NOFOLLOW_LINKS));
		assertEquals("/etc/hostname", Files.readString(checkedOut));
	}

	@Test
	void cloneOfEmptyRepositorySucceeds() throws Exception {
		Path source = tempDir.resolve("empty");
		Git.init().setDirectory(source.toFile()).call().close();

		try (JGitRepoManager repoManager = new JGitRepoManager(basePath.toString(), user)) {
			repoManager.clone("namespace", "empty", source.toUri().toString(), credentialsManager);

			StoredConfig config = repoManager.getGitApi().getRepository().getConfig();
			assertFalse(config.getBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_SYMLINKS, true));
		}
	}

	@Test
	void openDisablesSymlinksInExistingClones() throws Exception {
		Path clone = basePath.resolve("user").resolve("namespace").resolve("existing");
		try (Git git = Git.init().setDirectory(clone.toFile()).call()) {
			StoredConfig config = git.getRepository().getConfig();
			config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_SYMLINKS, true);
			config.save();
		}

		try (JGitRepoManager repoManager = new JGitRepoManager(basePath.toString(), user)) {
			repoManager.open("namespace", "existing");

			StoredConfig config = repoManager.getGitApi().getRepository().getConfig();
			assertFalse(config.getBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_SYMLINKS, true));
		}
	}
}
