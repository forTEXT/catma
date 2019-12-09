package de.catma.repository.git.managers.jgitcommand;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.jgit.attributes.Attributes;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.merge.RecursiveMerger;
import org.eclipse.jgit.merge.StrategyRecursive;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

public class DeletedByThemWorkaroundStrategyRecursive extends StrategyRecursive {
	
	private static class ConflictedMergeResult extends MergeResult<RawText> {

		public ConflictedMergeResult() {
			super(Collections.<RawText>emptyList());
			setContainsConflicts(true);
		}
		
	}
	
	private static class DeletedByThemWorkaroundMerger extends RecursiveMerger {

		public DeletedByThemWorkaroundMerger(Repository local) {
			super(local);
		}
		
		@Override
		protected boolean processEntry(CanonicalTreeParser base, CanonicalTreeParser ours, CanonicalTreeParser theirs,
				DirCacheBuildIterator index, WorkingTreeIterator work, boolean ignoreConflicts, Attributes attributes)
				throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
			enterSubtree = true;
			final int modeO = tw.getRawMode(T_OURS);
			final int modeT = tw.getRawMode(T_THEIRS);
			final int modeB = tw.getRawMode(T_BASE);
			DirCacheEntry ourDce = null;

			if (index == null || index.getDirCacheEntry() == null) {
				// create a fake DCE, but only if ours is valid. ours is kept only
				// in case it is valid, so a null ourDce is ok in all other cases.
				if (nonTree(modeO)) {
					ourDce = new DirCacheEntry(tw.getRawPath());
					ourDce.setObjectId(tw.getObjectId(T_OURS));
					ourDce.setFileMode(tw.getFileMode(T_OURS));
				}
			} else {
				ourDce = index.getDirCacheEntry();
			}
			
			// mpetris: these are all cases that are handled by the super class
			// we're just interested in the last case where modeO != modeT
			// and all other cases are not true
			if (nonTree(modeO) && nonTree(modeT) && tw.idEqual(T_OURS, T_THEIRS)) {
				return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
			}
			
			if (modeB == modeT && tw.idEqual(T_BASE, T_THEIRS)) {
				return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
			}
			
			if (modeB == modeO && tw.idEqual(T_BASE, T_OURS)) {
				return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
			}

			if (tw.isSubtree()) {
				if (nonTree(modeO) && !nonTree(modeT)) {
					return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
				}
				if (nonTree(modeT) && !nonTree(modeO)) {
					return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
				}
				if (!nonTree(modeO))
					return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
			}
			
			if (nonTree(modeO) && nonTree(modeT)) {
				return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
			}
			else if (modeO != modeT) {
				// OURS or THEIRS has been deleted
				if (((modeO != 0 && !tw.idEqual(T_BASE, T_OURS)) || (modeT != 0 && !tw
						.idEqual(T_BASE, T_THEIRS)))) {
					
					// mpetris: when merging .gitsubmodules and descending into the submodules
					// this contentMerge fails because it tries to get the base of the submodule repo
					// from the container repo
					// since we're not interested in a content merge as either OURS or THEIRS
					// has been deleted anyway
					// we just generate the conflict and handle resolution later within the submodule
					
//					MergeResult<RawText> result = contentMerge(base, ours, theirs,
//							attributes);
					
					MergeResult<RawText> result = new ConflictedMergeResult();

					add(tw.getRawPath(), base, DirCacheEntry.STAGE_1, 0, 0);
					add(tw.getRawPath(), ours, DirCacheEntry.STAGE_2, 0, 0);
					DirCacheEntry e = add(tw.getRawPath(), theirs,
							DirCacheEntry.STAGE_3, 0, 0);

					// OURS was deleted checkout THEIRS
					if (modeO == 0) {
						// Check worktree before checking out THEIRS
						if (isWorktreeDirty(work, ourDce))
							return false;
						if (nonTree(modeT)) {
							if (e != null)
								toBeCheckedOut.put(tw.getPathString(), e);
						}
					}

					unmergedPaths.add(tw.getPathString());

					// generate a MergeResult for the deleted file
					mergeResults.put(tw.getPathString(), result);
					
					return true;
				}
			}

			return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
		}
		
		/*
		 * copied from super class
		 */
		private boolean nonTree(final int mode) {
			return mode != 0 && !FileMode.TREE.equals(mode);
		}
		
		/*
		 * copied from super class
		 */
		private DirCacheEntry add(byte[] path, CanonicalTreeParser p, int stage,
				long lastMod, long len) {
			if (p != null && !p.getEntryFileMode().equals(FileMode.TREE)) {
				DirCacheEntry e = new DirCacheEntry(path, stage);
				e.setFileMode(p.getEntryFileMode());
				e.setObjectId(p.getEntryObjectId());
				e.setLastModified(lastMod);
				e.setLength(len);
				builder.add(e);
				return e;
			}
			return null;
		}
		
		/*
		 * copied from super class
		 */
		private boolean isWorktreeDirty(WorkingTreeIterator work,
				DirCacheEntry ourDce) throws IOException {
			if (work == null)
				return false;

			final int modeF = tw.getRawMode(T_FILE);
			final int modeO = tw.getRawMode(T_OURS);

			// Worktree entry has to match ours to be considered clean
			boolean isDirty;
			if (ourDce != null)
				isDirty = work.isModified(ourDce, true, reader);
			else {
				isDirty = work.isModeDifferent(modeO);
				if (!isDirty && nonTree(modeF))
					isDirty = !tw.idEqual(T_FILE, T_OURS);
			}

			// Ignore existing empty directories
			if (isDirty && modeF == FileMode.TYPE_TREE
					&& modeO == FileMode.TYPE_MISSING)
				isDirty = false;
			if (isDirty)
				failingPaths.put(tw.getPathString(),
						MergeFailureReason.DIRTY_WORKTREE);
			return isDirty;
		}
		
	}
	

	public DeletedByThemWorkaroundStrategyRecursive() {
		super();
	}

	@Override
	public ThreeWayMerger newMerger(Repository db) {
		return new DeletedByThemWorkaroundMerger(db);
	}
	
}
