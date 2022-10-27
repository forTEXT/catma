package de.catma.repository.git.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleStatusCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.RecursiveMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;

import de.catma.project.CommitInfo;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.jgit.JGitCommandFactory;
import de.catma.repository.git.managers.jgit.RelativeJGitCommandFactory;
import de.catma.user.User;
import de.catma.util.Pair;

public class JGitRepoManager implements LocalGitRepositoryManager, AutoCloseable {
	
	public static class ClosableRecursiveMerger extends RecursiveMerger implements AutoCloseable {
	
		public ClosableRecursiveMerger(Repository local, boolean inCore) {
			super(local, inCore);
		}

		public void close() {
			getRepository().getObjectDatabase().close();
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {
					Logger.getLogger(getClass().getName()).log(
							Level.INFO, "closing sleep got interrupted", e);
				}
			}
		}
	}
	
	private final String repositoryBasePath;
	private String username;

	private Git gitApi;
	private JGitCommandFactory jGitCommandFactory;
	private Logger logger = Logger.getLogger(JGitRepoManager.class.getName());
	

	/**
	 * Creates a new instance of this class for the given {@link User}.
	 * <p>
	 * Note that the <code>catmaUser</code> argument is NOT used for authentication to remote Git servers. It is only
	 * used to organise repositories on the local file system, based on the User's identifier. Methods of this class
	 * that support authentication have their own username and password parameters for this purpose.
	 *
	 * @param repositoryBasePath the repo base path
	 * @param catmaUser a {@link User} object
	 */
	public JGitRepoManager(String repositoryBasePath, User catmaUser) {
		this.repositoryBasePath = repositoryBasePath;
		this.username = catmaUser.getIdentifier();
		this.jGitCommandFactory = new RelativeJGitCommandFactory();
	}
	
	public String getUsername() {
		return username;
	}

	public Git getGitApi() {
		return this.gitApi;
	}

	/**
	 * Whether this instance is attached to a Git repository.
	 *
	 * @return true if attached, otherwise false
	 */
	@Override
	public boolean isAttached() {
		return this.gitApi != null;
	}

	/**
	 * Detach this instance from the currently attached Git repository, if any. If this instance is currently attached,
	 * this will allow you to re-use it to make calls to methods that require it to be detached, for example
	 * <code>init</code>, <code>clone</code> or <code>open</code>.
	 * <p>
	 * Whenever possible, you should use a try-with-resources statement instead, which will do this for you
	 * automatically.
	 * <p>
	 * Calling this method or using a try-with-resources statement will cause {@link #close()} to be called.
	 *
	 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">The try-with-resources Statement</a>
	 */
	@Override
	public void detach() {
		this.close();
	}

	/**
	 * Gets the repository base path for this instance, which is specific to the {@link User} supplied at instantiation.
	 *
	 * @return a {@link File} object
	 */
	@Override
	public File getRepositoryBasePath() {
		return Paths.get(new File(repositoryBasePath).toURI()).resolve(this.username).toFile();
	}

	/**
	 * Gets the current Git working tree for the repository this instance is attached to, if any.
	 *
	 * @return a {@link File} object
	 */
	@Override
	public File getRepositoryWorkTree() {
		if (!this.isAttached()) {
			return null;
		}

		return this.gitApi.getRepository().getWorkTree();
	}

	/**
	 * Gets the URL for the remote with the name <code>remoteName</code>.
	 *
	 * @param remoteName the name of the remote for which the URL should be fetched. Defaults to 'origin' if not
	 *                   supplied.
	 * @return the remote URL
	 */
	@Override
	public String getRemoteUrl(String remoteName) {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getRemoteUrl` on a detached instance");
		}

		if (remoteName == null) {
			remoteName = "origin";
		}

		StoredConfig config = this.gitApi.getRepository().getConfig();
		return config.getString("remote", remoteName, "url");
	}

	/**
	 * Initialises a new Git repository with the directory name <code>name</code> and stores the
	 * <code>description</code> in '.git/description'.
	 *
	 * @param name the directory name of the Git repository to initialise
	 * @param description the description of the Git repository to initialise
	 * @throws IOException if the Git repository already exists or couldn't
	 *         be initialised for some other reason
	 */
	@Override
	public void init(String group, String name, String description)
			throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `init` on an attached instance");
		}

		File repositoryPath = 
			Paths.get(this.getRepositoryBasePath().toURI()).resolve(group).resolve(name).toFile();

		// if the directory exists we assume it's a Git repo, could also check for a child .git
		// directory
		if (repositoryPath.exists() && repositoryPath.isDirectory()) {
			throw new IOException(
				String.format(
					"A Git repository with the name '%s' already exists at base path '%s'. " +
					"Did you mean to call `open`?",
						name,
					this.getRepositoryBasePath()
				)
			);
		}

		try {
			this.gitApi = Git.init().setDirectory(repositoryPath).call();
			this.gitApi.getRepository().setGitwebDescription(description);
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to init Git repository", e);
		}
	}
	
	@Override
	public String clone(
			String namespace, String projectId, String uri, 
			CredentialsProvider credentialsProvider)
			throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `clone` on an attached instance");
		}

		File path = 
			Paths.get(this.getRepositoryBasePath().toURI())
				.resolve(namespace)
				.resolve(projectId)
				.toFile();

		try {
			CloneCommand cloneCommand = 
					jGitCommandFactory.newCloneCommand()
					.setURI(uri).setDirectory(path);

			cloneCommand.setCredentialsProvider(credentialsProvider);
			
			this.gitApi = cloneCommand.call();			
		}
		catch (GitAPIException e) {
			throw new IOException(
				"Failed to clone remote Git repository", e
			);
		}

		return path.getName();
	}

	@Deprecated
	public String cloneWithSubmodules(String group, String uri, CredentialsProvider credentialsProvider)
			throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `cloneWithSubmodules` on an attached instance");
		}

		String repositoryName = uri.substring(uri.lastIndexOf("/") + 1);
		if (repositoryName.endsWith(".git")) {
			repositoryName = repositoryName.substring(0, repositoryName.length() - 4);
		}
		File path = 
			Paths.get(this.getRepositoryBasePath().toURI())
				.resolve(group)
				.resolve(repositoryName).toFile();

		try {
			CloneCommand cloneCommand = 
					jGitCommandFactory.newCloneCommand()
					.setURI(uri).setDirectory(path);

			cloneCommand.setCredentialsProvider(credentialsProvider);
			
			this.gitApi = cloneCommand.call();
			
			this.gitApi.submoduleInit().call();

			SubmoduleUpdateCommand submoduleUpdateCommand = 
					jGitCommandFactory.newSubmoduleUpdateCommand(gitApi.getRepository());
			submoduleUpdateCommand.setCredentialsProvider(credentialsProvider);
			submoduleUpdateCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException(
				"Failed to clone remote Git repository", e
			);
		}

		return path.getName();
	}
	
	/**
	 * Opens an existing Git repository with the directory name <code>name</code>.
	 *
	 * @param namespace the Gitlab namespace of the Git repository to open
	 * @param name the directory name of the Git repository to open
	 * @throws IOException if the Git repository couldn't be found or
	 *         couldn't be opened for some other reason
	 */
	@Override
	public void open(String namespace, String name) throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `open` on an attached instance");
		}

		File repositoryPath = 
				Paths.get(getRepositoryBasePath().toURI()).resolve(namespace).resolve(name).toFile();

		// could also check for the absence of a child .git directory
		if (!repositoryPath.exists() || !repositoryPath.isDirectory()) {
			throw new IOException(
				String.format(
					"Couldn't find a Git repository with the name '%s' at base path '%s'. " +
					"Did you mean to call `init`?",
						name,
					this.getRepositoryBasePath()
				)
			);
		}

		File gitDir = new File(repositoryPath, ".git");
		if (gitDir.isFile()) {
			gitDir = Paths.get(repositoryPath.toURI()).resolve(
				FileUtils.readFileToString(gitDir, StandardCharsets.UTF_8)
				.replace("gitdir:", "")
				.trim()).normalize().toFile();
		}
		this.gitApi = Git.open(gitDir);
	}

	@Override
	public String getRevisionHash() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't determine root revision hash on a detached instance");
		}
		ObjectId headRevision = this.gitApi.getRepository().resolve(Constants.HEAD);
		if (headRevision == null) {
			return NO_COMMITS_YET;
		}
		else {
			return headRevision.getName();
		}
	}
	
	@Deprecated
	public List<String> getSubmodulePaths() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't determine submodules from a detached instance");
		}
		try {
			List<String> paths = new ArrayList<>();
			try (SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(this.gitApi.getRepository())) {
				while (submoduleWalk.next()) {
					paths.add(submoduleWalk.getModulesPath());
				}
				
				return paths;
			}
		}
		catch (Exception e) {
			throw new IOException(e);
		}			
	}

	@Deprecated
	public List<Pair<String, String>> getSubmoduleConfigs() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't determine submodules from a detached instance");
		}
		try {
			List<Pair<String, String>> paths = new ArrayList<>();
			try (SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(this.gitApi.getRepository())) {
				while (submoduleWalk.next()) {
					paths.add(new Pair<>(submoduleWalk.getModulesPath(), submoduleWalk.getConfigUrl()));
				}
				
				return paths;
			}
		}
		catch (Exception e) {
			throw new IOException(e);
		}			
	}
	@Override
	public String getRevisionHash(String submodule) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't determine root revision hash on a detached instance");
		}
		
		try (Repository subModuleRepo = SubmoduleWalk
				.getSubmoduleRepository(this.gitApi.getRepository(), submodule)) {
			
			ObjectId headRevision = subModuleRepo.resolve(Constants.HEAD);
			
			if (headRevision == null) {
				return "no_commits_yet";
			}
			else {
				return headRevision.getName();
			}
		}
		
	}

	/**
	 * Writes a new file with contents <code>bytes</code> to disk at path <code>targetFile</code>
	 * and adds it to the attached Git repository.
	 * <p>
	 * It's the caller's responsibility to call {@link #commit(String, String, String)}.
	 *
	 * @param targetFile a {@link File} object representing the target path
	 * @param bytes the file contents
	 * @throws IOException if the file contents couldn't be written to disk
	 *         or the file couldn't be added for some other reason
	 */
	@Override
	public void add(File targetFile, byte[] bytes) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `add` on a detached instance");
		}

		try (FileOutputStream fileOutputStream = FileUtils.openOutputStream(targetFile)) {
			fileOutputStream.write(bytes);

			Path basePath = this.gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(targetFile.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);

			this.gitApi
				.add()
				.addFilepattern(FilenameUtils.separatorsToUnix(relativeFilePath.toString()))
				.call();
		}
		catch (GitAPIException e) {
			throw new IOException(e);
		}
	}
	
	public void add(Path relativePath) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `add` on a detached instance");
		}
		try {
			this.gitApi
			.add()
			.addFilepattern(FilenameUtils.separatorsToUnix(relativePath.toString()))
			.call();
		}
		catch (GitAPIException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void remove(File targetFile) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `remove` on a detached instance");
		}

		try {

			Path basePath = this.gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(targetFile.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);

			this.gitApi
				.rm()
				.setCached(true)
				.addFilepattern(FilenameUtils.separatorsToUnix(relativeFilePath.toString()))
				.call();
			
			if (targetFile.isDirectory()) {
				FileUtils.deleteDirectory(targetFile);
			}
			else if (targetFile.exists() && !targetFile.delete()) {
				throw new IOException(String.format(
						"could not remove %s", 
						targetFile.toString()));
			}
		}
		catch (GitAPIException e) {
			throw new IOException(e);
		}
	}
	
	@Deprecated
	public void removeSubmodule(File submodulePath) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `removeSubmodule` on a detached instance");
		}
		try {
			Path basePath = this.gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(submodulePath.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);
			String relativeUnixStyleFilePath = FilenameUtils.separatorsToUnix(relativeFilePath.toString());
			
		    File gitSubmodulesFile = 
		    	new File(
		    			this.gitApi.getRepository().getWorkTree(), 
		    			Constants.DOT_GIT_MODULES );
		    FileBasedConfig gitSubmodulesConfig = 
		    		new FileBasedConfig(null, gitSubmodulesFile, FS.DETECTED);		
		    gitSubmodulesConfig.load();
		    gitSubmodulesConfig.unsetSection(
		    		ConfigConstants.CONFIG_SUBMODULE_SECTION, relativeUnixStyleFilePath);
		    gitSubmodulesConfig.save();
		    StoredConfig repositoryConfig = this.getGitApi().getRepository().getConfig();
		    repositoryConfig.unsetSection(
		    	ConfigConstants.CONFIG_SUBMODULE_SECTION, relativeUnixStyleFilePath);
		    repositoryConfig.save();
		    
		    gitApi.add().addFilepattern(Constants.DOT_GIT_MODULES).call();
		    gitApi.rm().setCached(true).addFilepattern(relativeUnixStyleFilePath).call();

			File submoduleGitDir = 
				basePath
				.resolve(Constants.DOT_GIT)
				.resolve(Constants.MODULES)
				.resolve(relativeFilePath).toFile();
			
			detach();
			
			FileUtils.deleteDirectory(submoduleGitDir);
			
			FileUtils.deleteDirectory(absoluteFilePath.toFile());
		}
		catch (GitAPIException | ConfigInvalidException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public String removeAndCommit(
			File targetFile, boolean removeEmptyParent, 
			String commitMsg, String committerName, String committerEmail)
			throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `removeAndCommit` on a detached instance");
		}
		
		File parentDir = targetFile.getParentFile();
		
		this.remove(targetFile);
		
		if ((parentDir != null) && parentDir.isDirectory() && removeEmptyParent) {
			String[] content = parentDir.list();
			if ((content != null) && content.length == 0) {
				parentDir.delete();
			}
		}
		
		return this.commit(commitMsg, committerName, committerEmail, false);
	}

	/**
	 * Writes a new file with contents <code>bytes</code> to disk at path <code>targetFile</code>,
	 * adds it to the attached Git repository and commits.
	 * <p>
	 * Calls {@link #add(File, byte[])} and {@link #commit(String, String, String)} internally.
	 *
	 * @param targetFile a {@link File} object representing the target path
	 * @param bytes the file contents
	 * @return the revisionHash of the commit
	 * @throws IOException if the file contents couldn't be written to disk
	 *         or the commit operation failed
	 */
	@Override
	public String addAndCommit(
		File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail)
			throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `addAndCommit` on a detached instance");
		}

		this.add(targetFile, bytes);
		return this.commit(commitMsg, committerName, committerEmail, false);
	}

	/**
	 * Commits pending changes to the attached Git repository.
	 *
	 * @param message the commit message
	 * @return revision hash
	 * @throws IOException if the commit operation failed
	 */
	@Override
	public String commit(String message, String committerName, String committerEmail, boolean force)
			throws IOException {
		return this.commit(message, committerName, committerEmail, false, force);
	}

	@Override
	public String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `commit` on a detached instance");
		}

		try {
			if (force || gitApi.status().call().hasUncommittedChanges()) {
				return this.gitApi
					.commit()
					.setMessage(message)
					.setCommitter(committerName, committerEmail)
					.setAll(all)
					.call()
					.getName();
			}
			else {
				return getRevisionHash();
			}
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to commit", e);
		}
	}

	/**
	 * Pushes commits made locally on the user branch to the associated remote ('origin') repository and branch.
	 *
	 * @param credentialsProvider a {@link CredentialsProvider} to use for authentication
	 * @return a {@link List<PushResult>} containing the results of the push operation
	 * @throws IOException if an error occurs when pushing
	 */
	@Override
	public List<PushResult> push(CredentialsProvider credentialsProvider) throws IOException {
		return push(credentialsProvider, null, 0, false);
	}

	/**
	 * Pushes commits made locally on the master branch to the associated remote ('origin') repository and branch.
	 *
	 * @param credentialsProvider a {@link CredentialsProvider} to use for authentication
	 * @return a {@link List<PushResult>} containing the results of the push operation
	 * @throws IOException if an error occurs when pushing
	 */
	@Override
	public List<PushResult> pushMaster(CredentialsProvider credentialsProvider) throws IOException {
		return push(credentialsProvider, Constants.MASTER, 0, false);
	}

	/**
	 * Pushes commits made locally on the currently checked out branch to the associated remote ('origin') repository and branch.
	 * <p>
	 * Skips branch checks - only to be used for project migration from CATMA 6 -> 7.
	 *
	 * @param credentialsProvider a {@link CredentialsProvider} to use for authentication
	 * @return a {@link List<PushResult>} containing the results of the push operation
	 * @throws IOException if an error occurs when pushing
	 */
	public List<PushResult> pushWithoutBranchChecks(CredentialsProvider credentialsProvider) throws IOException {
		return push(credentialsProvider, null, 0, true);
	}

	/**
	 * Pushes commits made locally on the specified branch to the associated remote ('origin') repository and branch.
	 *
	 * @param credentialsProvider a {@link CredentialsProvider} to use for authentication
	 * @param branch the branch to push, defaults to the user branch if null
	 * @param tryCount how often this push has been attempted already (start with 0, used internally to limit recursive retries)
	 * @param skipBranchChecks whether to skip branch checks, normally false
	 * @throws IOException if an error occurs when pushing
	 */
	private List<PushResult> push(CredentialsProvider credentialsProvider, String branch, int tryCount, boolean skipBranchChecks) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `push` on a detached instance");
		}

		List<PushResult> result = new ArrayList<>();

		if (CATMAPropertyKey.DEV_PREVENT_PUSH.getBooleanValue()) {
			logger.warning(String.format("FAKE PUSH - %s", getRemoteUrl(null)));
			return result;
		}

		try {
			String currentBranch = gitApi.getRepository().getBranch();

			if (!skipBranchChecks) {
				if (branch != null && !currentBranch.equals(branch)) {
					throw new IOException(
							String.format("Aborting push - branch to push was \"%s\" but currently checked out branch is \"%s\"", branch, currentBranch)
					);
				}
				if (branch == null && !currentBranch.equals(username)) {
					throw new IOException(
							String.format(
									"Aborting push - branch to push was null (= user branch \"%s\") but currently checked out branch is \"%s\"",
									username,
									currentBranch
							)
					);
				}
			}

			PushCommand pushCommand = gitApi.push();
			pushCommand.setCredentialsProvider(credentialsProvider);
			pushCommand.setRemote(Constants.DEFAULT_REMOTE_NAME);
			Iterable<PushResult> pushResults = pushCommand.call();

			for (PushResult pushResult : pushResults) {
				for (RemoteRefUpdate remoteRefUpdate : pushResult.getRemoteUpdates()) {
					logger.info(String.format("PushResult: %s", remoteRefUpdate));
				}

				result.add(pushResult);
			}
		}
		catch (GitAPIException e) {
			// sometimes GitLab refuses to accept the push and returns this error message
			// subsequent push attempts succeed however, so we retry the push up to 3 times before giving up
			if (e.getMessage().contains("authentication not supported") && tryCount < 3) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException ignored) {}

				push(credentialsProvider, branch, tryCount + 1, skipBranchChecks);
			}
			else {
				// give up, push not possible or unexpected error
				throw new IOException(String.format("Failed to push, tried %d times", tryCount + 1), e);
			}
		}

		return result;
	}

	/**
	 * Fetches refs from the associated remote repository ('origin' remote).
	 *
	 * @param username the username to authenticate with
	 * @param password the password to authenticate with
	 * @throws IOException if the fetch operation failed
	 */
	@Override
	public void fetch(CredentialsProvider credentialsProvider) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `fetch` on a detached instance");
		}

		try {
			FetchCommand fetchCommand = this.gitApi.fetch();
			fetchCommand.setCredentialsProvider(credentialsProvider);
			fetchCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to fetch", e);
		}
	}
	
	@Override
	public boolean hasRef(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `hasRef` on a detached instance");
		}
		
		return this.gitApi.getRepository().findRef(branch) != null;
	}
	
	public boolean hasRemoteRef(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `hasRef` on a detached instance");
		}		
		return this.gitApi.getRepository().resolve("refs/remotes/"+branch) != null;
	}
	
	public boolean canMerge(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `canMerge` on a detached instance");
		}
//		ThreeWayMerger merger = 
//				MergeStrategy.RECURSIVE.newMerger(this.gitApi.getRepository(), true);

		try (ClosableRecursiveMerger merger = new ClosableRecursiveMerger(this.gitApi.getRepository(), true)) { 
			Ref ref = this.gitApi.getRepository().findRef(branch);
			if (ref != null) {
				ObjectId headCommit = this.gitApi.getRepository().resolve(Constants.HEAD);
				return merger.merge(
						true, headCommit, ref.getObjectId());
			}
			return false;
		}
	}
	
	@Override
	public MergeResult merge(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `merge` on a detached instance");
		}

		try {
			MergeCommand mergeCommand = this.gitApi.merge();
			mergeCommand.setFastForward(FastForwardMode.FF);
			
			Ref ref = this.gitApi.getRepository().findRef(branch);
			if (ref != null) {
				mergeCommand.include(ref);
			} 
			
			return mergeCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to merge", e);
		}		
	}
	
	@Override
	public void rebase(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `rebase` on a detached instance");
		}

		try {
			RebaseCommand rebaseCommand = this.gitApi.rebase();

			rebaseCommand.setUpstream(branch);

			rebaseCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to rebase", e);
		}		
	}

	/**
	 * Checks out a branch or commit identified by <code>name</code>.
	 *
	 * @param name the name of the branch or commit to check out
	 * @throws IOException if the checkout operation failed
	 */
	@Override
	public void checkout(String name) throws IOException {
		this.checkout(name, false);
	}
	
	/**
	 * Checks out a branch or commit identified by <code>name</code>.
	 *
	 * @param name the name of the branch or commit to check out
	 * @param createBranch equals to -b CLI option
	 * @throws IOException if the checkout operation failed
	 */
	@Override
	public void checkout(String name, boolean createBranch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `checkout` on a detached instance");
		}

		try {
			if (!this.gitApi.getRepository().getBranch().equals(name)) {
				CheckoutCommand checkoutCommand = this.gitApi.checkout();
				if (createBranch) {
					List<Ref> refs = this.gitApi.branchList().call();
					if (refs
						.stream()
						.map(rev -> rev.getName())
						.filter(revName -> revName.equals("refs/heads/"+name))
						.findFirst()
						.isPresent()) {
						createBranch = false;
					}
					try (RevWalk revWalk = new RevWalk(this.gitApi.getRepository())) {
						ObjectId commitId = this.gitApi.getRepository().resolve("refs/heads/"+Constants.MASTER);
						// can be null, if a Project is still empty, 
						// branch creation will fail nevertheless, because JGit cannot create a branch wihtout a startpoint
						if (commitId != null) { 
							RevCommit commit = revWalk.parseCommit(commitId);
							checkoutCommand.setStartPoint(commit);
						}
					}
				}
				checkoutCommand.setCreateBranch(createBranch);
				checkoutCommand.setName(name).call();
				
				if (createBranch) {
					StoredConfig config = this.gitApi.getRepository().getConfig();
					config.setString(
							ConfigConstants.CONFIG_BRANCH_SECTION, name, 
							ConfigConstants.CONFIG_KEY_REMOTE, "origin");
					config.setString(
							ConfigConstants.CONFIG_BRANCH_SECTION, name, 
							ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/" + name);
					config.save();
				}
			}
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to checkout", e);
		}
	}
	
	public void checkoutNewFromBranch(String name, String baseBranch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `checkout` on a detached instance");
		}

		try {
			if (!this.gitApi.getRepository().getBranch().equals(name)) {
				CheckoutCommand checkoutCommand = this.gitApi.checkout();
				List<Ref> refs = this.gitApi.branchList().call();
				if (refs
					.stream()
					.map(rev -> rev.getName())
					.filter(revName -> revName.equals("refs/heads/"+name))
					.findFirst()
					.isPresent()) {
				}
				try (RevWalk revWalk = new RevWalk(this.gitApi.getRepository())) {
					ObjectId commitId = this.gitApi.getRepository().resolve("refs/heads/"+baseBranch);
					// can be null, if a Project is still empty, 
					// branch creation will fail nevertheless, because JGit cannot create a branch wihtout a startpoint
					if (commitId != null) { 
						RevCommit commit = revWalk.parseCommit(commitId);
						checkoutCommand.setStartPoint(commit);
					}
				}
				checkoutCommand.setCreateBranch(true);
				checkoutCommand.setName(name).call();
				
				StoredConfig config = this.gitApi.getRepository().getConfig();
				config.setString(
						ConfigConstants.CONFIG_BRANCH_SECTION, name, 
						ConfigConstants.CONFIG_KEY_REMOTE, "origin");
				config.setString(
						ConfigConstants.CONFIG_BRANCH_SECTION, name, 
						ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/" + name);
				config.save();
			}
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to checkout", e);
		}
	}
	public void checkoutFromOrigin(String name) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `checkoutFromOrigin` on a detached instance");
		}

		try {
			boolean createBranch = true;
			if (!this.gitApi.getRepository().getBranch().equals(name)) {
				CheckoutCommand checkoutCommand = this.gitApi.checkout();
				List<Ref> refs = this.gitApi.branchList().call();
				if (refs
					.stream()
					.map(rev -> rev.getName())
					.filter(revName -> revName.equals("refs/heads/"+name))
					.findFirst()
					.isPresent()) {
					createBranch = false;
				}
				try (RevWalk revWalk = new RevWalk(this.gitApi.getRepository())) {
					ObjectId commitId = this.gitApi.getRepository().resolve("refs/remotes/origin/"+name);
					RevCommit commit = revWalk.parseCommit(commitId);
					checkoutCommand.setStartPoint(commit);
				}
				checkoutCommand.setCreateBranch(createBranch);
				checkoutCommand.setName(name).call();
				
				if (createBranch) {
					StoredConfig config = this.gitApi.getRepository().getConfig();
					config.setString(
							ConfigConstants.CONFIG_BRANCH_SECTION, name, 
							ConfigConstants.CONFIG_KEY_REMOTE, "origin");
					config.setString(
							ConfigConstants.CONFIG_BRANCH_SECTION, name, 
							ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/" + name);
					config.save();
				}
			}
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to checkout", e);
		}
	}

	/**
	 * Gets the HEAD revision hash for the submodule with the name <code>submoduleName</code>.
	 *
	 * @param submoduleName the name of the submodule whose HEAD revision hash to get
	 * @return a revision hash
	 * @throws IOException if a submodule with the name <code>submoduleName</code> doesn't exist
	 *                                            or if the operation failed for another reason
	 */
	@Deprecated
	public String getSubmoduleHeadRevisionHash(String submoduleName) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getSubmoduleHeadRevisionHash` on a detached instance");
		}

		try {
			SubmoduleStatusCommand submoduleStatusCommand = this.gitApi.submoduleStatus();
			submoduleStatusCommand.addPath(submoduleName);
			
			Map<String, SubmoduleStatus> results = submoduleStatusCommand.call();

			if (!results.containsKey(submoduleName)) {
				throw new IOException(
						String.format("Failed to get HEAD revision hash for submodule `%s`. " +
								"A submodule with that name does not appear to exist.", submoduleName)
				);
			}

			return results.get(submoduleName).getHeadId().getName();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to get submodule status", e);
		}
	}
	
	@Override
	public boolean hasUncommitedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `status` on a detached instance");
		}
		
		try {
			return gitApi.status().call().hasUncommittedChanges();
		} catch (GitAPIException e) {
			throw new IOException("Failed to check for uncommited changes", e);
		}
	}

	@Deprecated
	public boolean hasUncommitedChangesWithSubmodules(Set<String> submodules) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `status` on a detached instance");
		}
		
		try {
			StatusCommand statusCommand = gitApi.status();
			for (String submodule : submodules) {
				statusCommand.addPath(submodule);
			}
			
			statusCommand.addPath(".");
			
			return statusCommand.call().hasUncommittedChanges();
		} catch (GitAPIException e) {
			throw new IOException("Failed to check for uncommited changes", e);
		}
	}
	
	@Override
	public boolean hasUntrackedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `status` on a detached instance");
		}
		
		try {
			Status status = gitApi.status().call();
			
			return !status.getUntracked().isEmpty() || !status.getUntrackedFolders().isEmpty();
		} catch (GitAPIException e) {
			throw new IOException("Failed to check for untracked changes", e);
		}
	}
	
	@Override
	public String addAllAndCommit(
			String message, String committerName, String committerEmail, boolean force) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `addAllAndCommit` on a detached instance");
		}

		try {
			gitApi.add().addFilepattern(".").call();
			
			List<DiffEntry> diffEntries = gitApi.diff().call();
			if (!diffEntries.isEmpty()) {
				RmCommand rmCmd = gitApi.rm();
				for (DiffEntry entry : diffEntries) {
					if (entry.getChangeType().equals(ChangeType.DELETE)) {
						rmCmd.addFilepattern(entry.getOldPath());
					}
				}
				
				rmCmd.call();
			}
			
			if (force || gitApi.status().call().hasUncommittedChanges()) {
				return commit(message, committerName, committerEmail, force);
			}
			else {
				return getRevisionHash();
			}
			
		} catch (GitAPIException e) {
			throw new IOException("Failed to add and commit changes", e);
		}
	}

	@Override // AutoCloseable
	public void close() {
		if (this.gitApi != null) {
			try {
				// apparently JGit doesn't close Git's internal Repository instance
				// on its close. We need to call the close method of the Repository explicitly 
				// to avoid open handles to pack files
				// see https://stackoverflow.com/questions/31764311/how-do-i-release-file-system-locks-after-cloning-repo-via-jgit
				// see maybe related https://bugs.eclipse.org/bugs/show_bug.cgi?id=439305
				this.gitApi.getRepository().close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			this.gitApi.close();
			this.gitApi = null;
		}
	}
	
	@Override
	public Status getStatus() throws IOException {
		try {
			if (!isAttached()) {
				throw new IllegalStateException("Can't call `getStatus` on a detached instance");
			}
			
			return gitApi.status().call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to retrieve the status", e);
		}
	}

	@Deprecated
	public void initAndUpdateSubmodules(CredentialsProvider credentialsProvider, Set<String> submodules) throws Exception {
		
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `initAndUpdateSubmodules` on a detached instance");
		}

		SubmoduleInitCommand submoduleInitCommand = this.gitApi.submoduleInit();
		submoduleInitCommand.call();
		
		if (!submodules.isEmpty()) {
			SubmoduleUpdateCommand submoduleUpdateCommand = 
				jGitCommandFactory.newSubmoduleUpdateCommand(gitApi.getRepository());
			submodules.forEach(submodule -> submoduleUpdateCommand.addPath(submodule));
			submoduleUpdateCommand.setCredentialsProvider(credentialsProvider);
			submoduleUpdateCommand.call();
		}		
	}
	
	
	private Config getDotGitModulesConfig(ObjectId objectId) throws Exception {
        ObjectLoader loader = gitApi.getRepository().open(objectId);
        String content = new String(loader.getBytes(), "UTF-8");	
        
        Config config = new Config();
        config.fromText(content);
        return config;
	}
		
	@Override
	public List<CommitInfo> getUnsynchronizedChanges() throws Exception {
		List<CommitInfo> result = new ArrayList<>();
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `hasUnsynchronizedChanges` on a detached instance");
		}		
	
		try {
			if (this.gitApi.getRepository().resolve(Constants.HEAD) == null) {
				return result; // no HEAD -> new empty Project, no commits yet
			}
			
			
			List<Ref> refs = this.gitApi.branchList().setListMode(ListMode.REMOTE).call();
			Iterable<RevCommit> commits = null;
			
			if (refs.isEmpty()) {
				// project never synchronized
				
				commits = this.gitApi.log().call();
			}
			else {
				ObjectId remote =
					this.gitApi.getRepository().resolve("refs/remotes/origin/" + username); 
				if (remote != null) {
					commits = this.gitApi.log().addRange(
							remote, 
							this.gitApi.getRepository().resolve("refs/heads/" + username)).call();
				}
				else {
					commits = Collections.<RevCommit>emptyList();
				}
			}
			
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen()));
			}
			
		} catch (GitAPIException e) {
			throw new IOException("Cannot check for unsynchronized changes!", e);
		}
		
		return result;
	}
	

	@Override
	public Set<String> getDeletedResourcesFromLog(
			Set<String> resourceIds, String resourceDir) throws IOException {
		try {
			Set<String> result = new HashSet<String>();
			ObjectId objectId = gitApi.getRepository().resolve("refs/heads/"+username);
			if (objectId != null) {
				Iterator<RevCommit> remoteCommitIterator =
						gitApi.log().add(objectId).addPath(resourceDir).call().iterator();
				
				while(remoteCommitIterator.hasNext()) {
					RevCommit revCommit = remoteCommitIterator.next();
					String fullMsg = revCommit.getFullMessage();
					if (fullMsg.startsWith("Removing Document")) {
						for (String resourceId : resourceIds) {
							if (!result.contains(resourceId) && fullMsg.endsWith(resourceId)) {
								result.add(resourceId);
							}
						}
					}
				}
			}	
			return result;
		}
		catch (GitAPIException e) {
			throw new IOException("Cannot verify deleted resources!", e);
		}
	}
	
	public void abortMerge(MergeResult mergeResult) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `abortMerge` on a detached instance");
		}		
		
		// clear the merge state
		this.gitApi.getRepository().writeMergeCommitMsg(null);
		this.gitApi.getRepository().writeMergeHeads(null);

		// reset the index and work directory to HEAD
		try {
			Git.wrap(this.gitApi.getRepository()).reset().setMode(ResetType.HARD).call();
		}
		catch (GitAPIException e) {
			throw new IOException("Could not abort conflicted merge!", e);
		}
	}
	
	@Override
	public List<String> getRemoteBranches() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getRemoteBranches` on a detached instance");
		}		
		try {
			List<Ref> branches = 
					this.gitApi.branchList().setListMode(ListMode.REMOTE).call();
			return branches.stream().map(ref -> ref.getName()).collect(Collectors.toList());
		} catch (GitAPIException e) {
			throw new IOException("Could not retrieve remote branches", e);
		}
	}
	
	@Override
	public Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getBranchDifferences` on a detached instance");
		}		
		
		checkout(username, true);  // the user branch has to be present at this point
		
		DiffCommand diffCommand = this.gitApi.diff();
		
		ObjectId thisUserBranchHeadRevisionTree = 
				this.gitApi.getRepository().resolve("refs/heads/" + username + "^{tree}");
		ObjectId otherBranchRevisionTree = 
				this.gitApi.getRepository().resolve(otherBranchName + "^{tree}");
			
		Set<String> paths = new HashSet<String>();

		if (thisUserBranchHeadRevisionTree != null && otherBranchRevisionTree != null) {
			
			ObjectReader reader = this.gitApi.getRepository().newObjectReader();
			
            CanonicalTreeParser thisUserBranchHeadRevisionTreeParser = 
            		new CanonicalTreeParser();
            thisUserBranchHeadRevisionTreeParser.reset(reader, thisUserBranchHeadRevisionTree);
            
            CanonicalTreeParser otherBranchRevisionTreeParser = new CanonicalTreeParser();
            otherBranchRevisionTreeParser.reset(reader, otherBranchRevisionTree);

			diffCommand.setOldTree(thisUserBranchHeadRevisionTreeParser);
			diffCommand.setNewTree(otherBranchRevisionTreeParser);
			
			try {
				List<DiffEntry> result = diffCommand.call();
				for (DiffEntry diffEntry : result) {
					if (!diffEntry.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
						paths.add(diffEntry.getNewPath());
					}
				}
			} catch (GitAPIException e) {
				throw new IOException("Could not get git diff info!", e);
			}
		}
		return paths;
	}
	
	@Override
	public List<CommitInfo> getTheirPublishedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getTheirPublishedChanges` on a detached instance");
		}		
		
		List<CommitInfo> result = new ArrayList<>();
		
		if (this.gitApi.getRepository().resolve(Constants.HEAD) == null) {
			return result; // no HEAD -> new empty Project, no commits yet
		}
		
		
		try {
			List<Ref> refs = this.gitApi.branchList().setListMode(ListMode.REMOTE).call();
			Iterable<RevCommit> commits = null;
			
			if (!refs.isEmpty()) { // otherwise project has never been never synchronized

				ObjectId remote =
					this.gitApi.getRepository().resolve("refs/remotes/origin/" + Constants.MASTER); 
				
				if (remote != null) {
					commits = this.gitApi.log().addRange(
							this.gitApi.getRepository().resolve("refs/heads/" + username),
							remote).call();
				}
				else {
					commits = Collections.<RevCommit>emptyList();
				}
			}
			
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen()));
			}
			
			
		} catch (GitAPIException e) {
			throw new IOException("Could not get git log info!", e);
		}
		
		return result;
	}
	
	
	@Override
	public List<CommitInfo> getOurUnpublishedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getOurUnpublishedChanges` on a detached instance");
		}		
		
		List<CommitInfo> result = new ArrayList<>();
		
		if (this.gitApi.getRepository().resolve(Constants.HEAD) == null) {
			return result; // no HEAD -> new empty Project, no commits yet
		}
		
		
		try {
			List<Ref> refs = this.gitApi.branchList().setListMode(ListMode.REMOTE).call();
			Iterable<RevCommit> commits = null;
			
			if (!refs.isEmpty()) { // otherwise project has never been never synchronized

				ObjectId remote =
					this.gitApi.getRepository().resolve("refs/remotes/origin/" + Constants.MASTER); 
				
				if (remote != null) {
					commits = this.gitApi.log().addRange(
							remote,
							this.gitApi.getRepository().resolve("refs/heads/" + username)).call();
				}
				else {
					commits = Collections.<RevCommit>emptyList();
				}
			}
			
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen()));
			}
			
			
		} catch (GitAPIException e) {
			throw new IOException("Could not get git log info!", e);
		}
		
		return result;
	}
	
	@Deprecated
	public List<CommitInfo> getCommitsNeedToBeMergedFromDevToMaster() throws Exception {
		List<CommitInfo> result = new ArrayList<>();
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getCommitsNeedsToBeMergedFromDevToMaster` on a detached instance");
		}		
	
		try {
			if (this.gitApi.getRepository().resolve(Constants.HEAD) == null) {
				return result; // no HEAD -> new empty Project, no commits yet
			}
			
			
			Iterable<RevCommit> commits = null;

			ObjectId master =
				this.gitApi.getRepository().resolve("refs/heads/master"); 
			ObjectId dev = 
				this.gitApi.getRepository().resolve("refs/heads/dev");
			
			if (dev != null) {
				if (master != null) {
					commits = this.gitApi.log().addRange(master, dev).call();
				}
				else {
					commits = this.gitApi.log().call();
				}
			}
			else {
				commits = Collections.<RevCommit>emptyList();
			}
			
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen()));
			}
			
		} catch (GitAPIException e) {
			throw new IOException("Cannot check for unsynchronized changes!", e);
		}
		
		return result;
	}
	
	@Deprecated
	public List<CommitInfo> getCommitsNeedToBeMergedFromMasterToOriginMaster() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getCommitsNeedsToBeMergedFromMasterIntoOriginMaster` on a detached instance");
		}		
		
		List<CommitInfo> result = new ArrayList<>();
		
		if (this.gitApi.getRepository().resolve(Constants.HEAD) == null) {
			return result; // no HEAD -> new empty Project, no commits yet
		}
		
		
		try {
			List<Ref> refs = this.gitApi.branchList().setListMode(ListMode.REMOTE).call();
			Iterable<RevCommit> commits = null;
			
			if (!refs.isEmpty()) { // otherwise project has never been never synchronized

				ObjectId remote =
					this.gitApi.getRepository().resolve("refs/remotes/origin/" + Constants.MASTER); 
				
				if (remote != null) {
					commits = this.gitApi.log().addRange(
							remote,
							this.gitApi.getRepository().resolve("refs/heads/" + Constants.MASTER)).call();
				}
				else {
					commits = this.gitApi.log().call();
				}
			}
			else {
				commits = this.gitApi.log().call();
			}
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen()));
			}
			
			
		} catch (GitAPIException e) {
			throw new IOException("Could not get git log info!", e);
		}
		
		return result;
	}
	
	@Deprecated
	public List<CommitInfo> getCommitsNeedToBeMergedFromC6MigrationToOriginC6Migration(String migrationBranch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getCommitsNeedToBeMergedFromC6MigrationToOriginC6Migration` on a detached instance");
		}		
		
		List<CommitInfo> result = new ArrayList<>();
		
		if (this.gitApi.getRepository().resolve(Constants.HEAD) == null) {
			return result; // no HEAD -> new empty Project, no commits yet
		}
		
		
		try {
			List<Ref> refs = this.gitApi.branchList().setListMode(ListMode.REMOTE).call();
			Iterable<RevCommit> commits = null;
			
			if (!refs.isEmpty()) { // otherwise project has never been synchronized

				ObjectId remote =
					this.gitApi.getRepository().resolve("refs/remotes/origin/" + migrationBranch); 
				
				if (remote != null) {
					commits = this.gitApi.log().addRange(
							remote,
							this.gitApi.getRepository().resolve("refs/heads/" + migrationBranch)).call();
				}
				else {
					commits = this.gitApi.log().call();
				}
			}
			else {
				commits = this.gitApi.log().call();
			}
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen()));
			}
			
			
		} catch (GitAPIException e) {
			throw new IOException("Could not get git log info!", e);
		}
		
		return result;
	}
	
	public void remoteAdd(String name, String uri) throws IOException {
		
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `remoteAdd` on a detached instance");
		}		
		try {
			RemoteAddCommand remoteAddCommand = this.gitApi.remoteAdd();
			remoteAddCommand.setName(name);
			remoteAddCommand.setUri(new URIish(uri));
			remoteAddCommand.call();
		}
		catch (GitAPIException | URISyntaxException e) {
			throw new IOException("Could not add remote!", e);
		}
	}
	
	public void remoteRemove(String name) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `remoteRemove` on a detached instance");
		}		
	
		try {
			RemoteRemoveCommand remoteRemoveCommand = this.gitApi.remoteRemove();
			remoteRemoveCommand.setName(name);
			remoteRemoveCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Could not remove remote!", e);
		}
	}
	
	@Deprecated
	public boolean resolveRootConflicts(String projectId, CredentialsProvider credentialsProvider) throws IOException {
		Set<String> unresolved = new HashSet<String>();
		
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `resolveRootConflicts` on a detached instance");
		}

		DirCache dirCache = gitApi.getRepository().lockDirCache();
		
		try {
			if (dirCache.hasUnmergedPaths()) {
				
				Status status = gitApi.status().call();
				
				for (String conflictingSubmodule : status.getConflicting()) {
					
					StageState conflictState = 
							status.getConflictingStageState().get(conflictingSubmodule);
					
					switch (conflictState) {
					
					case BOTH_MODIFIED: {
						// get the base entry from where the branches diverge, the common ancestor version
						int baseIdx = dirCache.findEntry(conflictingSubmodule);
						DirCacheEntry baseEntry = dirCache.getEntry(baseIdx);
						
						// get their version, the being-merged in version
						DirCacheEntry theirEntry = dirCache.getEntry(baseIdx+2);
						
					    //Stage 0: 'normal', un-conflicted, all-is-well entry.
					    //Stage 1: 'base', the common ancestor version.
					    //Stage 2: 'ours', the target (HEAD) version.
					    //Stage 3: 'theirs', the being-merged-in version.
						
						if (theirEntry.getPathString().equals(conflictingSubmodule)
								&& theirEntry.getStage() == 3) {
							// we try to make sure that their version is included (merged) in the latest version
							// of this submodule
							ensureLatestSubmoduleRevision(
									baseEntry, theirEntry, conflictingSubmodule, credentialsProvider);
							try (Repository subModuleRepo = 
									SubmoduleWalk.getSubmoduleRepository(
											this.gitApi.getRepository(), conflictingSubmodule)) {
								// now get the current submodule revision (which includes the merge)
								ObjectId subModuleHeadRevision = 
										subModuleRepo.resolve(Constants.HEAD);
								
								baseEntry.setObjectId(subModuleHeadRevision);
							}
							break;
						}
						else {
							Logger.getLogger(this.getClass().getName()).severe(
									String.format(
										"Cannot resolve root conflict for submodule %1$s expected a 'theirs'-stage-3 commit entry but found none!",
									conflictingSubmodule));
							unresolved.add(conflictingSubmodule);
						}
					}
					case DELETED_BY_THEM: {
						unresolved.add(conflictingSubmodule);
						
						String ourTreeName = "refs/heads/master"; 
						RevCommit ourCommit = 
							gitApi.log().add(gitApi.getRepository().resolve(ourTreeName)).addPath(conflictingSubmodule).call().iterator().next();
						String ourLastCommitMsg = ourCommit.getFullMessage();
						
						String theirTreeName = "refs/remotes/origin/master"; 
						
						RevCommit theirCommit = 
							gitApi.log().add(gitApi.getRepository().resolve(theirTreeName)).addPath(conflictingSubmodule).call().iterator().next();
						
						if (theirCommit == null) {
							
							// couldn't find their commit based on the conflicting submodule path
							// we try to find it based on the DOT_GIT_MODULES file and the resourceId in the commit message
							Iterator<RevCommit> remoteCommitIterator =
								gitApi.log().add(gitApi.getRepository().resolve(theirTreeName)).addPath(Constants.DOT_GIT_MODULES).call().iterator();
							String resourceId = conflictingSubmodule.substring(conflictingSubmodule.indexOf('/')+1);
							while(remoteCommitIterator.hasNext()) {
								RevCommit revCommit = remoteCommitIterator.next();
								if (revCommit.getFullMessage().contains(resourceId)) {
									theirCommit = revCommit;
									break;
								}
							}
						}
						
						String theirLastCommitMsg = "no commit found";
						if (theirCommit != null) {
							theirLastCommitMsg = theirCommit.getFullMessage();
						}
						Logger.getLogger(this.getClass().getName()).severe(
							String.format(
								"Cannot resolve root conflict DELETED_BY_THEM in %1$s "
								+ "with our commit %2$s '%3$s' "
								+ "and their commit %4$s '%5$s'", 
								conflictingSubmodule, 
								ourCommit.getName(), ourLastCommitMsg,
								theirCommit != null?theirCommit.getName():"N/A",
								theirLastCommitMsg));
						break;		
					}
					case DELETED_BY_US: {
						unresolved.add(conflictingSubmodule);

						String ourTreeName = "refs/heads/master"; 
						RevCommit ourCommit = 
							gitApi.log().add(gitApi.getRepository().resolve(ourTreeName)).addPath(conflictingSubmodule).call().iterator().next();
						String ourLastCommitMsg = ourCommit.getFullMessage();
						
						String theirTreeName = "refs/remotes/origin/master"; 
						RevCommit theirCommit = 
							gitApi.log().add(gitApi.getRepository().resolve(theirTreeName)).addPath(conflictingSubmodule).call().iterator().next();
						String theirLastCommitMsg = theirCommit.getFullMessage();
						
						Logger.getLogger(this.getClass().getName()).severe(
								String.format(
									"Cannot resolve root conflict DELETED_BY_US in %1$s "
									+ "with our commit %2$s '%3$s' "
									+ "and their commit %4$s '%5$s'", 
									conflictingSubmodule, 
									ourCommit.getName(), ourLastCommitMsg,
									theirCommit != null?theirCommit.getName():"N/A",
									theirLastCommitMsg));

						break;		
					}
					default: {
						unresolved.add(conflictingSubmodule);

						Logger.getLogger(this.getClass().getName()).severe(
								String.format(
									"Cannot resolve root conflict for submodule %1$s %2$s not supported yet!",
								conflictingSubmodule, conflictState.name()));
					}
					}

				}

				dirCache.write();
				dirCache.commit();
			}
			else {
				dirCache.unlock();
			}
		}
		catch (Exception e) {
			try  {
				dirCache.unlock();
			}
			catch (Exception e2) {
				e2.printStackTrace();
			}
			throw new IOException("Failed to resolve root conflicts", e);
		}
		
		return unresolved.isEmpty();
	}
	
	@Deprecated
	private void ensureLatestSubmoduleRevision(DirCacheEntry baseEntry, 
			DirCacheEntry theirEntry, String conflictingSubmodule,
			CredentialsProvider credentialsProvider) throws Exception {
		
		try (Repository submoduleRepo = 
			SubmoduleWalk.getSubmoduleRepository(this.gitApi.getRepository(), conflictingSubmodule)) {
			Git submoduleGitApi = Git.wrap(submoduleRepo);
			
			boolean foundTheirs = false;
			int tries = 10;
			while (!foundTheirs && tries > 0) {
				// iterate over the revisions until we find their commit or until we reach
				// the the common ancestor version (base)
				Iterator<RevCommit> revIterator = submoduleGitApi
				.log()
				.call()
				.iterator();
				while (revIterator.hasNext()) {
					RevCommit revCommit = revIterator.next();
					if (revCommit.getId().equals(theirEntry.getObjectId())) {
						// we found their version
						foundTheirs = true;
						break;
					}
					else if (revCommit.getId().equals(baseEntry.getObjectId())) {
						// we reached the common ancestor
						break;
					}
				}
				
				if (!foundTheirs) {
					// we reached the common ancestor without finding
					// their commit, so we pull again to see if it comes in now
					// and then we start over
					
					submoduleGitApi.checkout()
					.setName(Constants.MASTER)
					.setCreateBranch(false)
					.call();
					
					PullResult pullResult = 
						submoduleGitApi.pull().setCredentialsProvider(credentialsProvider).call();
					if (!pullResult.isSuccessful()) {
						throw new IllegalStateException(
							String.format(
							"Trying to get the latest commits for %1$s failed!", conflictingSubmodule));
					}
					
					submoduleGitApi
						.checkout()
						.setName(LocalGitRepositoryManager.DEFAULT_LOCAL_DEV_BRANCH)
						.setCreateBranch(false)
						.call();
					
					submoduleGitApi.rebase().setUpstream(Constants.MASTER).call();
				}
				tries --;
			}
			
			if (!foundTheirs) {
				Logger.getLogger(this.getClass().getName()).severe(
						String.format("Cannot resolve root conflict for submodule %1$s commit %2$s is missing!",
						conflictingSubmodule, theirEntry.getObjectId().toString()));
				throw new CommitMissingException(
					"Failed to synchronize the Project because of a missing commit, "
					+ "try again later or contact the system administrator!");
			}
		}
	}

	@Deprecated
	public void resolveGitSubmoduleFileConflicts() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `resolveGitSubmoduleFileConflicts` on a detached instance");
		}		
		
		DirCache dirCache = gitApi.getRepository().lockDirCache();
		try {
			int baseIdx = dirCache.findEntry(Constants.DOT_GIT_MODULES);

			// get base version
			DirCacheEntry baseEntry = dirCache.getEntry(baseIdx);

			Set<String> baseModules = null;
			Set<String> ourModules = null;
			Set<String> theirModules = null;

			Config ourConfig = null;
			Config theirConfig = null;
			
			if (baseEntry.getStage() == DirCacheEntry.STAGE_1) {
				// get our version
				DirCacheEntry ourEntry = dirCache.getEntry(baseIdx+1);
				// get their version, the being-merged in version
				DirCacheEntry theirEntry = dirCache.getEntry(baseIdx+2);

				
				Config baseConfig = getDotGitModulesConfig(baseEntry.getObjectId());

				ourConfig = getDotGitModulesConfig(ourEntry.getObjectId());

				theirConfig = getDotGitModulesConfig(theirEntry.getObjectId());
				
				baseModules = baseConfig.getSubsections(ConfigConstants.CONFIG_SUBMODULE_SECTION);
				ourModules = ourConfig.getSubsections(ConfigConstants.CONFIG_SUBMODULE_SECTION);
				theirModules = theirConfig.getSubsections(ConfigConstants.CONFIG_SUBMODULE_SECTION);
				
			}
			else if (baseEntry.getStage() == DirCacheEntry.STAGE_2) { // no common ancestor
				DirCacheEntry ourEntry = baseEntry;
				DirCacheEntry theirEntry = dirCache.getEntry(baseIdx+1);
				
				ourConfig = getDotGitModulesConfig(ourEntry.getObjectId());
				theirConfig = getDotGitModulesConfig(theirEntry.getObjectId());
				
				baseModules = new HashSet<String>();
				ourModules = ourConfig.getSubsections(ConfigConstants.CONFIG_SUBMODULE_SECTION);
				theirModules = theirConfig.getSubsections(ConfigConstants.CONFIG_SUBMODULE_SECTION);
			}
			
			for (String name : theirModules) {
				if (!ourModules.contains(name)) {
					if (baseModules.contains(name)) {
						//deleted by us
					}
					else {
						//added by them
						
						ourConfig.setString(
							ConfigConstants.CONFIG_SUBMODULE_SECTION, name,
							"path", 
							theirConfig.getString(ConfigConstants.CONFIG_SUBMODULE_SECTION, name, "path"));
						ourConfig.setString(
							ConfigConstants.CONFIG_SUBMODULE_SECTION, name,
							"url", 
							theirConfig.getString(ConfigConstants.CONFIG_SUBMODULE_SECTION, name, "url"));
					}
				}
			}
			
			for (String name : ourModules) {
				if (!theirModules.contains(name)) {
					if (baseModules.contains(name)) {
						//deleted by them
						ourConfig.unsetSection(ConfigConstants.CONFIG_SUBMODULE_SECTION, name);
					}
					else {
						//added by us
					}
				}
			}

			dirCache.unlock();
			
			add(
				new File(this.gitApi.getRepository().getWorkTree(), Constants.DOT_GIT_MODULES), 
				ourConfig.toText().getBytes("UTF-8"));
			
		}
		catch (Exception e) {
			try  {
				dirCache.unlock();
			}
			catch (Exception e2) {
				e2.printStackTrace();
			}
			throw new IOException("Failed to resolve root conflicts", e);
		}		
		
	}

	public CommitInfo getHeadCommit() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `resolveGitSubmoduleFileConflicts` on a detached instance");
		}		
		try {
			ObjectId headCommit = gitApi.getRepository().resolve(Constants.HEAD);
			if (headCommit != null) {
				Iterator<RevCommit> iterator = gitApi.log().add(headCommit).call().iterator();
				if (iterator.hasNext()) {
					RevCommit c = iterator.next();
					return new CommitInfo(c.getId().getName(), c.getFullMessage(), c.getAuthorIdent().getWhen());
				}
			}
			return null;
		}
		catch (GitAPIException e) {
			throw new IOException("Faild to retrieve HEAD commit!", e);
		}
	}

	public void reAddSubmodule(File submodulePath, String uri, CredentialsProvider credentialsProvider)
			throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `reAddSubmodule` on a detached instance");
		}
		try {

			Path basePath = this.gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(submodulePath.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);
			String relativeUnixStyleFilePath = FilenameUtils.separatorsToUnix(relativeFilePath.toString());
			
		
		    if (absoluteFilePath.toFile().list().length == 0) {
		    	File gitSubmodulesFile = 
		    			new File(
		    					this.gitApi.getRepository().getWorkTree(), 
		    					Constants.DOT_GIT_MODULES );
		    	FileBasedConfig gitSubmodulesConfig = 
		    			new FileBasedConfig(null, gitSubmodulesFile, FS.DETECTED );		
		    	gitSubmodulesConfig.load();
		    	gitSubmodulesConfig.unsetSection(
		    			ConfigConstants.CONFIG_SUBMODULE_SECTION, relativeUnixStyleFilePath);
		    	gitSubmodulesConfig.save();
		    	StoredConfig repositoryConfig = this.getGitApi().getRepository().getConfig();
		    	repositoryConfig.unsetSection(
		    			ConfigConstants.CONFIG_SUBMODULE_SECTION, relativeUnixStyleFilePath);
		    	repositoryConfig.save();

		    	gitApi.rm().setCached(true).addFilepattern(relativeUnixStyleFilePath).call();

		    	FileUtils.deleteDirectory(absoluteFilePath.toFile());
		    	
		    	// NB: Git doesn't understand Windows path separators (\) in the .gitmodules file
		    	String unixStyleRelativeSubmodulePath = FilenameUtils.separatorsToUnix(relativeFilePath.toString());
		    	
		    	SubmoduleAddCommand submoduleAddCommand = 
		    			jGitCommandFactory.newSubmoduleAddCommand(gitApi.getRepository())
		    			.setURI(uri)
		    			.setPath(unixStyleRelativeSubmodulePath);
		    	//needs permissions because the submodule is cloned from remote first and then added locally
		    	submoduleAddCommand.setCredentialsProvider(credentialsProvider);
		    	
		    	Repository repository = submoduleAddCommand.call();
		    	repository.close();
		    }
		}
		catch (GitAPIException | ConfigInvalidException e) {
			throw new IOException(e);
		}
	}
} 
