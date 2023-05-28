package de.catma.repository.git.managers.jgit;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.lib.Repository;

public class RelativeJGitCommandFactory implements JGitCommandFactory {
	@Override
	public CloneCommand newCloneCommand() {
		return new RelativeCloneCommand();
	}

	@Override
	public SubmoduleAddCommand newSubmoduleAddCommand(Repository repository) {
		return new RelativeSubmoduleAddCommand(repository);
	}

	@Override
	public SubmoduleUpdateCommand newSubmoduleUpdateCommand(Repository repository) {
		return new RelativeSubmoduleUpdateCommand(repository);
	}
}
