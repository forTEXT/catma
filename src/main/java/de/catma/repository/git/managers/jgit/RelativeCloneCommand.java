package de.catma.repository.git.managers.jgit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.SubmoduleInitCommand;
import org.eclipse.jgit.api.SubmoduleUpdateCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;

public class RelativeCloneCommand extends CloneCommand {

	private String uri;

	private File directory;

	private File gitDir;

	private boolean bare;

	private String remote = Constants.DEFAULT_REMOTE_NAME;

	private String branch = Constants.HEAD;

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	private boolean cloneAllBranches;

	private boolean cloneSubmodules;

	private boolean noCheckout;

	private Collection<String> branchesToClone;

	private Callback callback;

	private boolean directoryExistsInitially;

	private boolean gitDirExistsInitially;

	/**
	 * Create clone command with no repository set
	 */
	public RelativeCloneCommand() {
		super();
	}

	/**
	 * Executes the {@code Clone} command.
	 *
	 * The Git instance returned by this command needs to be closed by the
	 * caller to free resources held by the underlying {@link Repository}
	 * instance. It is recommended to call this method as soon as you don't need
	 * a reference to this {@link Git} instance and the underlying
	 * {@link Repository} instance anymore.
	 *
	 * @return the newly created {@code Git} object with associated repository
	 * @throws InvalidRemoteException
	 * @throws org.eclipse.jgit.api.errors.TransportException
	 * @throws GitAPIException
	 */
	@Override
	public Git call() throws GitAPIException, InvalidRemoteException,
			org.eclipse.jgit.api.errors.TransportException {
		URIish u = null;
		try {
			u = new URIish(uri);
			verifyDirectories(u);
		} catch (URISyntaxException e) {
			throw new InvalidRemoteException(
					MessageFormat.format(JGitText.get().invalidURL, uri));
		}
		Repository repository = null;
		FetchResult fetchResult = null;
		Thread cleanupHook = new Thread(() -> cleanup());
		Runtime.getRuntime().addShutdownHook(cleanupHook);
		try {
			repository = init();
			fetchResult = fetch(repository, u);
		} catch (IOException ioe) {
			if (repository != null) {
				repository.close();
			}
			cleanup();
			throw new JGitInternalException(ioe.getMessage(), ioe);
		} catch (URISyntaxException e) {
			if (repository != null) {
				repository.close();
			}
			cleanup();
			throw new InvalidRemoteException(MessageFormat.format(
					JGitText.get().invalidRemote, remote));
		} catch (GitAPIException | RuntimeException e) {
			if (repository != null) {
				repository.close();
			}
			cleanup();
			throw e;
		} finally {
			Runtime.getRuntime().removeShutdownHook(cleanupHook);
		}
		if (!noCheckout) {
			try {
				checkout(repository, fetchResult);
			} catch (IOException ioe) {
				repository.close();
				throw new JGitInternalException(ioe.getMessage(), ioe);
			} catch (GitAPIException | RuntimeException e) {
				repository.close();
				throw e;
			}
		}
		return new Git(repository);
	}

	private static boolean isNonEmptyDirectory(File dir) {
		if (dir != null && dir.exists()) {
			File[] files = dir.listFiles();
			return files != null && files.length != 0;
		}
		return false;
	}

