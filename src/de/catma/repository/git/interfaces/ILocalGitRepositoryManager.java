package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;

import javax.annotation.Nullable;
import java.io.File;

public interface ILocalGitRepositoryManager extends AutoCloseable {
	boolean isAttached();

	void detach();

	File getRepositoryWorkTree();

	String getRemoteUrl(@Nullable String remoteName);

	void init(String group, String name, @Nullable String description) throws LocalGitRepositoryManagerException;

	String clone(String group, String uri, @Nullable File path, @Nullable String username, @Nullable String password)
			throws LocalGitRepositoryManagerException;

	void open(String group, String name) throws LocalGitRepositoryManagerException;

	void add(File targetFile, byte[] bytes) throws LocalGitRepositoryManagerException;

	void addAndCommit(File targetFile, byte[] bytes, String committerName, String committerEmail)
			throws LocalGitRepositoryManagerException;

	void commit(String message, String committerName, String committerEmail) throws LocalGitRepositoryManagerException;

	void addSubmodule(File path, String uri, @Nullable String username, @Nullable String password)
			throws LocalGitRepositoryManagerException;

	void push(@Nullable String username, @Nullable String password) throws LocalGitRepositoryManagerException;

	void fetch(@Nullable String username, @Nullable String password) throws LocalGitRepositoryManagerException;

	void checkout(String name) throws LocalGitRepositoryManagerException;

	String getSubmoduleHeadRevisionHash(String submoduleName) throws LocalGitRepositoryManagerException;

	@Override
	void close();

	File getRepositoryBasePath();
}
