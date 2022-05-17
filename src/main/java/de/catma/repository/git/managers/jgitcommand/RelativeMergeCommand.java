package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.lib.Repository;

public class RelativeMergeCommand extends MergeCommand {
    public RelativeMergeCommand(Repository repo) {
        super(repo);
    }
}
