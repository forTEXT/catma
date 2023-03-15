package de.catma.repository.git.managers.interfaces;

import de.catma.project.CommitInfo;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.user.User;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.PushResult;

import java.io.File;
import java.io.IOException;
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
	File getUserRepositoryBasePath();

	/**
	 * Whether the instance is attached to a Git repository.
	 *
	 * @return true if attached, otherwise false
	 */
	boolean isAttached();

	/**
	 * Detaches the instance from the currently attached Git repository, if any. If the instance is currently attached,
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
	/**
	 * Clones a remote Git repository locally.
	 *
	 * @param namespace the namespace of the Git repository to clone (parent directory name)
	 * @param name the name of the Git repository to clone (directory name)
	 * @param uri the URI of the Git repository to clone
	 * @param jGitCredentialsManager a {@link JGitCredentialsManager} to use for authentication
	 * @return the resultant directory name (last path part)
	 * @throws IOException if an error occurs when cloning
	 */
	String clone(String namespace, String name, String uri, JGitCredentialsManager jGitCredentialsManager) throws IOException;

	/**
	 * Opens an existing Git repository.
	 *
	 * @param namespace the namespace of the Git repository to open (parent directory name)
	 * @param name the name of the Git repository to open (directory name)
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

	/**
	 * Gets all available remote branches.
	 *
	 * @return a list of remote branch names
	 * @throws IOException if an error occurs when getting the remote branches
	 */
	List<String> getRemoteBranches() throws IOException;


	/**
	 * Gets the HEAD revision hash.
	 *
	 * @return the hash
	 * @throws IOException if an error occurs when getting the HEAD revision hash
	 */
	String getRevisionHash() throws IOException;

	/**
	 * Fetches refs from the associated remote repository ('origin' remote).
	 *
	 * @param jGitCredentialsManager a {@link JGitCredentialsManager} to use for authentication
	 * @throws IOException if an error occurs when fetching
	 */
	void fetch(JGitCredentialsManager jGitCredentialsManager) throws IOException;

	/**
	 * Searches the Git log of the user branch for commits that affect <code>resourceDir</code> and
	 * whose message indicates that one of the resources in <code>resourceIds</code> was deleted.
	 *
	 * @param resourceDir the name of the directory corresponding to the type of resource
	 * @param resourceTypeKeywords a keyword, or keywords, that must appear after "deleted " at the beginning
	 *                             of the commit message (case-insensitive)
	 * @param resourceIds a {@link Set} of string resource IDs
	 * @return a {@link Set} of those resource IDs that were verifiably deleted according to the log
	 * @throws IOException if an error occurs when verifying deleted resources
	 */
	Set<String> verifyDeletedResourcesViaLog(String resourceDir, String resourceTypeKeywords, Set<String> resourceIds) throws IOException;

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

	/**
	 * Gets the Git status.
	 *
	 * @return the {@link Status}
	 * @throws IOException if an error occurs when getting the status
	 */
	Status getStatus() throws IOException;

	/**
	 * Whether there are any untracked changes.
	 *
	 * @return true if there are untracked changes, otherwise false
	 * @throws IOException if an error occurs when checking for untracked changes
	 */
	boolean hasUntrackedChanges() throws IOException;

	/**
	 * Whether there are any uncommitted changes.
	 *
	 * @return true if there are uncommitted changes, otherwise false
	 * @throws IOException if an error occurs when checking for uncommitted changes
	 */
	boolean hasUncommittedChanges() throws IOException;

	/**
	 * Adds the given file or directory.
	 *
	 * @param relativeTargetFile a {@link File} representing the relative file/directory path to add
	 * @throws IOException if an error occurs when adding
	 */
	void add(File relativeTargetFile) throws IOException;

	/**
	 * Writes a new file with contents <code>bytes</code> to disk at path <code>targetFile</code>
	 * and adds it.
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
	 * adds it and commits.
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

	/**
	 * Automatically stages all new, modified and deleted files and then commits.
	 *
	 * @param commitMsg the commit message
	 * @param committerName the name of the committer
	 * @param committerEmail the email address of the committer
	 * @param force whether to create a commit even when there are no uncommitted changes
	 * @return the revision hash of the new commit
	 * @throws IOException if an error occurs when adding or committing
	 */
	String addAllAndCommit(String commitMsg, String committerName, String committerEmail, boolean force) throws IOException;

	/**
	 * Removes the given file or directory.
	 *
	 * @param targetFile a {@link File} representing the file/directory to remove
	 * @throws IOException if an error occurs when removing
	 */
	void remove(File targetFile) throws IOException;

	/**
	 * Removes a file or directory and commits.
	 *
	 * @param targetFile a {@link File} representing the file/directory to remove
	 * @param removeEmptyParent whether to delete the parent directory, if it is empty after the remove operation
	 * @param commitMsg the commit message
	 * @param committerName the name of the committer
	 * @param committerEmail the email address of the committer
	 * @return the revision hash of the new commit
	 * @throws IOException if an error occurs when removing or committing
	 */
	String removeAndCommit(File targetFile, boolean removeEmptyParent, String commitMsg, String committerName, String committerEmail) throws IOException;

	/**
	 * Commits pending changes.
	 *
	 * @param message the commit message
	 * @param committerName the name of the committer
	 * @param committerEmail the email address of the committer
	 * @param force whether to create a commit even when there are no uncommitted changes
	 * @return the revision hash of the new commit
	 * @throws IOException if an error occurs when committing
	 */
	String commit(String message, String committerName, String committerEmail, boolean force) throws IOException;

	/**
	 * Commits pending changes.
	 *
	 * @param message the commit message
	 * @param committerName the name of the committer
	 * @param committerEmail the email address of the committer
	 * @param all if true, modified and deleted files are automatically staged
	 * @param force whether to create a commit even when there are no uncommitted changes
	 * @return the revision hash of the new commit
	 * @throws IOException if an error occurs when committing
	 */
	String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException;

	/**
	 * Checks whether the given branch can be merged into the user branch.
	 *
	 * @param branch the name of the branch to merge
	 * @return true if the merge can be completed without conflicts, otherwise false
	 * @throws IOException if an error occurs when checking whether the merge is possible
	 */
	boolean canMerge(String branch) throws IOException;

	/**
	 * Merges the given branch into the user branch.
	 *
	 * @param branch the name of the branch to merge
	 * @return the {@link MergeResult}
	 * @throws IOException if an error occurs when merging
	 */
	MergeResult merge(String branch) throws IOException;

	/**
	 * Aborts an in-progress merge.
	 * <p>
	 * NB: This performs a hard reset! Commit pending changes before attempting a merge.
	 *
	 * @throws IOException if an error occurs when aborting the merge
	 */
	void abortMerge() throws IOException;

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


	/**
	 * Compares the user branch to the given branch and returns all paths with differences, ignoring deletes.
	 *
	 * @param otherBranchName the name of the branch to compare to the user branch
	 * @return a {@link Set} of all paths that differ between the two branches
	 * @throws IOException if an error occurs when comparing the branches
	 */
	Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException;


	/**
	 * Gets a list of all commits from the user branch that have not been merged into origin/master.
	 * <p>
	 * Inverse of {@link #getTheirPublishedChanges()}.
	 *
	 * @return a {@link List<CommitInfo>}
	 * @throws IOException if an error occurs when getting the unpublished commits
	 */
	List<CommitInfo> getOurUnpublishedChanges() throws IOException;

	/**
	 * Gets a list of all commits from origin/master that have not been merged into the user branch.
	 * <p>
	 * Inverse of {@link #getOurUnpublishedChanges()}.
	 *
	 * @return a {@link List<CommitInfo>}
	 * @throws IOException if an error occurs when getting the published commits
	 */
	List<CommitInfo> getTheirPublishedChanges() throws IOException;


	// override close here because ours doesn't throw
	@Override
	void close();
}
