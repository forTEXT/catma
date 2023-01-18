package de.catma.repository.git.managers.interfaces;

import de.catma.project.CommitInfo;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.user.User;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.PushResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface LocalGitRepositoryManager extends AutoCloseable {
	String DEFAULT_LOCAL_DEV_BRANCH = "dev";
	String NO_COMMITS_YET = "no_commits_yet";


	// methods that can always be called, irrespective of the instance state
	/**
	 * Gets the repository base path for the instance, which is specific to the {@link User} supplied at instantiation.
	 *
	 * @return a {@link File}
	 */
	File getRepositoryBasePath();

	/**
	 * Whether the instance is attached to a Git repository.
	 *
	 * @return true if attached, otherwise false
	 */
	boolean isAttached();

	/**
	 * Detach the instance from the currently attached Git repository, if any. If the instance is currently attached,
	 * this will allow you to re-use it to make calls to methods that require it to be detached (<code>clone</code> & <code>open</code>).
	 * <p>
	 * Whenever possible, you should use a try-with-resources statement instead, which will do this for you automatically.
	 * <p>
	 * Calling this method or using a try-with-resources statement will cause {@link #close()} to be called.
	 *
	 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">The try-with-resources Statement</a>
	 */
	void detach();


	// methods that require the instance to be in a detached state
	String clone(String namespace, String projectId, String uri, JGitCredentialsManager jGitCredentialsManager) throws IOException;

	/**
	 * Opens an existing Git repository.
	 *
	 * @param namespace the GitLab namespace of the Git repository to open (parent directory name)
	 * @param name the directory name of the Git repository to open
	 * @throws IOException if the Git repository couldn't be found or couldn't be opened for some other reason
	 */
	void open(String namespace, String name) throws IOException;


	// methods that require the instance to be in an attached state
	/**
	 * Gets the URL for the specified remote.
	 *
	 * @param remoteName the name of the remote for which the URL should be fetched.
	 *                   Defaults to 'origin' if not supplied.
	 * @return the remote URL
	 */
	String getRemoteUrl(String remoteName);

	List<String> getRemoteBranches() throws IOException;


	String getRevisionHash() throws IOException;

	/**
	 * Fetches refs from the associated remote repository ('origin' remote).
	 *
	 * @param jGitCredentialsManager a {@link JGitCredentialsManager} to use for authentication
	 * @throws IOException if an error occurs when fetching
	 */
	void fetch(JGitCredentialsManager jGitCredentialsManager) throws IOException;

	Set<String> getDeletedResourcesFromLog(Set<String> resourceIds, String resourceDir) throws IOException;

	/**
	 * Checks out a branch or commit.
	 *
	 * @param name the name of the branch or commit to check out
	 * @throws IOException if an error occurs when checking out
	 */
	void checkout(String name) throws IOException;

	/**
	 * Checks out a branch or commit.
	 *
	 * @param name the name of the branch or commit to check out
	 * @param createBranch whether to create the branch if it doesn't exist (like -b CLI option)
	 * @throws IOException if an error occurs when checking out
	 */
	void checkout(String name, boolean createBranch) throws IOException;

	Status getStatus() throws IOException;

	boolean hasUntrackedChanges() throws IOException;

	boolean hasUncommittedChanges() throws IOException;

	void add(Path relativePath) throws IOException;

	/**
	 * Writes a new file with contents <code>bytes</code> to disk at path <code>targetFile</code>
	 * and adds it to the attached Git repository.
	 * <p>
	 * It's the caller's responsibility to call {@link #commit(String, String, String, boolean)}.
	 * Alternatively, use {@link #addAndCommit(File, byte[], String, String, String)}.
	 *
	 * @param targetFile a {@link File} representing the target path
	 * @param bytes the file contents
	 * @throws IOException if an error occurs when adding
	 */
	void add(File targetFile, byte[] bytes) throws IOException;

	/**
	 * Writes a new file with contents <code>bytes</code> to disk at path <code>targetFile</code>,
	 * adds it to the attached Git repository and commits.
	 * <p>
	 * Calls {@link #add(File, byte[])} and {@link #commit(String, String, String, boolean)} internally.
	 *
	 * @param targetFile a {@link File} representing the target path
	 * @param bytes the file contents
	 * @param commitMsg the commit message
	 * @param committerName the name of the committer
	 * @param committerEmail the email address of the committer
	 * @return the revision hash of the new commit
	 * @throws IOException if an error occurs when adding or committing
	 */
	String addAndCommit(File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail) throws IOException;

	String addAllAndCommit(String commitMsg, String committerName, String committerEmail, boolean force) throws IOException;

	void remove(File targetFile) throws IOException;

	String removeAndCommit(File targetFile, boolean removeEmptyParent, String commitMsg, String committerName, String committerEmail) throws IOException;

	/**
	 * Commits pending changes to the attached Git repository.
	 *
	 * @param message the commit message
	 * @param committerName the name of the committer
	 * @param committerEmail the email address of the committer
	 * @param force whether to create a commit even when there are no uncommitted changes
	 * @return the revision hash of the new commit
	 * @throws IOException if an error occurs when committing
	 */
	String commit(String message, String committerName, String committerEmail, boolean force) throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException;

	boolean canMerge(String branch) throws IOException;

	MergeResult merge(String branch) throws IOException;

	void abortMerge(MergeResult mergeResult) throws IOException;

	/**
	 * Pushes commits made locally on the user branch to the associated remote ('origin') repository and branch.
	 *
	 * @param jGitCredentialsManager a {@link JGitCredentialsManager} to use for authentication
	 * @return a {@link List<PushResult>} containing the results of the push operation
	 * @throws IOException if an error occurs when pushing
	 */
	List<PushResult> push(JGitCredentialsManager jGitCredentialsManager) throws IOException;

	/**
	 * Pushes commits made locally on the master branch to the associated remote ('origin') repository and branch.
	 *
	 * @param jGitCredentialsManager a {@link JGitCredentialsManager} to use for authentication
	 * @return a {@link List<PushResult>} containing the results of the push operation
	 * @throws IOException if an error occurs when pushing
	 */
	List<PushResult> pushMaster(JGitCredentialsManager jGitCredentialsManager) throws IOException;


	Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException;


	List<CommitInfo> getOurUnpublishedChanges() throws IOException;

	List<CommitInfo> getTheirPublishedChanges() throws IOException;


	// override close here because ours doesn't throw
	@Override
	void close();
}
