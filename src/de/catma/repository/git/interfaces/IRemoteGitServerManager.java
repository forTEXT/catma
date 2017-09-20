package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.RemoteGitServerManagerException;

import javax.annotation.Nullable;

public interface IRemoteGitServerManager {
	int createRepository(String name, @Nullable String path)
			throws RemoteGitServerManagerException;

	int createRepository(String name, @Nullable String path, int groupId)
			throws RemoteGitServerManagerException;

	void deleteRepository(int repositoryId) throws RemoteGitServerManagerException;

	int createGroup(String name, String path, @Nullable String description)
			throws RemoteGitServerManagerException;

	void deleteGroup(int groupId) throws RemoteGitServerManagerException;
}
