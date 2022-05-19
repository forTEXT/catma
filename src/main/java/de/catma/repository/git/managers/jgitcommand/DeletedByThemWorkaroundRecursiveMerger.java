package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.attributes.Attributes;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.merge.RecursiveMerger;
import org.eclipse.jgit.submodule.SubmoduleConflict;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static java.time.Instant.EPOCH;

public class DeletedByThemWorkaroundRecursiveMerger extends RecursiveMerger {
	private static class DeletedByThemWorkaroundRecursiveMergerReflectionException extends IOException {
		public DeletedByThemWorkaroundRecursiveMergerReflectionException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static class ConflictedMergeResult extends MergeResult<RawText> {
		public ConflictedMergeResult() {
			super(Collections.emptyList());
			super.setContainsConflicts(true);
		}

		// workaround for protected setContainsConflicts method on MergeResult
		@Override
		protected void setContainsConflicts(boolean containsConflicts) {
			super.setContainsConflicts(containsConflicts);
		}
	}

	// workaround for protected setContainsConflicts method on MergeResult
	private static class SubmoduleConflictMergeResult extends MergeResult<SubmoduleConflict> {
		public SubmoduleConflictMergeResult(List<SubmoduleConflict> sequences) {
			super(sequences);
		}

		@Override
		protected void setContainsConflicts(boolean containsConflicts) {
			super.setContainsConflicts(containsConflicts);
		}
	}

	protected DeletedByThemWorkaroundRecursiveMerger(Repository local, boolean inCore) {
		super(local, inCore);
	}

	@Override
	protected boolean processEntry(CanonicalTreeParser base,
			CanonicalTreeParser ours, CanonicalTreeParser theirs,
			DirCacheBuildIterator index, WorkingTreeIterator work,
			boolean ignoreConflicts, Attributes attributes)
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {
		enterSubtree = true;
		final int modeO = tw.getRawMode(T_OURS);
		final int modeT = tw.getRawMode(T_THEIRS);
		final int modeB = tw.getRawMode(T_BASE);
		boolean gitLinkMerging = isGitLink(modeO) || isGitLink(modeT)
				|| isGitLink(modeB);
		if (modeO == 0 && modeT == 0 && modeB == 0)
			// File is either untracked or new, staged but uncommitted
			return true;

		if (isIndexDirty())
			return false;

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
			// file/folder conflicts: here I want to detect only file/folder
			// conflict between ours and theirs. file/folder conflicts between
			// base/index/workingTree and something else are not relevant or
			// detected later
			if (nonTree(modeO) != nonTree(modeT)) {
				return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
			}

			// ours and theirs are both folders or both files (and treewalk
			// tells us we are in a subtree because of index or working-dir).
			// If they are both folders no content-merge is required - we can
			// return here.
			if (!nonTree(modeO))
				return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);

			// ours and theirs are both files, just fall out of the if block
			// and do the content merge
		}

