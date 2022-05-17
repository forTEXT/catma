package de.catma.repository.git.managers.jgitcommand;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RelativeCloneCommand extends CloneCommand {
	public static class RelativeCloneCommandReflectionException extends GitAPIException {
		public RelativeCloneCommandReflectionException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public RelativeCloneCommand() {
		super();
	}

	// copied from superclass and modified to use RelativeInitCommand
	private Repository init() throws GitAPIException {
		InitCommand command = new RelativeInitCommand();
		command.setBare(getBare());
		FS fs = getFS();
		if (fs != null) {
			command.setFs(fs);
		}
		File directory = getDirectory();
		if (directory != null) {
			command.setDirectory(directory);
		}
		File gitDir = getGitDir();
		if (gitDir != null) {
			command.setGitDir(gitDir);
		}
		return command.call().getRepository();
	}

	// reflective private field getters
	private boolean getBare() throws RelativeCloneCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("bare");
			field.setAccessible(true);
			return field.getBoolean(this);
		}
		catch (Exception e) {
			throw new RelativeCloneCommandReflectionException("getBare", e);
		}
	}

	private FS getFS() throws RelativeCloneCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("fs");
			field.setAccessible(true);
			return (FS) field.get(this);
		}
		catch (Exception e) {
			throw new RelativeCloneCommandReflectionException("getFS", e);
		}
	}

	private File getGitDir() throws RelativeCloneCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Field field = superclass.getDeclaredField("gitDir");
			field.setAccessible(true);
			return (File) field.get(this);
		}
		catch (Exception e) {
			throw new RelativeCloneCommandReflectionException("getGitDir", e);
		}
	}

	// reflective private method wrappers
	private File getDirectory() throws RelativeCloneCommandReflectionException {
		try {
			Class<?> superclass = getClass().getSuperclass();
			Method method = superclass.getDeclaredMethod("getDirectory");
			method.setAccessible(true);
			Object result = method.invoke(this);
			return result == null ? null : (File) result;
		}
		catch (Exception e) {
			throw new RelativeCloneCommandReflectionException("getDirectory", e);
		}
	}
}
