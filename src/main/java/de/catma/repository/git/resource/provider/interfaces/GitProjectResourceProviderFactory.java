package de.catma.repository.git.resource.provider.interfaces;

import java.io.File;

import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import org.eclipse.jgit.transport.CredentialsProvider;

import de.catma.project.ProjectReference;

public interface GitProjectResourceProviderFactory {
	public GitProjectResourceProvider createResourceProvider(String projectId, ProjectReference projectReference,
															 File projectPath, LocalGitRepositoryManager localGitRepositoryManager,
															 RemoteGitManagerRestricted remoteGitServerManager, CredentialsProvider credentialsProvider);
}