		if (nonTree(modeO) && nonTree(modeT)) {
			return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
		} else if (modeO != modeT) {
			// OURS or THEIRS has been deleted
			if (((modeO != 0 && !tw.idEqual(T_BASE, T_OURS)) || (modeT != 0 && !tw
					.idEqual(T_BASE, T_THEIRS)))) {
				if (gitLinkMerging && ignoreConflicts) {
					add(tw.getRawPath(), ours, DirCacheEntry.STAGE_0, EPOCH, 0);
				} else if (gitLinkMerging) {
					add(tw.getRawPath(), base, DirCacheEntry.STAGE_1, EPOCH, 0);
					add(tw.getRawPath(), ours, DirCacheEntry.STAGE_2, EPOCH, 0);
					add(tw.getRawPath(), theirs, DirCacheEntry.STAGE_3, EPOCH, 0);
					SubmoduleConflictMergeResult result = createGitLinksMergeResult(
							base, ours, theirs);
					result.setContainsConflicts(true);
					mergeResults.put(tw.getPathString(), result);
					unmergedPaths.add(tw.getPathString());
				} else {
					// mpetris: when merging .gitmodules and descending into the submodules
					// this contentMerge fails because it tries to get the base of the submodule repo
					// from the container repo
					// since we're not interested in a content merge as either OURS or THEIRS
					// has been deleted anyway
					// we just generate the conflict and handle resolution later within the submodule

					// Content merge strategy does not apply to delete-modify
					// conflicts!
//					MergeResult<RawText> result;
//					try {
//						result = contentMerge(base, ours, theirs, attributes,
//								ContentMergeStrategy.CONFLICT);
//					} catch (BinaryBlobException e) {
//						result = new MergeResult<>(Collections.emptyList());
//						result.setContainsConflicts(true);
//					}

					ConflictedMergeResult result = new ConflictedMergeResult();

					if (ignoreConflicts) {
						// In case a conflict is detected the working tree file
						// is again filled with new content (containing conflict
						// markers). But also stage 0 of the index is filled
						// with that content.
						result.setContainsConflicts(false);
						updateIndex(base, ours, theirs, result, attributes);
					} else {
						add(tw.getRawPath(), base, DirCacheEntry.STAGE_1, EPOCH,
								0);
						add(tw.getRawPath(), ours, DirCacheEntry.STAGE_2, EPOCH,
								0);
						DirCacheEntry e = add(tw.getRawPath(), theirs,
								DirCacheEntry.STAGE_3, EPOCH, 0);

						// OURS was deleted checkout THEIRS
						if (modeO == 0) {
							// Check worktree before checking out THEIRS
							if (isWorktreeDirty(work, ourDce)) {
								return false;
							}
							if (nonTree(modeT)) {
								if (e != null) {
									addToCheckout(tw.getPathString(), e,
											attributes);
								}
							}
						}

						unmergedPaths.add(tw.getPathString());

						// generate a MergeResult for the deleted file
						mergeResults.put(tw.getPathString(), result);
					}

					return true;
				}
			}
		}
		return super.processEntry(base, ours, theirs, index, work, ignoreConflicts, attributes);
	}

	// reflective private method wrappers, note that all of these are defined on the superclass of the superclass
	private boolean isIndexDirty() throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod("isIndexDirty");
			method.setAccessible(true);
			return (boolean) method.invoke(this);
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("isIndexDirty", e);
		}
	}

	private static boolean nonTree(final int mode) throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = DeletedByThemWorkaroundRecursiveMerger.class.getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod("nonTree", int.class);
			method.setAccessible(true);
			return (boolean) method.invoke(null, mode);
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("nonTree", e);
		}
	}

	private DirCacheEntry add(byte[] path, CanonicalTreeParser p, int stage, Instant lastMod, long len)
			throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod("add", byte[].class, CanonicalTreeParser.class, int.class, Instant.class, long.class);
			method.setAccessible(true);
			Object result = method.invoke(this, path, p, stage, lastMod, len);
			return result == null ? null : (DirCacheEntry) result;
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("add", e);
		}
	}

	private boolean isWorktreeDirty(WorkingTreeIterator work, DirCacheEntry ourDce) throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod("isWorktreeDirty", WorkingTreeIterator.class, DirCacheEntry.class);
			method.setAccessible(true);
			return (boolean) method.invoke(this, work, ourDce);
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("isWorktreeDirty", e);
		}
	}

	private static boolean isGitLink(int mode) throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = DeletedByThemWorkaroundRecursiveMerger.class.getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod("isGitLink", int.class);
			method.setAccessible(true);
			return (boolean) method.invoke(null, mode);
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("isGitLink", e);
		}
	}

	private static SubmoduleConflictMergeResult createGitLinksMergeResult(CanonicalTreeParser base, CanonicalTreeParser ours, CanonicalTreeParser theirs)
			throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = DeletedByThemWorkaroundRecursiveMerger.class.getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod(
					"createGitLinksMergeResult", CanonicalTreeParser.class, CanonicalTreeParser.class, CanonicalTreeParser.class
			);
			method.setAccessible(true);
			return (SubmoduleConflictMergeResult) method.invoke(null, base, ours, theirs);
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("createGitLinksMergeResult", e);
		}
	}

	private void updateIndex(CanonicalTreeParser base, CanonicalTreeParser ours, CanonicalTreeParser theirs, MergeResult<RawText> result, Attributes attributes)
			throws DeletedByThemWorkaroundRecursiveMergerReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass().getSuperclass();
			Method method = superclass.getDeclaredMethod(
					"updateIndex", CanonicalTreeParser.class, CanonicalTreeParser.class, CanonicalTreeParser.class,
					MergeResult.class, Attributes.class
			);
			method.setAccessible(true);
			method.invoke(this, base, ours, theirs, result, attributes);
		}
		catch (Exception e) {
			throw new DeletedByThemWorkaroundRecursiveMergerReflectionException("updateIndex", e);
		}
	}
}
