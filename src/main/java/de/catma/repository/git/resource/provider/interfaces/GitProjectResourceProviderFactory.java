package de.catma.repository.git.resource.provider.interfaces;

import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;

public interface GitProjectResourceProviderFactory {
	GitProjectResourceProvider createResourceProvider(
			String projectId,
			ProjectReference projectReference,
			File projectPath,
			LocalGitRepositoryManager localGitRepositoryManager,
			RemoteGitManagerRestricted remoteGitServerManager,
			CredentialsProvider credentialsProvider
	);
}
