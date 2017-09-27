package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;

import javax.annotation.Nullable;
import java.io.File;

public interface ILocalGitRepositoryManager {
	boolean isAttached();

	File getRepositoryWorkTree();

	void init(String name, @Nullable String description) throws LocalGitRepositoryManagerException;

	String clone(String uri) throws LocalGitRepositoryManagerException;

	void open(String name) throws LocalGitRepositoryManagerException;

	void add(File targetFile, byte[] bytes) throws LocalGitRepositoryManagerException;

	void addAndCommit(File targetFile, byte[] bytes) throws LocalGitRepositoryManagerException;

	void commit(String message) throws LocalGitRepositoryManagerException;
}
