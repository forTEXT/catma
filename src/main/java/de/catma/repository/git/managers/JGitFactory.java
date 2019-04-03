package de.catma.repository.git.managers;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.lib.Repository;

public interface JGitFactory {
	
	public CloneCommand newCloneCommand();
	public SubmoduleAddCommand newSubmoduleAddCommand(Repository repository);
	public SubmoduleUpdateCommand newSubmoduleUpdateCommand(Repository repository);
}