	private void verifyDirectories(URIish u) {
		if (directory == null && gitDir == null) {
			directory = new File(u.getHumanishName(), Constants.DOT_GIT);
		}
		directoryExistsInitially = directory != null && directory.exists();
		gitDirExistsInitially = gitDir != null && gitDir.exists();
		validateDirs(directory, gitDir, bare);
		if (isNonEmptyDirectory(directory)) {
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().cloneNonEmptyDirectory, directory.getName()));
		}
		if (isNonEmptyDirectory(gitDir)) {
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().cloneNonEmptyDirectory, gitDir.getName()));
		}
	}

	private Repository init() throws GitAPIException {
		InitCommand command = new RelativeInitCommand(); //mpetris: changed to use RelativeInitCommand
		command.setBare(bare);
		if (directory != null) {
			command.setDirectory(directory);
		}
		if (gitDir != null) {
			command.setGitDir(gitDir);
		}
		return command.call().getRepository();
	}

	private FetchResult fetch(Repository clonedRepo, URIish u)
			throws URISyntaxException,
			org.eclipse.jgit.api.errors.TransportException, IOException,
			GitAPIException {
		// create the remote config and save it
		RemoteConfig config = new RemoteConfig(clonedRepo.getConfig(), remote);
		config.addURI(u);

		final String dst = (bare ? Constants.R_HEADS : Constants.R_REMOTES
				+ config.getName() + "/") + "*"; //$NON-NLS-1$//$NON-NLS-2$
		RefSpec refSpec = new RefSpec();
		refSpec = refSpec.setForceUpdate(true);
		refSpec = refSpec.setSourceDestination(Constants.R_HEADS + "*", dst); //$NON-NLS-1$

		config.addFetchRefSpec(refSpec);
		config.update(clonedRepo.getConfig());

		clonedRepo.getConfig().save();

		// run the fetch command
		FetchCommand command = new FetchCommand(clonedRepo) {};
		command.setRemote(remote);
		command.setProgressMonitor(monitor);
		command.setTagOpt(TagOpt.FETCH_TAGS);
		configure(command);

		List<RefSpec> specs = calculateRefSpecs(dst);
		command.setRefSpecs(specs);

		return command.call();
	}

	private List<RefSpec> calculateRefSpecs(final String dst) {
		RefSpec wcrs = new RefSpec();
		wcrs = wcrs.setForceUpdate(true);
		wcrs = wcrs.setSourceDestination(Constants.R_HEADS + "*", dst); //$NON-NLS-1$
		List<RefSpec> specs = new ArrayList<>();
		if (cloneAllBranches)
			specs.add(wcrs);
		else if (branchesToClone != null
				&& branchesToClone.size() > 0) {
			for (final String selectedRef : branchesToClone)
				if (wcrs.matchSource(selectedRef))
					specs.add(wcrs.expandFromSource(selectedRef));
		}
		return specs;
	}

	private void checkout(Repository clonedRepo, FetchResult result)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException, GitAPIException {

		Ref head = null;
		if (branch.equals(Constants.HEAD)) {
			Ref foundBranch = findBranchToCheckout(result);
			if (foundBranch != null)
				head = foundBranch;
		}
		if (head == null) {
			head = result.getAdvertisedRef(branch);
			if (head == null)
				head = result.getAdvertisedRef(Constants.R_HEADS + branch);
			if (head == null)
				head = result.getAdvertisedRef(Constants.R_TAGS + branch);
		}

		if (head == null || head.getObjectId() == null)
			return; // TODO throw exception?

		if (head.getName().startsWith(Constants.R_HEADS)) {
			final RefUpdate newHead = clonedRepo.updateRef(Constants.HEAD);
			newHead.disableRefLog();
			newHead.link(head.getName());
			addMergeConfig(clonedRepo, head);
		}

		final RevCommit commit = parseCommit(clonedRepo, head);

		boolean detached = !head.getName().startsWith(Constants.R_HEADS);
		RefUpdate u = clonedRepo.updateRef(Constants.HEAD, detached);
		u.setNewObjectId(commit.getId());
		u.forceUpdate();

		if (!bare) {
			DirCache dc = clonedRepo.lockDirCache();
			DirCacheCheckout co = new DirCacheCheckout(clonedRepo, dc,
					commit.getTree());
			co.checkout();
			if (cloneSubmodules)
				cloneSubmodules(clonedRepo);
		}
	}

	private void cloneSubmodules(Repository clonedRepo) throws IOException,
			GitAPIException {
		SubmoduleInitCommand init = new SubmoduleInitCommand(clonedRepo);
		Collection<String> submodules = init.call();
		if (submodules.isEmpty()) {
			return;
		}
		if (callback != null) {
			callback.initializedSubmodules(submodules);
		}

		SubmoduleUpdateCommand update = new SubmoduleUpdateCommand(clonedRepo);
		configure(update);
		update.setProgressMonitor(monitor);
		update.setCallback(callback);
		if (!update.call().isEmpty()) {
			SubmoduleWalk walk = SubmoduleWalk.forIndex(clonedRepo);
			while (walk.next()) {
				Repository subRepo = walk.getRepository();
				if (subRepo != null) {
					try {
						cloneSubmodules(subRepo);
					} finally {
						subRepo.close();
					}
				}
			}
		}
	}

	private Ref findBranchToCheckout(FetchResult result) {
		final Ref idHEAD = result.getAdvertisedRef(Constants.HEAD);
		ObjectId headId = idHEAD != null ? idHEAD.getObjectId() : null;
		if (headId == null) {
			return null;
		}

		Ref master = result.getAdvertisedRef(Constants.R_HEADS
				+ Constants.MASTER);
		ObjectId objectId = master != null ? master.getObjectId() : null;
		if (headId.equals(objectId)) {
			return master;
		}

		Ref foundBranch = null;
		for (final Ref r : result.getAdvertisedRefs()) {
			final String n = r.getName();
			if (!n.startsWith(Constants.R_HEADS))
				continue;
			if (headId.equals(r.getObjectId())) {
				foundBranch = r;
				break;
			}
		}
		return foundBranch;
	}

	private void addMergeConfig(Repository clonedRepo, Ref head)
			throws IOException {
		String branchName = Repository.shortenRefName(head.getName());
		clonedRepo.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_REMOTE, remote);
		clonedRepo.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_MERGE, head.getName());
		String autosetupRebase = clonedRepo.getConfig().getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, null,
				ConfigConstants.CONFIG_KEY_AUTOSETUPREBASE);
		if (ConfigConstants.CONFIG_KEY_ALWAYS.equals(autosetupRebase)
				|| ConfigConstants.CONFIG_KEY_REMOTE.equals(autosetupRebase))
			clonedRepo.getConfig().setEnum(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.REBASE);
		clonedRepo.getConfig().save();
	}

	private RevCommit parseCommit(final Repository clonedRepo, final Ref ref)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		final RevCommit commit;
		try (final RevWalk rw = new RevWalk(clonedRepo)) {
			commit = rw.parseCommit(ref.getObjectId());
		}
		return commit;
	}

	/**
	 * @param uri
	 *            the URI to clone from, or {@code null} to unset the URI.
	 *            The URI must be set before {@link #call} is called.
	 * @return this instance
	 */
	public CloneCommand setURI(String uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * The optional directory associated with the clone operation. If the
	 * directory isn't set, a name associated with the source uri will be used.
	 *
	 * @see URIish#getHumanishName()
	 *
	 * @param directory
	 *            the directory to clone to, or {@code null} if the directory
	 *            name should be taken from the source uri
	 * @return this instance
	 * @throws IllegalStateException
	 *             if the combination of directory, gitDir and bare is illegal.
	 *             E.g. if for a non-bare repository directory and gitDir point
	 *             to the same directory of if for a bare repository both
	 *             directory and gitDir are specified
	 */
	public CloneCommand setDirectory(File directory) {
		validateDirs(directory, gitDir, bare);
		this.directory = directory;
		return this;
	}

	/**
	 * @param gitDir
	 *            the repository meta directory, or {@code null} to choose one
	 *            automatically at clone time
	 * @return this instance
	 * @throws IllegalStateException
	 *             if the combination of directory, gitDir and bare is illegal.
	 *             E.g. if for a non-bare repository directory and gitDir point
	 *             to the same directory of if for a bare repository both
	 *             directory and gitDir are specified
	 * @since 3.6
	 */
	public CloneCommand setGitDir(File gitDir) {
		validateDirs(directory, gitDir, bare);
		this.gitDir = gitDir;
		return this;
	}

	/**
	 * @param bare
	 *            whether the cloned repository is bare or not
	 * @return this instance
	 * @throws IllegalStateException
	 *             if the combination of directory, gitDir and bare is illegal.
	 *             E.g. if for a non-bare repository directory and gitDir point
	 *             to the same directory of if for a bare repository both
	 *             directory and gitDir are specified
	 */
	public CloneCommand setBare(boolean bare) throws IllegalStateException {
		validateDirs(directory, gitDir, bare);
		this.bare = bare;
		return this;
	}

	/**
	 * The remote name used to keep track of the upstream repository for the
	 * clone operation. If no remote name is set, the default value of
	 * <code>Constants.DEFAULT_REMOTE_NAME</code> will be used.
	 *
	 * @see Constants#DEFAULT_REMOTE_NAME
	 * @param remote
	 *            name that keeps track of the upstream repository.
	 *            {@code null} means to use DEFAULT_REMOTE_NAME.
	 * @return this instance
	 */
	public CloneCommand setRemote(String remote) {
		if (remote == null) {
			remote = Constants.DEFAULT_REMOTE_NAME;
		}
		this.remote = remote;
		return this;
	}

	/**
	 * @param branch
	 *            the initial branch to check out when cloning the repository.
	 *            Can be specified as ref name (<code>refs/heads/master</code>),
	 *            branch name (<code>master</code>) or tag name (<code>v1.2.3</code>).
	 *            The default is to use the branch pointed to by the cloned
	 *            repository's HEAD and can be requested by passing {@code null}
	 *            or <code>HEAD</code>.
	 * @return this instance
	 */
	public CloneCommand setBranch(String branch) {
		if (branch == null) {
			branch = Constants.HEAD;
		}
		this.branch = branch;
		return this;
	}

	/**
	 * The progress monitor associated with the clone operation. By default,
	 * this is set to <code>NullProgressMonitor</code>
	 *
	 * @see NullProgressMonitor
	 *
	 * @param monitor
	 * @return {@code this}
	 */
	public CloneCommand setProgressMonitor(ProgressMonitor monitor) {
		if (monitor == null) {
			monitor = NullProgressMonitor.INSTANCE;
		}
		this.monitor = monitor;
		return this;
	}

	/**
	 * @param cloneAllBranches
	 *            true when all branches have to be fetched (indicates wildcard
	 *            in created fetch refspec), false otherwise.
	 * @return {@code this}
	 */
	public CloneCommand setCloneAllBranches(boolean cloneAllBranches) {
		this.cloneAllBranches = cloneAllBranches;
		return this;
	}

	/**
	 * @param cloneSubmodules
	 *            true to initialize and update submodules. Ignored when
	 *            {@link #setBare(boolean)} is set to true.
	 * @return {@code this}
	 */
	public CloneCommand setCloneSubmodules(boolean cloneSubmodules) {
		this.cloneSubmodules = cloneSubmodules;
		return this;
	}

	/**
	 * @param branchesToClone
	 *            collection of branches to clone. Ignored when allSelected is
	 *            true. Must be specified as full ref names (e.g.
	 *            <code>refs/heads/master</code>).
	 * @return {@code this}
	 */
	public CloneCommand setBranchesToClone(Collection<String> branchesToClone) {
		this.branchesToClone = branchesToClone;
		return this;
	}

	/**
	 * @param noCheckout
	 *            if set to <code>true</code> no branch will be checked out
	 *            after the clone. This enhances performance of the clone
	 *            command when there is no need for a checked out branch.
	 * @return {@code this}
	 */
	public CloneCommand setNoCheckout(boolean noCheckout) {
		this.noCheckout = noCheckout;
		return this;
	}

	/**
	 * Register a progress callback.
	 *
	 * @param callback
	 *            the callback
	 * @return {@code this}
	 * @since 4.8
	 */
	public CloneCommand setCallback(Callback callback) {
		this.callback = callback;
		return this;
	}

	private static void validateDirs(File directory, File gitDir, boolean bare)
			throws IllegalStateException {
		if (directory != null) {
			if (directory.exists() && !directory.isDirectory()) {
				throw new IllegalStateException(MessageFormat.format(
						JGitText.get().initFailedDirIsNoDirectory, directory));
			}
			if (gitDir != null && gitDir.exists() && !gitDir.isDirectory()) {
				throw new IllegalStateException(MessageFormat.format(
						JGitText.get().initFailedGitDirIsNoDirectory,
						gitDir));
			}
			if (bare) {
				if (gitDir != null && !gitDir.equals(directory))
					throw new IllegalStateException(MessageFormat.format(
							JGitText.get().initFailedBareRepoDifferentDirs,
							gitDir, directory));
			} else {
				if (gitDir != null && gitDir.equals(directory))
					throw new IllegalStateException(MessageFormat.format(
							JGitText.get().initFailedNonBareRepoSameDirs,
							gitDir, directory));
			}
		}
	}

	private void cleanup() {
		try {
			if (directory != null) {
				if (!directoryExistsInitially) {
					FileUtils.delete(directory, FileUtils.RECURSIVE
							| FileUtils.SKIP_MISSING | FileUtils.IGNORE_ERRORS);
				} else {
					deleteChildren(directory);
				}
			}
			if (gitDir != null) {
				if (!gitDirExistsInitially) {
					FileUtils.delete(gitDir, FileUtils.RECURSIVE
							| FileUtils.SKIP_MISSING | FileUtils.IGNORE_ERRORS);
				} else {
					deleteChildren(directory);
				}
			}
		} catch (IOException e) {
			// Ignore; this is a best-effort cleanup in error cases, and
			// IOException should not be raised anyway
		}
	}

	private void deleteChildren(File file) throws IOException {
		if (!FS.DETECTED.isDirectory(file)) {
			return;
		}
		for (File child : file.listFiles()) {
			FileUtils.delete(child, FileUtils.RECURSIVE | FileUtils.SKIP_MISSING
					| FileUtils.IGNORE_ERRORS);
		}
	}

}
