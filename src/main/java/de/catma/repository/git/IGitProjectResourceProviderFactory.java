package de.catma.repository.git;

import java.io.File;
import java.util.Set;

import org.eclipse.jgit.transport.CredentialsProvider;

import de.catma.project.ProjectReference;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;

public interface IGitProjectResourceProviderFactory {
	public IGitProjectResourceProvider createResourceProvider(String projectId, ProjectReference projectReference,
			File projectPath, ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager, CredentialsProvider credentialsProvider);
}
