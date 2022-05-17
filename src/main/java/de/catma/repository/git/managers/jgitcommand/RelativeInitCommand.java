package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class RelativeInitCommand extends InitCommand {
	public static class RelativeInitCommandReflectionException extends GitAPIException {
		public RelativeInitCommandReflectionException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public RelativeInitCommand() {
		super();
	}

	// copied from superclass and modified to use RelativeFileRepository
	@Override
	public Git call() throws GitAPIException {
		try {
			RepositoryBuilder builder = new RepositoryBuilder();
			boolean bare = getBare();
			if (bare)
				builder.setBare();
			FS fs = getFS();
			if (fs != null) {
				builder.setFS(fs);
			}
			builder.readEnvironment();
			File gitDir = getGitDir();
			if (gitDir != null)
				builder.setGitDir(gitDir);
			else {
				gitDir = builder.getGitDir();
				setGitDir(gitDir);
			}
			File directory = getDirectory();
			if (directory != null) {
				if (bare)
					builder.setGitDir(directory);
				else {
					builder.setWorkTree(directory);
					if (gitDir == null)
						builder.setGitDir(new File(directory, Constants.DOT_GIT));
				}
			} else if (builder.getGitDir() == null) {
				String dStr = SystemReader.getInstance()
						.getProperty("user.dir"); //$NON-NLS-1$
				if (dStr == null)
					dStr = "."; //$NON-NLS-1$
				File d = new File(dStr);
				if (!bare)
					d = new File(d, Constants.DOT_GIT);
				builder.setGitDir(d);
			} else {
				// directory was not set but gitDir was set
				if (!bare) {
					String dStr = SystemReader.getInstance().getProperty(
							"user.dir"); //$NON-NLS-1$
					if (dStr == null)
						dStr = "."; //$NON-NLS-1$
					builder.setWorkTree(new File(dStr));
				}
			}
			Repository repository = new RelativeFileRepository(builder.setup());
			if (!repository.getObjectDatabase().exists())
				repository.create(bare);
			return new Git(repository);
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}
	}

	// reflective private field getters
	private boolean getBare() throws RelativeInitCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("bare");
			field.setAccessible(true);
			return field.getBoolean(this);
		}
		catch (Exception e) {
			throw new RelativeInitCommandReflectionException("getBare", e);
		}
	}

	private FS getFS() throws RelativeInitCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("fs");
			field.setAccessible(true);
			return (FS) field.get(this);
		}
		catch (Exception e) {
			throw new RelativeInitCommandReflectionException("getFS", e);
		}
	}

	private File getDirectory() throws RelativeInitCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("directory");
			field.setAccessible(true);
			return (File) field.get(this);
		}
		catch (Exception e) {
			throw new RelativeInitCommandReflectionException("getDirectory", e);
		}
	}

	private File getGitDir() throws RelativeInitCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("gitDir");
			field.setAccessible(true);
			return (File) field.get(this);
		}
		catch (Exception e) {
			throw new RelativeInitCommandReflectionException("getGitDir", e);
		}
	}
}
