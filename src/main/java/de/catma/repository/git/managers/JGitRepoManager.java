package de.catma.repository.git.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleStatusCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.util.FS;

import de.catma.project.CommitInfo;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.managers.jgitcommand.RelativeJGitFactory;
import de.catma.user.User;

public class JGitRepoManager implements ILocalGitRepositoryManager, AutoCloseable {
	private final String repositoryBasePath;
	private String username;

	private Git gitApi;
	private JGitFactory jGitFactory;
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
		this.jGitFactory = new RelativeJGitFactory();
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
					jGitFactory.newCloneCommand()
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
	public String removeSubmodule(File submodulePath, String commitMsg, String committerName, String committerEmail) throws IOException {
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
		    		new FileBasedConfig(null, gitSubmodulesFile, FS.DETECTED );		
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

		    String projectRevisionHash = commit(commitMsg, committerName, committerEmail, false);
			
			File submoduleGitDir = 
				basePath
				.resolve(Constants.DOT_GIT)
				.resolve(Constants.MODULES)
				.resolve(relativeFilePath).toFile();
			
			detach();
			
			FileUtils.deleteDirectory(submoduleGitDir);
			
			FileUtils.deleteDirectory(absoluteFilePath.toFile());

			return projectRevisionHash;
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

	@Override
	public void push(CredentialsProvider credentialsProvider) throws IOException {
		push(credentialsProvider, null, 0);
	}
	
	@Override
	public void push_master(CredentialsProvider credentialsProvider) throws IOException {
		push(credentialsProvider, Constants.MASTER, 0);
	}

	/**
	 * Pushes commits made locally to the associated remote repository ('origin' remote).
	 *
	 * @param username the username to authenticate with
	 * @param password the password to authenticate with
	 * @param tryCount the number of attempts this push has been tried already 
	 * @throws IOException if the push operation failed
	 */
	private void push(CredentialsProvider credentialsProvider, String branch, int tryCount) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `push` on a detached instance");
		}

		try {
			if (!CATMAPropertyKey.devPreventPush.getValue(false)) {
				
				String currentBranch = this.gitApi.getRepository().getBranch();
				if ((branch != null && branch.equals(Constants.MASTER) && !currentBranch.equals(Constants.MASTER))
					|| (branch == null && !currentBranch.equals(this.username))) {
					throw new IOException(
							String.format(
								"Can only push branch %1$s, got %2$s!", 
								this.username, currentBranch));
				}
				
				PushCommand pushCommand = this.gitApi.push();
				pushCommand.setCredentialsProvider(credentialsProvider);
				pushCommand.setRemote(Constants.DEFAULT_REMOTE_NAME);
				Iterable<PushResult> pushResults = pushCommand.call();
				for (PushResult pushResult : pushResults) {
					for (RemoteRefUpdate remoteRefUpdate : pushResult.getRemoteUpdates()) {
						logger.info("PushResult " + remoteRefUpdate);
					}
				}
			}
			else {
				System.out.println(String.format("FAKE PUSH - %1$s", this.getRemoteUrl(null)));
			}
		}
		catch (GitAPIException e) {
			// sometimes Gitlab refuses to accept the push and gives this error messages
			// subsequent push attempts however pass through
			// so we try to push up to 3 times before giving up
			if (e.getMessage().contains("authentication not supported") && tryCount<3) {
				try {
					Thread.sleep(3);
				} catch (InterruptedException notOfInterest) {
				}
				push(credentialsProvider, branch, tryCount+1);
			}
			else {
				// give up, push not possible
				throw new IOException(String.format("Failed to push, tried %1$d times!", tryCount-1), e);
			}
		}
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
	
	public boolean canMerge(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `canMerge` on a detached instance");
		}
		ThreeWayMerger merger = 
				MergeStrategy.RECURSIVE.newMerger(this.gitApi.getRepository(), true);
		
		Ref ref = this.gitApi.getRepository().findRef(branch);
		if (ref != null) {
			return merger.merge(
					true, this.gitApi.getRepository().resolve(Constants.HEAD), ref.getObjectId());
		}
		return false;
	}
	
	@Override
	public MergeResult merge(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `merge` on a detached instance");
		}

		try {
			MergeCommand mergeCommand = this.gitApi.merge();
			
			
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
				jGitFactory.newSubmoduleUpdateCommand(gitApi.getRepository());
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
				commits = this.gitApi.log().addRange(
						this.gitApi.getRepository().resolve("refs/remotes/origin/" + username), 
						this.gitApi.getRepository().resolve("refs/heads/" + username)).call();
			}
			
			
			for (RevCommit c : commits) {
				result.add(new CommitInfo(c.getId().getName(), c.getFullMessage()));
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
	
	public void revert(MergeResult mergeResult) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `hasUnsynchronizedChanges` on a detached instance");
		}		
		
		RevertCommand revertCommand = this.gitApi.revert();
		
		for (ObjectId oid : mergeResult.getMergedCommits()) {
			revertCommand.include(oid);
		}
		try {
			revertCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Could not revert conflicted merge!", e);
		}
	}
}
