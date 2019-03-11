package de.catma.repository.git.managers.jgitcommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.LockFile;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.CoreConfig.HideDotFiles;
import org.eclipse.jgit.lib.CoreConfig.SymLinks;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.SystemReader;

final class RelativeFileRepository extends FileRepository {

	public RelativeFileRepository(final BaseRepositoryBuilder<?,?> options) throws IOException {
		super(options);
	}

	public void create(boolean bare) throws IOException {
		final FileBasedConfig cfg = getConfig();
		if (cfg.getFile().exists()) {
			throw new IllegalStateException(MessageFormat.format(
					JGitText.get().repositoryAlreadyExists, getDirectory()));
		}
		org.eclipse.jgit.util.FileUtils.mkdirs(getDirectory(), true);
		HideDotFiles hideDotFiles = getConfig().getEnum(
				ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_HIDEDOTFILES,
				HideDotFiles.DOTGITONLY);
		if (hideDotFiles != HideDotFiles.FALSE && !isBare()
				&& getDirectory().getName().startsWith(".")) //$NON-NLS-1$
			getFS().setHidden(getDirectory(), true);
		this.getRefDatabase().create();
		this.getObjectDatabase().create();

		org.eclipse.jgit.util.FileUtils.mkdir(new File(getDirectory(), "branches")); //$NON-NLS-1$
		org.eclipse.jgit.util.FileUtils.mkdir(new File(getDirectory(), "hooks")); //$NON-NLS-1$

		RefUpdate head = updateRef(Constants.HEAD);
		head.disableRefLog();
		head.link(Constants.R_HEADS + Constants.MASTER);

		final boolean fileMode;
		if (getFS().supportsExecute()) {
			File tmp = File.createTempFile("try", "execute", getDirectory()); //$NON-NLS-1$ //$NON-NLS-2$

			getFS().setExecute(tmp, true);
			final boolean on = getFS().canExecute(tmp);

			getFS().setExecute(tmp, false);
			final boolean off = getFS().canExecute(tmp);
			org.eclipse.jgit.util.FileUtils.delete(tmp);

			fileMode = on && !off;
		} else {
			fileMode = false;
		}

		SymLinks symLinks = SymLinks.FALSE;
		if (getFS().supportsSymlinks()) {
			File tmp = new File(getDirectory(), "tmplink"); //$NON-NLS-1$
			try {
				getFS().createSymLink(tmp, "target"); //$NON-NLS-1$
				symLinks = null;
				org.eclipse.jgit.util.FileUtils.delete(tmp);
			} catch (IOException e) {
				// Normally a java.nio.file.FileSystemException
			}
		}
		if (symLinks != null)
			cfg.setString(ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_SYMLINKS, symLinks.name()
							.toLowerCase(Locale.ROOT));
		cfg.setInt(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_REPO_FORMAT_VERSION, 0);
		cfg.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_FILEMODE, fileMode);
		if (bare)
			cfg.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_BARE, true);
		cfg.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_LOGALLREFUPDATES, !bare);
		if (SystemReader.getInstance().isMacOS())
			// Java has no other way
			cfg.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_PRECOMPOSEUNICODE, true);
		if (!bare) {
			File workTree = getWorkTree();
			if (!getDirectory().getParentFile().equals(workTree)) {
				// mpetris: Git doesn't understand Windows path separators (\), we follow git clc style and use relative unix paths
				Path relativeWorkTreePath = getDirectory().toPath().relativize(workTree.toPath());
				String unixStyleRelativeWorkTreePath = FilenameUtils.separatorsToUnix(relativeWorkTreePath.toString());

				
				cfg.setString(ConfigConstants.CONFIG_CORE_SECTION, null,
						ConfigConstants.CONFIG_KEY_WORKTREE, 
						unixStyleRelativeWorkTreePath);
				LockFile dotGitLockFile = new LockFile(new File(workTree,
						Constants.DOT_GIT));
				try {
					if (dotGitLockFile.lock()) {
						// mpetris: Git doesn't understand Windows path separators (\), we follow git clc style and use relative unix paths
						Path relativeGitDirPath = workTree.toPath().relativize(getDirectory().toPath());
						String unixStyleRelativeGitDirPath = 
								FilenameUtils.separatorsToUnix(relativeGitDirPath.toString());
						
						dotGitLockFile.write(Constants.encode(Constants.GITDIR
								+ unixStyleRelativeGitDirPath));
						dotGitLockFile.commit();
					}
				} finally {
					dotGitLockFile.unlock();
				}
			}
		}
		cfg.save();
	}
}