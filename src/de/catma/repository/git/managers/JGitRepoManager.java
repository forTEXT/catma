package de.catma.repository.git.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleStatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.user.User;

public class JGitRepoManager implements ILocalGitRepositoryManager, AutoCloseable {
	
	private final String repositoryBasePath;
	private String username;

	private Git gitApi;

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
	}

	/**
	 * Creates an instance of this class for the given {@link User} and opens an existing Git repository with the
	 * directory name <code>repositoryName</code>.
	 * <p>
	 * Note that the <code>catmaUser</code> argument is NOT used for authentication to remote Git servers. It is only
	 * used to organise repositories on the local file system, based on the User's identifier. Methods of this class
	 * that support authentication have their own username and password parameters for this purpose.
	 * <p>
	 * Calls {@link #open(String)} internally.
	 *
	 * @param repositoryBasePath the repo base path
	 * @param catmaUser a {@link User} object
	 * @param repositoryName the directory name of the Git repository to open
	 * @throws IOException if the Git repository couldn't be found or
	 *         couldn't be opened for some other reason
	 */
	public JGitRepoManager(String repositoryBasePath, User catmaUser, String group,
			String repositoryName)
			throws IOException {
		this(repositoryBasePath, catmaUser);

		this.open(group, repositoryName);
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

	public String clone(String group, String uri, File path, String username, String password)
			throws IOException {
		return clone(group, uri, path, username, password, false);
	}
	
	/**
	 * Clones a remote repository whose address is specified via the <code>uri</code> parameter.
	 *
	 * @param uri the URI of the remote repository to clone
	 * @param path the destination path of the clone operation
	 * @param username the username to authenticate with
	 * @param password the password to authenticate with
	 * @param initAndUpdateSubmodules init and update submodules
	 * @return the name of the cloned repository
	 */
	@Override
	public String clone(String group, String uri, File path, String username, String password, boolean initAndUpdateSubmodules)
			throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `clone` on an attached instance");
		}

		if (path == null) {
			String repositoryName = uri.substring(uri.lastIndexOf("/") + 1);
			if (repositoryName.endsWith(".git")) {
				repositoryName = repositoryName.substring(0, repositoryName.length() - 4);
			}
			path = Paths.get(this.getRepositoryBasePath().toURI()).resolve(group).resolve(repositoryName).toFile();
		}

		try {
			CloneCommand cloneCommand = Git.cloneRepository().setURI(uri).setDirectory(path);
			if (username != null) {
				cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					username, password
				));
			}
			this.gitApi = cloneCommand.call();
			
			if (initAndUpdateSubmodules) {
				this.gitApi.submoduleInit().call();
				this.gitApi.submoduleUpdate().call();
			}
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
	 * @param name the directory name of the Git repository to open
	 * @throws IOException if the Git repository couldn't be found or
	 *         couldn't be opened for some other reason
	 */
	@Override
	public void open(String group, String name) throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `open` on an attached instance");
		}

		File repositoryPath = Paths.get(getRepositoryBasePath().toURI()).resolve(group).resolve(name).toFile();

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
	public String getRevisionHash() throws Exception {
		if (!isAttached()) {
			throw new IllegalStateException("Can't determine root revision hash on a detached instance");
		}
		ObjectId headRevision = this.gitApi.getRepository().resolve(Constants.HEAD);
		if (headRevision == null) {
			return "no_commits_yet";
		}
		else {
			return headRevision.getName();
		}
	}
	
	@Override
	public String getRevisionHash(String submodule) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't determine root revision hash on a detached instance");
		}
		
		ObjectId headRevision = 
			SubmoduleWalk
			.getSubmoduleRepository(this.gitApi.getRepository(), submodule)
			.resolve(Constants.HEAD);
		if (headRevision == null) {
			return "no_commits_yet";
		}
		else {
			return headRevision.getName();
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

			this.gitApi.add().addFilepattern(relativeFilePath.toString()).call();
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
			if (targetFile.isDirectory()) {
				FileUtils.deleteDirectory(targetFile);
			}
			else if (!targetFile.delete()) {
				throw new IOException(String.format(
						"could not remove %s", 
						targetFile.toString()));
			}

			Path basePath = this.gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(targetFile.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);

			this.gitApi.rm().addFilepattern(relativeFilePath.toString()).call();
		}
		catch (GitAPIException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public String removeAndCommit(File targetFile, String committerName, String committerEmail)
			throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `removeAndCommit` on a detached instance");
		}

		this.remove(targetFile);
		String commitMessage = String.format("Removing %s", targetFile.getName());
		return this.commit(commitMessage, committerName, committerEmail);
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
	public String addAndCommit(File targetFile, byte[] bytes, String committerName, String committerEmail)
			throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `addAndCommit` on a detached instance");
		}

		this.add(targetFile, bytes);
		String commitMessage = String.format("Adding %s", targetFile.getName());
		return this.commit(commitMessage, committerName, committerEmail);
	}

	/**
	 * Commits pending changes to the attached Git repository.
	 *
	 * @param message the commit message
	 * @return revision hash
	 * @throws IOException if the commit operation failed
	 */
	@Override
	public String commit(String message, String committerName, String committerEmail)
			throws IOException {
		return this.commit(message, committerName, committerEmail, false);
	}

	@Override
	public String commit(String message, String committerName, String committerEmail, boolean all) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `commit` on a detached instance");
		}

		try {
			return this.gitApi
				.commit()
				.setMessage(message)
				.setCommitter(committerName, committerEmail)
				.setAll(all)
				.call()
				.getName();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to commit", e);
		}
	}
	/**
	 * Adds a new submodule to the attached Git repository.
	 *
	 * @param path a {@link File} object representing the target path of the submodule
	 * @param uri the URI of the remote repository to add as a submodule
	 * @param username the username to authenticate with
	 * @param password the password to authenticate with
	 * @throws IOException if the submodule couldn't be added
	 */
	@Override
	public void addSubmodule(File path, String uri, String username, String password)
			throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `addSubmodule` on a detached instance");
		}

		Path basePath = this.gitApi.getRepository().getWorkTree().toPath();
		Path relativeSubmodulePath = basePath.relativize(path.toPath());
		// NB: Git doesn't understand Windows path separators (\) in the .gitmodules file
		String unixStyleRelativeSubmodulePath = FilenameUtils.separatorsToUnix(relativeSubmodulePath.toString());

		try {
			SubmoduleAddCommand submoduleAddCommand = 
				new RelativeSubmoduleAddCommand(gitApi.getRepository())
					.setURI(uri)
					.setPath(unixStyleRelativeSubmodulePath);
			
			if (username != null) {
				submoduleAddCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					username, password
				));
			}

			Repository repository = submoduleAddCommand.call();
			repository.close();
			// TODO: pull commit up to have one commit for multiple submodules (e.g. add a corpus of documents)
			gitApi.commit().setMessage("Added submodule " + path.toString()).call(); //TODO: resource title instead of filepath
			gitApi
				.push()
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					username, password
				))
				.call(); //TODO: push might need a pull first in collaborative settings!
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to add submodule", e);
		}
	}

	/**
	 * Pushes commits made locally to the associated remote repository ('origin' remote).
	 *
	 * @param username the username to authenticate with
	 * @param password the password to authenticate with
	 * @throws IOException if the push operation failed
	 */
	@Override
	public void push(String username, String password) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `push` on a detached instance");
		}

		try {
			PushCommand pushCommand = this.gitApi.push();
			if (username != null) {
				pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					username, password
				));
			}
			pushCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to push", e);
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
	public void fetch(String username, String password) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `fetch` on a detached instance");
		}

		try {
			FetchCommand fetchCommand = this.gitApi.fetch();
			if (username != null) {
				fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					username, password
				));
			}
			fetchCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to fetch", e);
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
			CheckoutCommand checkoutCommand = this.gitApi.checkout();
			checkoutCommand.setCreateBranch(createBranch);
			checkoutCommand.setName(name).call();
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
	@Override
	public String getSubmoduleHeadRevisionHash(String submoduleName) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getSubmoduleHeadRevisionHash` on a detached instance");
		}

		try {
			SubmoduleStatusCommand submoduleStatusCommand = this.gitApi.submoduleStatus();
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

	@Override // AutoCloseable
	public void close() {
		if (this.gitApi != null) {
			this.gitApi.close();
			this.gitApi = null;
		}
	}

}
