package de.catma.repository.git.managers.interfaces;

import de.catma.project.CommitInfo;
import de.catma.repository.git.managers.JGitCredentialsManager;
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
	File getRepositoryBasePath();

	File getRepositoryWorkTree();

	boolean isAttached();

	void detach();


	// methods that require the instance to be in a detached state
	void init(String group, String name, String description) throws IOException;

	String clone(String namespace, String projectId, String uri, JGitCredentialsManager jGitCredentialsManager) throws IOException;

	void open(String namespace, String name) throws IOException;


	// methods that require the instance to be in an attached state
	String getRemoteUrl(String remoteName);

	List<String> getRemoteBranches() throws IOException;


	String getRevisionHash() throws IOException;

	void fetch(JGitCredentialsManager jGitCredentialsManager) throws IOException;

	Set<String> getDeletedResourcesFromLog(Set<String> resourceIds, String resourceDir) throws IOException;

	boolean hasRef(String branch) throws IOException;

	void checkout(String name) throws IOException;

	void checkout(String name, boolean createBranch) throws IOException;

	Status getStatus() throws IOException;

	boolean hasUntrackedChanges() throws IOException;

	boolean hasUncommittedChanges() throws IOException;

	void add(Path relativePath) throws IOException;

	void add(File targetFile, byte[] bytes) throws IOException;

	String addAndCommit(File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail) throws IOException;

	String addAllAndCommit(String commitMsg, String committerName, String committerEmail, boolean force) throws IOException;

	void remove(File targetFile) throws IOException;

	String removeAndCommit(File targetFile, boolean removeEmptyParent, String commitMsg, String committerName, String committerEmail) throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean force) throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException;

	boolean canMerge(String branch) throws IOException;

	MergeResult merge(String branch) throws IOException;

	void abortMerge(MergeResult mergeResult) throws IOException;

	List<PushResult> push(JGitCredentialsManager jGitCredentialsManager) throws IOException;

	List<PushResult> pushMaster(JGitCredentialsManager jGitCredentialsManager) throws IOException;


	Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException;


	List<CommitInfo> getOurUnpublishedChanges() throws IOException;

	List<CommitInfo> getTheirPublishedChanges() throws IOException;


	// override close here because ours doesn't throw
	@Override
	void close();
}
