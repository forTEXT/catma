package de.catma.repository.git.managers.jgit;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.lib.Repository;

public interface JGitCommandFactory {
	CloneCommand newCloneCommand();
	SubmoduleAddCommand newSubmoduleAddCommand(Repository repository);
	SubmoduleUpdateCommand newSubmoduleUpdateCommand(Repository repository);
}
