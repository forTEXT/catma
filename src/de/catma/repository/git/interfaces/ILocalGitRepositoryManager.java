package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;

import javax.annotation.Nullable;
import java.io.File;

public interface ILocalGitRepositoryManager extends AutoCloseable {
	boolean isAttached();

	void detach();

	String getRepositoryBasePath();

	File getRepositoryWorkTree();

	void init(String name, @Nullable String description) throws LocalGitRepositoryManagerException;

	String clone(String uri, @Nullable String username, @Nullable String password)
			throws LocalGitRepositoryManagerException;

	void open(String name) throws LocalGitRepositoryManagerException;

	void add(File targetFile, byte[] bytes) throws LocalGitRepositoryManagerException;

	void addAndCommit(File targetFile, byte[] bytes) throws LocalGitRepositoryManagerException;

	void commit(String message) throws LocalGitRepositoryManagerException;

	@Override
	void close();
}
