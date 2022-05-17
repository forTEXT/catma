package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.submodule.SubmoduleValidator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.submodule.SubmoduleWalk;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;

public class RelativeSubmoduleAddCommand extends SubmoduleAddCommand {
	public static class RelativeSubmoduleAddCommandReflectionException extends GitAPIException {
		public RelativeSubmoduleAddCommandReflectionException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public RelativeSubmoduleAddCommand(final Repository repo) {
		super(repo);
	}

	// copied from superclass and modified to use RelativeCloneCommand
	@Override
	public Repository call() throws GitAPIException {
		checkCallable();
		String path = getPath();
		if (path == null || path.length() == 0)
			throw new IllegalArgumentException(JGitText.get().pathNotConfigured);
		String uri = getUri();
		if (uri == null || uri.length() == 0)
			throw new IllegalArgumentException(JGitText.get().uriNotConfigured);

		try {
			SubmoduleValidator.assertValidSubmoduleName(path);
			SubmoduleValidator.assertValidSubmodulePath(path);
			SubmoduleValidator.assertValidSubmoduleUri(uri);
		} catch (SubmoduleValidator.SubmoduleValidationException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

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
		CloneCommand clone = new RelativeCloneCommand();
		configure(clone);
		clone.setDirectory(moduleDirectory);
		clone.setGitDir(new File(new File(repo.getDirectory(),
				Constants.MODULES), path));
		clone.setURI(resolvedUri);
		ProgressMonitor monitor = getMonitor();
		if (monitor != null)
			clone.setProgressMonitor(monitor);
		Repository subRepo = null;
		try (Git git = clone.call()) {
			subRepo = git.getRepository();
			subRepo.incrementOpen();
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

	// reflective private field getters
	private String getPath() throws RelativeSubmoduleAddCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("path");
			field.setAccessible(true);
			return (String) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleAddCommandReflectionException("getPath", e);
		}
	}

	private String getUri() throws RelativeSubmoduleAddCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("uri");
			field.setAccessible(true);
			return (String) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleAddCommandReflectionException("getUri", e);
		}
	}

	private ProgressMonitor getMonitor() throws RelativeSubmoduleAddCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("monitor");
			field.setAccessible(true);
			return (ProgressMonitor) field.get(this);
		} catch (Exception e) {
			throw new RelativeSubmoduleAddCommandReflectionException("getMonitor", e);
		}
	}
}
