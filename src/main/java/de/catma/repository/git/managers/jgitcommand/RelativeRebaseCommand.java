package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.lib.Repository;

public class RelativeRebaseCommand extends RebaseCommand {
    public RelativeRebaseCommand(Repository repo) {
        super(repo);
    }
}
