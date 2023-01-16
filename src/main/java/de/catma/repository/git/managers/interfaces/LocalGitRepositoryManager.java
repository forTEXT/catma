package de.catma.repository.git.managers.interfaces;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.PushResult;

import de.catma.project.CommitInfo;
import de.catma.repository.git.managers.JGitCredentialsManager;

public interface LocalGitRepositoryManager extends AutoCloseable {
	static String DEFAULT_LOCAL_DEV_BRANCH = "dev";
	static String NO_COMMITS_YET = "no_commits_yet";
	
	boolean isAttached();

	void detach();

	File getRepositoryWorkTree();

	String getRemoteUrl(String remoteName);

	void init(String group, String name, String description) throws IOException;

	String clone(
			String namespace, String projectId, 
			String uri, JGitCredentialsManager jGitCredentialsManager) throws IOException;

	void open(String namespace, String name) throws IOException;

	void add(File targetFile, byte[] bytes) throws IOException;
	
	void add(Path relativePath) throws IOException;

	String addAndCommit(File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail)
			throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean force) throws IOException;

	List<PushResult> push(JGitCredentialsManager jGitCredentialsManager) throws IOException;
	List<PushResult> pushMaster(JGitCredentialsManager jGitCredentialsManager) throws IOException;
	
	void fetch(JGitCredentialsManager jGitCredentialsManager) throws IOException;

	void checkout(String name) throws IOException;
	void checkout(String name, boolean createBranch) throws IOException;

	@Override
	void close();

	File getRepositoryBasePath();


	String getRevisionHash() throws IOException;

	void remove(File targetFile) throws IOException;

	String removeAndCommit(File targetFile, boolean removeEmptyParent, String commitMsg, String committerName, String committerEmail) throws IOException;

	String getRevisionHash(String submodule) throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException;

	boolean hasUncommitedChanges() throws IOException;

	String addAllAndCommit(String commitMsg, String committerName, String committerEmail, boolean force) throws IOException;

	MergeResult merge(String branch) throws IOException;
	
	boolean canMerge(String branch) throws IOException;

	void rebase(String branch) throws IOException;

	boolean hasUntrackedChanges() throws IOException;

	Status getStatus() throws IOException;

	boolean hasRef(String branch) throws IOException;

	List<CommitInfo> getUnsynchronizedChanges() throws Exception;

	Set<String> getDeletedResourcesFromLog(Set<String> resourceIds, String resourceDir) throws IOException;

	void abortMerge(MergeResult mergeResult) throws IOException;

	Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException;

	List<String> getRemoteBranches() throws IOException;

	List<CommitInfo> getTheirPublishedChanges() throws IOException;

	List<CommitInfo> getOurUnpublishedChanges() throws IOException;


}
