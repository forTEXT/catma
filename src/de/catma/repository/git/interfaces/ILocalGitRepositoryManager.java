package de.catma.repository.git.interfaces;

import java.io.File;
import java.io.IOException;

public interface ILocalGitRepositoryManager extends AutoCloseable {
	boolean isAttached();

	void detach();

	File getRepositoryWorkTree();

	String getRemoteUrl(String remoteName);

	void init(String group, String name, String description) throws IOException;

	String clone(String group, String uri, File path, String username, String password)
			throws IOException;

	void open(String group, String name) throws IOException;

	void add(File targetFile, byte[] bytes) throws IOException;

	String addAndCommit(File targetFile, byte[] bytes, String committerName, String committerEmail)
			throws IOException;

	String commit(String message, String committerName, String committerEmail) throws IOException;

	void addSubmodule(File path, String uri, String username, String password)
			throws IOException;

	void push(String username, String password) throws IOException;

	void fetch(String username, String password) throws IOException;

	void checkout(String name) throws IOException;

	String getSubmoduleHeadRevisionHash(String submoduleName) throws IOException;

	@Override
	void close();

	File getRepositoryBasePath();

	String clone(String group, String uri, File path, String username, String password, boolean initSubmodules)
			throws IOException;

	String getRootRevisionHash() throws Exception;

	void remove(File targetFile) throws IOException;

	String removeAndCommit(File targetFile, String committerName, String committerEmail) throws IOException;
}
