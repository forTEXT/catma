package de.catma.repository.git.managers;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.lib.Repository;

public class DefaultJGitFactory implements JGitFactory {

	@Override
	public CloneCommand newCloneCommand() {
		return Git.cloneRepository();
	}

	@Override
	public SubmoduleAddCommand newSubmoduleAddCommand(Repository repository) {
		return new SubmoduleAddCommand(repository);
	}

	@Override
	public SubmoduleUpdateCommand newSubmoduleUpdateCommand(Repository repository) {
		return new SubmoduleUpdateCommand(repository);
	}

}
