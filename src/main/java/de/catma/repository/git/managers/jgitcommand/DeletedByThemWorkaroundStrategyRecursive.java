package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.StrategyRecursive;
import org.eclipse.jgit.merge.ThreeWayMerger;

public class DeletedByThemWorkaroundStrategyRecursive extends StrategyRecursive {
	public DeletedByThemWorkaroundStrategyRecursive() {
		super();
	}

	@Override
	public ThreeWayMerger newMerger(Repository db) {
		return new DeletedByThemWorkaroundRecursiveMerger(db, false);
	}
}
