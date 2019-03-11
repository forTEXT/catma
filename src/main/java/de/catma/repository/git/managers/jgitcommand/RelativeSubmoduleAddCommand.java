package de.catma.repository.git.managers.jgitcommand;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class RelativeSubmoduleAddCommand extends SubmoduleAddCommand {

	private String path;

	private String uri;

	private ProgressMonitor monitor;

	/**
	 * @param repo
	 */
	public RelativeSubmoduleAddCommand(final Repository repo) {
		super(repo);
	}

	/**
	 * Set repository-relative path of submodule
	 *
	 * @param path
	 *            (with <code>/</code> as separator)
	 * @return this command
	 */
	public SubmoduleAddCommand setPath(final String path) {
		this.path = path;
		return this;
	}

	/**
	 * Set URI to clone submodule from
	 *
	 * @param uri
	 * @return this command
	 */
	public SubmoduleAddCommand setURI(final String uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * The progress monitor associated with the clone operation. By default,
	 * this is set to <code>NullProgressMonitor</code>
	 *
	 * @see NullProgressMonitor
	 * @param monitor
	 * @return this command
	 */
	public SubmoduleAddCommand setProgressMonitor(final ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	/**
	 * Is the configured already a submodule in the index?
	 *
	 * @return true if submodule exists in index, false otherwise
	 * @throws IOException
	 */
	protected boolean submoduleExists() throws IOException {
		TreeFilter filter = PathFilter.create(path);
		try (SubmoduleWalk w = SubmoduleWalk.forIndex(repo)) {
			return w.setFilter(filter).next();
		}
	}

	/**
	 * Executes the {@code SubmoduleAddCommand}
	 *
	 * The {@code Repository} instance returned by this command needs to be
	 * closed by the caller to free resources held by the {@code Repository}
	 * instance. It is recommended to call this method as soon as you don't need
	 * a reference to this {@code Repository} instance anymore.
	 *
	 * @return the newly created {@link Repository}
	 * @throws GitAPIException
	 */
	@Override
	public Repository call() throws GitAPIException {
		checkCallable();
		if (path == null || path.length() == 0)
			throw new IllegalArgumentException(JGitText.get().pathNotConfigured);
		if (uri == null || uri.length() == 0)
			throw new IllegalArgumentException(JGitText.get().uriNotConfigured);

		try {
			if (submoduleExists())
				throw new JGitInternalException(MessageFormat.format(
						JGitText.get().submoduleExists, path));
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		final String resolvedUri;
		try {
			resolvedUri = SubmoduleWalk.getSubmoduleRemoteUrl(repo, uri);
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}
		// Clone submodule repository
		File moduleDirectory = SubmoduleWalk.getSubmoduleDirectory(repo, path);
		// mpetris: changed to use the RelativeCloneCommand 
		CloneCommand clone = new RelativeCloneCommand();
		configure(clone);
		clone.setDirectory(moduleDirectory);
		clone.setGitDir(new File(new File(repo.getDirectory(),
				Constants.MODULES), path));
		clone.setURI(resolvedUri);
		if (monitor != null)
			clone.setProgressMonitor(monitor);
		Repository subRepo = null;
		try (Git git = clone.call()) {
			subRepo = git.getRepository();
			subRepo.incrementOpen();
			subRepo.close();
		}

		// Save submodule URL to parent repository's config
		StoredConfig config = repo.getConfig();
		config.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION, path,
				ConfigConstants.CONFIG_KEY_URL, resolvedUri);
		try {
			config.save();
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		// Save path and URL to parent repository's .gitmodules file
		FileBasedConfig modulesConfig = new FileBasedConfig(new File(
				repo.getWorkTree(), Constants.DOT_GIT_MODULES), repo.getFS());
		try {
			modulesConfig.load();
			modulesConfig.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION,
					path, ConfigConstants.CONFIG_KEY_PATH, path);
			modulesConfig.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION,
					path, ConfigConstants.CONFIG_KEY_URL, uri);
			modulesConfig.save();
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		} catch (ConfigInvalidException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		AddCommand add = new AddCommand(repo);
		// Add .gitmodules file to parent repository's index
		add.addFilepattern(Constants.DOT_GIT_MODULES);
		// Add submodule directory to parent repository's index
		add.addFilepattern(path);
		try {
			add.call();
		} catch (NoFilepatternException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		return subRepo;
	}

}
