package de.catma.repository.git.managers.jgit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidMergeHeadsException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

public class RelativeSubmoduleUpdateCommand extends SubmoduleUpdateCommand {
	
	//mpetris: added to workaround protected constructor
	private static class RelativeMergeCommand extends MergeCommand {

		RelativeMergeCommand(Repository repo) {
			super(repo);
		}
	}
	
	//mpetris: added to workaround protected constructor
	private static class RelativeRebaseCommand extends RebaseCommand {

		RelativeRebaseCommand(Repository repo) {
			super(repo);
		}
		
	}

	private ProgressMonitor monitor;

	private final Collection<String> paths;

	private MergeStrategy strategy = MergeStrategy.RECURSIVE;

	private CloneCommand.Callback callback;

	/**
	 * @param repo
	 */

	public RelativeSubmoduleUpdateCommand(final Repository repo) {
		super(repo);
		paths = new ArrayList<>();
	}

	/**
	 * The progress monitor associated with the clone operation. By default,
	 * this is set to <code>NullProgressMonitor</code>
	 *
	 * @see NullProgressMonitor
	 * @param monitor
	 * @return this command
	 */
	public SubmoduleUpdateCommand setProgressMonitor(
			final ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	/**
	 * Add repository-relative submodule path to initialize
	 *
	 * @param path
	 *            (with <code>/</code> as separator)
	 * @return this command
	 */
	public SubmoduleUpdateCommand addPath(final String path) {
		paths.add(path);
		return this;
	}

	/**
	 * Execute the SubmoduleUpdateCommand command.
	 *
	 * @return a collection of updated submodule paths
	 * @throws ConcurrentRefUpdateException
	 * @throws CheckoutConflictException
	 * @throws InvalidMergeHeadsException
	 * @throws InvalidConfigurationException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws RefNotFoundException
	 * @throws WrongRepositoryStateException
	 * @throws GitAPIException
	 */
	@Override
	public Collection<String> call() throws InvalidConfigurationException,
			NoHeadException, ConcurrentRefUpdateException,
			CheckoutConflictException, InvalidMergeHeadsException,
			WrongRepositoryStateException, NoMessageException, NoHeadException,
			RefNotFoundException, GitAPIException {
		checkCallable();

		try (SubmoduleWalk generator = SubmoduleWalk.forIndex(repo)) {
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

				Repository submoduleRepo = generator.getRepository();
				// Clone repository is not present
				if (submoduleRepo == null) {
					if (callback != null) {
						callback.cloningSubmodule(generator.getPath());
					}
					// mpetris: changed to use the RelativeCloneCommand 
					CloneCommand clone = new RelativeCloneCommand();
					configure(clone);
					clone.setURI(url);
					clone.setDirectory(generator.getDirectory());
					clone.setGitDir(new File(new File(repo.getDirectory(),
							Constants.MODULES), generator.getPath()));
					if (monitor != null)
						clone.setProgressMonitor(monitor);
					submoduleRepo = clone.call().getRepository();
				}

				try (RevWalk walk = new RevWalk(submoduleRepo)) {
					RevCommit commit = walk
							.parseCommit(generator.getObjectId());

					String update = generator.getConfigUpdate();
					if (ConfigConstants.CONFIG_KEY_MERGE.equals(update)) {
						MergeCommand merge = new RelativeMergeCommand(submoduleRepo); //mpetris: changed to workaround protected constructor
						merge.include(commit);
						merge.setProgressMonitor(monitor);
						merge.setStrategy(strategy);
						merge.call();
					} else if (ConfigConstants.CONFIG_KEY_REBASE.equals(update)) {
						RebaseCommand rebase = new RelativeRebaseCommand(submoduleRepo); //mpetris: changed to workaround protected constructor
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

	/**
	 * @param strategy
	 *            The merge strategy to use during this update operation.
	 * @return {@code this}
	 * @since 3.4
	 */
	public SubmoduleUpdateCommand setStrategy(MergeStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	/**
	 * Set status callback for submodule clone operation.
	 *
	 * @param callback
	 *            the callback
	 * @return {@code this}
	 * @since 4.8
	 */
	public SubmoduleUpdateCommand setCallback(CloneCommand.Callback callback) {
		this.callback = callback;
		return this;
	}	
}
