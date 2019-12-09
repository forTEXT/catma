package de.catma.repository.git.interfaces;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface ILocalGitRepositoryManager extends AutoCloseable {
	static String DEFAULT_LOCAL_DEV_BRANCH = "dev";
	static String NO_COMMITS_YET = "no_commits_yet";
	
	boolean isAttached();

	void detach();

	File getRepositoryWorkTree();

	String getRemoteUrl(String remoteName);

	void init(String group, String name, String description) throws IOException;

	String clone(String group, String uri, File path, CredentialsProvider credentialsProvider)
			throws IOException;

	void open(String group, String name) throws IOException;

	void add(File targetFile, byte[] bytes) throws IOException;
	
	void add(Path relativePath) throws IOException;

	String addAndCommit(File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail)
			throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean force) throws IOException;

	void addSubmodule(File path, String uri, CredentialsProvider credentialsProvider)
			throws IOException;

	void push(CredentialsProvider credentialsProvider) throws IOException;

	void fetch(CredentialsProvider credentialsProvider) throws IOException;

	void checkout(String name) throws IOException;
	void checkout(String name, boolean createBranch) throws IOException;

	String getSubmoduleHeadRevisionHash(String submoduleName) throws IOException;

	@Override
	void close();

	File getRepositoryBasePath();

	String clone(String group, String uri, File path, CredentialsProvider credentialsProvider, boolean initSubmodules)
			throws IOException;

	String getRevisionHash() throws IOException;

	void remove(File targetFile) throws IOException;

	String removeAndCommit(File targetFile, boolean removeEmptyParent, String commitMsg, String committerName, String committerEmail) throws IOException;

	String getRevisionHash(String submodule) throws IOException;

	String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException;
	String commitWithSubmodules(String message, String committerName, String committerEmail, Set<String> submodules)
			throws IOException;

	boolean hasUncommitedChanges() throws IOException;
	boolean hasUncommitedChangesWithSubmodules(Set<String> submodules) throws IOException;

	String addAllAndCommit(String commitMsg, String committerName, String committerEmail, boolean force) throws IOException;

	String removeSubmodule(File submodulePath, String commitMsg, String committerName, String committerEmail) throws IOException;

	MergeResult merge(String branch) throws IOException;

	void rebase(String branch) throws IOException;

	boolean hasUntrackedChanges() throws IOException;

	Status getStatus() throws IOException;

	void resolveRootConflicts(CredentialsProvider credentialsProvider) throws IOException;

	void initAndUpdateSubmodules(CredentialsProvider credentialsProvider, Set<String> submodules) throws Exception;

	List<String> getSubmodulePaths() throws IOException;

	boolean hasRef(String branch) throws IOException;

	Status getStatus(boolean ignoreSubmodules) throws IOException;

	void resolveGitSubmoduleFileConflicts() throws IOException;

	MergeResult mergeWithDeletedByThemWorkaroundStrategyRecursive(String branch) throws IOException;


}
