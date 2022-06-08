package de.catma.repository.git.interfaces;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.CredentialsProvider;

import de.catma.project.CommitInfo;

public interface ILocalGitRepositoryManager extends AutoCloseable {
	static String DEFAULT_LOCAL_DEV_BRANCH = "dev";
	static String NO_COMMITS_YET = "no_commits_yet";
	
	boolean isAttached();

	void detach();

	File getRepositoryWorkTree();

	String getRemoteUrl(String remoteName);

	void init(String group, String name, String description) throws IOException;

	String clone(
			String namespace, String projectId, 
			String uri, CredentialsProvider credentialsProvider) throws IOException;

	void open(String namespace, String name) throws IOException;

	void add(File targetFile, byte[] bytes) throws IOException;
	
	void add(Path relativePath) throws IOException;

	String addAndCommit(File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail)
			throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean force) throws IOException;

	void push(CredentialsProvider credentialsProvider) throws IOException;
	void push_master(CredentialsProvider credentialsProvider) throws IOException;
	
	void fetch(CredentialsProvider credentialsProvider) throws IOException;

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

	void revert(MergeResult mergeResult) throws IOException;

	Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException;

	List<String> getRemoteBranches() throws IOException;


}
