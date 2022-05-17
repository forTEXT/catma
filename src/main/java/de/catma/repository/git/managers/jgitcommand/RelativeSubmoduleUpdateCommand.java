package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RelativeSubmoduleUpdateCommand extends SubmoduleUpdateCommand {
	public static class RelativeSubmoduleUpdateCommandReflectionException extends GitAPIException {
		public RelativeSubmoduleUpdateCommandReflectionException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public RelativeSubmoduleUpdateCommand(final Repository repo) {
		super(repo);
	}

	// copied from superclass and modified to use RelativeCloneCommand, RelativeMergeCommand & RelativeRebaseCommand
	@Override
	public Collection<String> call() throws InvalidConfigurationException,
			NoHeadException, ConcurrentRefUpdateException,
			CheckoutConflictException, InvalidMergeHeadsException,
			WrongRepositoryStateException, NoMessageException, NoHeadException,
			RefNotFoundException, GitAPIException {
		checkCallable();

		try (SubmoduleWalk generator = SubmoduleWalk.forIndex(repo)) {
			Collection<String> paths = getPaths();
			if (!paths.isEmpty())
				generator.setFilter(PathFilterGroup.createFromStrings(paths));
			List<String> updated = new ArrayList<>();
			while (generator.next()) {
				// Skip submodules not registered in .gitmodules file
				if (generator.getModulesPath() == null)
					continue;
				// Skip submodules not registered in parent repository's config
				String url = generator.getConfigUrl();
				if (url == null)
					continue;

				CloneCommand.Callback callback = getCallback();
				ProgressMonitor monitor = getMonitor();

				Repository submoduleRepo = generator.getRepository();
				// Clone repository if not present
				if (submoduleRepo == null) {
					if (callback != null) {
						callback.cloningSubmodule(generator.getPath());
					}
					CloneCommand clone = new RelativeCloneCommand();
					configure(clone);
					clone.setURI(url);
					clone.setDirectory(generator.getDirectory());
					clone.setGitDir(new File(new File(repo.getDirectory(),
							Constants.MODULES), generator.getPath()));
					if (monitor != null)
						clone.setProgressMonitor(monitor);
					submoduleRepo = clone.call().getRepository();
				} else if (getFetch()) {
					FetchCommand.Callback fetchCallback = getFetchCallback();
					if (fetchCallback != null) {
						fetchCallback.fetchingSubmodule(generator.getPath());
					}
					FetchCommand fetchCommand = Git.wrap(submoduleRepo).fetch();
					if (monitor != null) {
						fetchCommand.setProgressMonitor(monitor);
					}
					configure(fetchCommand);
					fetchCommand.call();
				}

				try (RevWalk walk = new RevWalk(submoduleRepo)) {
					RevCommit commit = walk
							.parseCommit(generator.getObjectId());

					String update = generator.getConfigUpdate();
					MergeStrategy strategy = getStrategy();
					if (ConfigConstants.CONFIG_KEY_MERGE.equals(update)) {
						MergeCommand merge = new RelativeMergeCommand(submoduleRepo);
						merge.include(commit);
						merge.setProgressMonitor(monitor);
						merge.setStrategy(strategy);
						merge.call();
					} else if (ConfigConstants.CONFIG_KEY_REBASE.equals(update)) {
						RebaseCommand rebase = new RelativeRebaseCommand(submoduleRepo);
						rebase.setUpstream(commit);
						rebase.setProgressMonitor(monitor);
						rebase.setStrategy(strategy);
						rebase.call();
					} else {
						// Checkout commit referenced in parent repository's
						// index as a detached HEAD
						DirCacheCheckout co = new DirCacheCheckout(
								submoduleRepo, submoduleRepo.lockDirCache(),
								commit.getTree());
						co.setFailOnConflict(true);
						co.setProgressMonitor(monitor);
						co.checkout();
						RefUpdate refUpdate = submoduleRepo.updateRef(
								Constants.HEAD, true);
						refUpdate.setNewObjectId(commit);
						refUpdate.forceUpdate();
						if (callback != null) {
							callback.checkingOut(commit,
									generator.getPath());
						}
					}
				} finally {
					submoduleRepo.close();
				}
				updated.add(generator.getPath());
			}
			return updated;
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		} catch (ConfigInvalidException e) {
			throw new InvalidConfigurationException(e.getMessage(), e);
		}
	}

	// reflective private field getters
	private Collection<String> getPaths() throws RelativeSubmoduleUpdateCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("paths");
			field.setAccessible(true);
			return (Collection<String>) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleUpdateCommandReflectionException("getPaths", e);
		}
	}

	private CloneCommand.Callback getCallback() throws RelativeSubmoduleUpdateCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("callback");
			field.setAccessible(true);
			return (CloneCommand.Callback) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleUpdateCommandReflectionException("getCallback", e);
		}
	}

	private ProgressMonitor getMonitor() throws RelativeSubmoduleUpdateCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("monitor");
			field.setAccessible(true);
			return (ProgressMonitor) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleUpdateCommandReflectionException("getMonitor", e);
		}
	}

	private boolean getFetch() throws RelativeSubmoduleUpdateCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("fetch");
			field.setAccessible(true);
			return field.getBoolean(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleUpdateCommandReflectionException("getFetch", e);
		}
	}

	private FetchCommand.Callback getFetchCallback() throws RelativeSubmoduleUpdateCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("fetchCallback");
			field.setAccessible(true);
			return (FetchCommand.Callback) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleUpdateCommandReflectionException("getFetchCallback", e);
		}
	}

	private MergeStrategy getStrategy() throws RelativeSubmoduleUpdateCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("strategy");
			field.setAccessible(true);
			return (MergeStrategy) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleUpdateCommandReflectionException("getStrategy", e);
		}
	}
}
