package de.catma.repository.git.managers;

import com.google.common.collect.Lists;
import de.catma.project.CommitInfo;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.jgit.ClosableRecursiveMerger;
import de.catma.user.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JGitRepoManager implements LocalGitRepositoryManager, AutoCloseable {
	private final Logger logger = Logger.getLogger(JGitRepoManager.class.getName());

	private final String repositoryBasePath;
	private final String username;

	private Git gitApi;

	/**
	 * Creates a new instance of this class for the given {@link User}.
	 * <p>
	 * Note that the <code>user</code> argument is NOT used for authentication to remote Git servers. It is only
	 * used to organise repositories on the local file system, based on the user's identifier. Methods of this class
	 * that require authentication expect a {@link JGitCredentialsManager}.
	 *
	 * @param repositoryBasePath the base path for local repository storage
	 * @param user a {@link User}
	 */
	public JGitRepoManager(String repositoryBasePath, User user) {
		this.repositoryBasePath = repositoryBasePath;
		this.username = user.getIdentifier();
	}

	// methods that can always be called, irrespective of the instance state
	public String getUsername() {
		return username;
	}

	public Git getGitApi() {
		return gitApi;
	}

	@Override
	public File getUserRepositoryBasePath() {
		return Paths.get(new File(repositoryBasePath).toURI()).resolve(username).toFile();
	}

	@Override
	public boolean isAttached() {
		return gitApi != null;
	}

	@Override
	public void detach() {
		close();
	}


	// methods that require the instance to be in a detached state
	@Override
	public String clone(String namespace, String name, String uri, JGitCredentialsManager jGitCredentialsManager) throws IOException {
		return clone(namespace, name, uri, jGitCredentialsManager, 0, 0);
	}

	private String clone(
			String namespace,
			String name,
			String uri,
			JGitCredentialsManager jGitCredentialsManager,
			int refreshCredentialsTryCount,
			int tryCount
	) throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `clone` on an attached instance");
		}

		File targetPath = Paths.get(getUserRepositoryBasePath().toURI())
				.resolve(namespace)
				.resolve(name)
				.toFile();

		try {
			CloneCommand cloneCommand = new CloneCommand().setURI(uri).setDirectory(targetPath);
			cloneCommand.setCredentialsProvider(jGitCredentialsManager.getCredentialsProvider());
			gitApi = cloneCommand.call();
		}
		catch (GitAPIException e) {
			if (e instanceof TransportException && e.getMessage().contains("not authorized") && refreshCredentialsTryCount < 1) {
				// it's likely that the user is logged in using the username/password authentication method and that their
				// GitLab OAuth access token has expired - try to refresh credentials and retry the operation once
				detach();
				jGitCredentialsManager.refreshTransientCredentials();
				return clone(namespace, name, uri, jGitCredentialsManager, refreshCredentialsTryCount + 1, tryCount);
			}

			if (e instanceof TransportException && e.getMessage().contains("authentication not supported") && tryCount < 3) {
				// sometimes GitLab refuses to accept the clone and returns this error message
				// subsequent clone attempts succeed however, so we retry the clone up to 3 times before giving up
				detach();

				// delete the repo dir if it already exists, as that will prevent the recursive call from succeeding, throwing
				// org.eclipse.jgit.api.errors.JGitInternalException: Destination path "<path>" already exists and is not an empty directory
				if (targetPath.exists()) {
					FileUtils.deleteDirectory(targetPath);
				}

				try {
					Thread.sleep(100L * (tryCount + 1));
				}
				catch (InterruptedException ignored) {}

				return clone(namespace, name, uri, jGitCredentialsManager, refreshCredentialsTryCount, tryCount + 1);
			}

			// give up, refreshing credentials didn't work, retries exhausted, or unexpected error
			throw new IOException(
					String.format("Failed to clone, tried %d times", tryCount + 1),
					e
			);
		}

		return targetPath.getName();
	}

	@Override
	public void open(String namespace, String name) throws IOException {
		if (isAttached()) {
			throw new IllegalStateException("Can't call `open` on an attached instance");
		}

		File repositoryPath = Paths.get(getUserRepositoryBasePath().toURI())
				.resolve(namespace)
				.resolve(name)
				.toFile();

		File dotGitDirOrFile = new File(repositoryPath, ".git");

		if (!repositoryPath.exists() || !repositoryPath.isDirectory() || !dotGitDirOrFile.exists()) {
			throw new IOException(
					String.format("Couldn't find a Git repository at path %s.", repositoryPath)
			);
		}

		// handle opening of submodule repos
		if (dotGitDirOrFile.isFile()) {
			dotGitDirOrFile = Paths.get(repositoryPath.toURI()).resolve(
					FileUtils.readFileToString(dotGitDirOrFile, StandardCharsets.UTF_8)
							.replace("gitdir:", "")
							.trim()
			).normalize().toFile();
		}

		gitApi = Git.open(dotGitDirOrFile);
	}


	// methods that require the instance to be in an attached state
	@Override
	public String getRemoteUrl(String remoteName) {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getRemoteUrl` on a detached instance");
		}

		if (remoteName == null) {
			remoteName = "origin";
		}

		StoredConfig config = gitApi.getRepository().getConfig();
		return config.getString("remote", remoteName, "url");
	}

	@Override
	public List<String> getRemoteBranches() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getRemoteBranches` on a detached instance");
		}

		try {
			List<Ref> branches = gitApi.branchList().setListMode(ListMode.REMOTE).call();
			return branches.stream().map(Ref::getName).collect(Collectors.toList());
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to get remote branches", e);
		}
	}


	@Override
	public String getRevisionHash() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getRevisionHash` on a detached instance");
		}

		ObjectId headRevision = gitApi.getRepository().resolve(Constants.HEAD);

		return headRevision == null ? NO_COMMITS_YET : headRevision.getName();
	}

	@Override
	public void fetch(JGitCredentialsManager jGitCredentialsManager) throws IOException {
		fetch(jGitCredentialsManager, 0, 0);
	}

	private void fetch(JGitCredentialsManager jGitCredentialsManager, int refreshCredentialsTryCount, int tryCount) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `fetch` on a detached instance");
		}

		try {
			FetchCommand fetchCommand = gitApi.fetch();
			fetchCommand.setCredentialsProvider(jGitCredentialsManager.getCredentialsProvider());
			fetchCommand.call();
		}
		catch (GitAPIException e) {
			if (e instanceof TransportException && e.getMessage().contains("not authorized") && refreshCredentialsTryCount < 1) {
				// it's likely that the user is logged in using the username/password authentication method and that their
				// GitLab OAuth access token has expired - try to refresh credentials and retry the operation once
				jGitCredentialsManager.refreshTransientCredentials();
				fetch(jGitCredentialsManager, refreshCredentialsTryCount + 1, tryCount);
				return;
			}

			if (e instanceof TransportException && e.getMessage().contains("authentication not supported") && tryCount < 3) {
				// sometimes GitLab refuses to accept the fetch and returns this error message
				// subsequent fetch attempts succeed however, so we retry the fetch up to 3 times before giving up
				try {
					Thread.sleep(100L * (tryCount + 1));
				}
				catch (InterruptedException ignored) {}

				fetch(jGitCredentialsManager, refreshCredentialsTryCount, tryCount + 1);
				return;
			}

			// give up, refreshing credentials didn't work, retries exhausted, or unexpected error
			throw new IOException(
					String.format("Failed to fetch, tried %d times", tryCount + 1),
					e
			);
		}
	}

	@Override
	public Set<String> verifyDeletedResourcesViaLog(String resourceDir, String resourceTypeKeywords, Set<String> resourceIds) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `verifyDeletedResourcesViaLog` on a detached instance");
		}

		try {
			Set<String> result = new HashSet<>();
			ObjectId userBranch = gitApi.getRepository().resolve("refs/heads/" + username);

			if (userBranch == null) {
				return result;
			}

			for (RevCommit revCommit : gitApi.log().add(userBranch).addPath(resourceDir).call()) {
				String fullCommitMessageLowerCase = revCommit.getFullMessage().toLowerCase();

				// NB: comparison with commit messages generated by:
				// - GitSourceDocumentHandler.removeDocument
				// - GitAnnotationCollectionHandler.removeCollection
				// - GitTagsetHandler.removeTagsetDefinition
				if (fullCommitMessageLowerCase.startsWith(String.format("deleted %s", resourceTypeKeywords.toLowerCase()))) {
					for (String resourceId : resourceIds) {
						if (fullCommitMessageLowerCase.contains(resourceId.toLowerCase())) {
							result.add(resourceId);
						}
					}
				}
			}

			return result;
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to verify deleted resources", e);
		}
	}

	@Override
	public void checkout(String name) throws IOException {
		checkout(name, false);
	}

	@Override
	public void checkout(String name, boolean createBranch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `checkout` on a detached instance");
		}

		try {
			if (gitApi.getRepository().getBranch().equals(name)) {
				// already the active branch
				return;
			}

			CheckoutCommand checkoutCommand = gitApi.checkout();

			if (createBranch) {
				// check if the branch already exists
				List<Ref> refs = gitApi.branchList().call();
				boolean branchAlreadyExists = refs.stream()
						.map(Ref::getName)
						.anyMatch(refName -> refName.equals("refs/heads/" + name));

				if (branchAlreadyExists) {
					createBranch = false;
				}
				else {
					// set the start point for the checkout to master, if we can
					try (RevWalk revWalk = new RevWalk(gitApi.getRepository())) {
						ObjectId masterBranch = gitApi.getRepository().resolve("refs/heads/" + Constants.MASTER);
						// can be null if a project is still empty - in that case branch creation will fail,
						// because JGit cannot create a branch without a start point
						if (masterBranch != null) {
							RevCommit masterHeadCommit = revWalk.parseCommit(masterBranch);
							checkoutCommand.setStartPoint(masterHeadCommit);
						}
					}
				}
			}

			checkoutCommand.setCreateBranch(createBranch);
			checkoutCommand.setName(name).call();

			if (createBranch) {
				// update config to set 'remote' and 'merge' (upstream branch) for this branch
				StoredConfig config = gitApi.getRepository().getConfig();
				config.setString(
						ConfigConstants.CONFIG_BRANCH_SECTION,
						name,
						ConfigConstants.CONFIG_KEY_REMOTE,
						"origin"
				);
				config.setString(
						ConfigConstants.CONFIG_BRANCH_SECTION,
						name,
						ConfigConstants.CONFIG_KEY_MERGE,
						"refs/heads/" + name
				);
				config.save();
			}
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to checkout", e);
		}
	}

	@Override
	public Status getStatus() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getStatus` on a detached instance");
		}

		try {
			return gitApi.status().call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to get status", e);
		}
	}

	@Override
	public boolean hasUntrackedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `hasUntrackedChanges` on a detached instance");
		}

		try {
			Status status = gitApi.status().call();
			return !status.getUntracked().isEmpty();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to check for untracked changes", e);
		}
	}

	@Override
	public boolean hasUncommittedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `hasUncommittedChanges` on a detached instance");
		}

		try {
			return gitApi.status().call().hasUncommittedChanges();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to check for uncommitted changes", e);
		}
	}

	@Override
	public void add(File relativeTargetFile) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `add` on a detached instance");
		}

		try {
			gitApi.add()
					.addFilepattern(FilenameUtils.separatorsToUnix(relativeTargetFile.toString()))
					.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to add", e);
		}
	}

	@Override
	public void add(File targetFile, byte[] bytes) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `add` on a detached instance");
		}

		try (FileOutputStream fileOutputStream = FileUtils.openOutputStream(targetFile)) {
			fileOutputStream.write(bytes);

			Path basePath = gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(targetFile.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);

			gitApi.add()
					.addFilepattern(FilenameUtils.separatorsToUnix(relativeFilePath.toString()))
					.call();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to add", e);
		}
	}

	@Override
	public String addAndCommit(File targetFile, byte[] bytes, String commitMsg, String committerName, String committerEmail) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `addAndCommit` on a detached instance");
		}

		try {
			add(targetFile, bytes);
			return commit(commitMsg, committerName, committerEmail, false);
		}
		catch (IOException e) {
			throw new IOException("Failed to add and commit", e);
		}
	}

	// stages all new, modified and deleted files
	// this is different to commit with all=true, which only stages modified and deleted files
	@Override
	public String addAllAndCommit(String message, String committerName, String committerEmail, boolean force) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `addAllAndCommit` on a detached instance");
		}

		try {
			gitApi.add().addFilepattern(".").call();

			List<DiffEntry> diffEntries = gitApi.diff().call();
			if (!diffEntries.isEmpty()) {
				RmCommand rmCommand = gitApi.rm();
				for (DiffEntry entry : diffEntries) {
					if (entry.getChangeType().equals(ChangeType.DELETE)) {
						rmCommand.addFilepattern(entry.getOldPath());
					}
				}
				rmCommand.call();
			}

			if (force || gitApi.status().call().hasUncommittedChanges()) {
				return commit(message, committerName, committerEmail, force);
			}
			else {
				return getRevisionHash();
			}
		}
		catch (GitAPIException | IOException e) {
			throw new IOException("Failed to add all and commit", e);
		}
	}

	@Override
	public void remove(File targetFile) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `remove` on a detached instance");
		}

		try {
			Path basePath = gitApi.getRepository().getWorkTree().toPath();
			Path absoluteFilePath = Paths.get(targetFile.getAbsolutePath());
			Path relativeFilePath = basePath.relativize(absoluteFilePath);

			gitApi.rm()
					.setCached(true)
					.addFilepattern(FilenameUtils.separatorsToUnix(relativeFilePath.toString()))
					.call();

			// TODO: why don't we trust JGit to delete? (setCached(false) above, the default)
			if (targetFile.isDirectory()) {
				FileUtils.deleteDirectory(targetFile);
			}
			else if (targetFile.exists() && !targetFile.delete()) {
				throw new IOException(
						String.format("Unable to delete file %s.", targetFile) // like message from FileUtils.deleteDirectory above
				);
			}
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to remove", e);
		}
	}

	@Override
	public String removeAndCommit(
			File targetFile,
			boolean removeEmptyParent,
			String commitMsg,
			String committerName,
			String committerEmail
	) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `removeAndCommit` on a detached instance");
		}

		try {
			remove(targetFile);

			File parentDir = targetFile.getParentFile();
			if (removeEmptyParent && parentDir != null && parentDir.isDirectory()) {
				String[] content = parentDir.list();
				if (content != null && content.length == 0) {
					parentDir.delete();
				}
			}

			return commit(commitMsg, committerName, committerEmail, false);
		}
		catch (IOException e) {
			throw new IOException("Failed to remove and commit", e);
		}
	}

	@Override
	public String commit(String message, String committerName, String committerEmail, boolean force) throws IOException {
		return commit(message, committerName, committerEmail, false, force);
	}

	@Override
	public String commit(String message, String committerName, String committerEmail, boolean all, boolean force) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `commit` on a detached instance");
		}

		try {
			if (!gitApi.status().call().hasUncommittedChanges() && !force) {
				return getRevisionHash();
			}

			return gitApi.commit()
					.setMessage(message)
					.setCommitter(committerName, committerEmail)
					.setAll(all)
					.call()
					.getName();
		}
		catch (GitAPIException e) {
			throw new IOException("Failed to commit", e);
		}
	}

	@Override
	public boolean canMerge(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `canMerge` on a detached instance");
		}

		try {
			Repository repository = gitApi.getRepository();

			try (ClosableRecursiveMerger merger = new ClosableRecursiveMerger(repository, true)) {
				Ref ref = repository.findRef(branch);

				if (ref == null) {
					return false;
				}

				ObjectId head = repository.resolve(Constants.HEAD);
				return merger.merge(true, head, ref.getObjectId());
			}
		}
		catch (IOException e) {
			throw new IOException(
					String.format("Failed to check if branch %s can be merged", branch),
					e
			);
		}
	}

	@Override
	public MergeResult merge(String branch) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `merge` on a detached instance");
		}

		try {
			MergeCommand mergeCommand = gitApi.merge();
			mergeCommand.setFastForward(FastForwardMode.FF);

			Ref ref = gitApi.getRepository().findRef(branch);

			if (ref != null) {
				mergeCommand.include(ref);
			}

			return mergeCommand.call();
		}
		catch (GitAPIException e) {
			throw new IOException(
					String.format("Failed to merge branch %s", branch),
					e
			);
		}
	}

	@Override
	public void abortMerge() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `abortMerge` on a detached instance");
		}

		try {
			Repository repository = gitApi.getRepository();

			// clear the merge state
			repository.writeMergeCommitMsg(null);
			repository.writeMergeHeads(null);

			// hard reset the index and working directory to HEAD
			gitApi.reset().setMode(ResetType.HARD).call();
		}
		catch (GitAPIException | IOException e) {
			throw new IOException("Failed to abort merge", e);
		}
	}

	@Override
	public List<PushResult> push(JGitCredentialsManager jGitCredentialsManager) throws IOException {
		return push(jGitCredentialsManager, null, false, 0, 0);
	}

	@Override
	public List<PushResult> pushMaster(JGitCredentialsManager jGitCredentialsManager) throws IOException {
		return push(jGitCredentialsManager, Constants.MASTER, false, 0, 0);
	}

	/**
	 * Pushes commits made locally on the specified branch to the associated remote ('origin') repository and branch.
	 *
	 * @param jGitCredentialsManager a {@link JGitCredentialsManager} to use for authentication
	 * @param branch the branch to push, defaults to the user branch if null
	 * @param skipBranchChecks whether to skip branch checks, normally false
	 * @param refreshCredentialsTryCount how often this push has been attempted already (start with 0, used internally to limit recursive retries)
	 * @param tryCount how often this push has been attempted already (start with 0, used internally to limit recursive retries)
	 * @return a {@link List<PushResult>} containing the results of the push operation
	 * @throws IOException if an error occurs when pushing
	 */
	private List<PushResult> push(
			JGitCredentialsManager jGitCredentialsManager,
			String branch,
			boolean skipBranchChecks,
			int refreshCredentialsTryCount,
			int tryCount
	) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `push` on a detached instance");
		}

		if (CATMAPropertyKey.DEV_PREVENT_PUSH.getBooleanValue()) {
			logger.warning(String.format("FAKE PUSH - %s", getRemoteUrl(null)));
			return new ArrayList<>();
		}

		try {
			String currentBranch = gitApi.getRepository().getBranch();

			if (!skipBranchChecks) {
				if (branch != null && !currentBranch.equals(branch)) {
					throw new IOException(
							String.format(
									"Aborting push - branch to push was \"%s\" but currently checked out branch is \"%s\"",
									branch,
									currentBranch
							)
					);
				}

				if (branch == null && !currentBranch.equals(username)) {
					throw new IOException(
							String.format(
									"Aborting push - branch to push was null (= user branch \"%s\") but currently checked out branch is \"%s\"",
									username,
									currentBranch
							)
					);
				}
			}

			PushCommand pushCommand = gitApi.push();
			pushCommand.setCredentialsProvider(jGitCredentialsManager.getCredentialsProvider());
			pushCommand.setRemote(Constants.DEFAULT_REMOTE_NAME);
			Iterable<PushResult> pushResults = pushCommand.call();

			for (PushResult pushResult : pushResults) {
				for (RemoteRefUpdate remoteRefUpdate : pushResult.getRemoteUpdates()) {
					logger.info(String.format("PushResult: %s", remoteRefUpdate));
				}
			}

			return Lists.newArrayList(pushResults);
		}
		catch (GitAPIException e) {
			if (e instanceof TransportException && e.getMessage().contains("not authorized") && refreshCredentialsTryCount < 1) {
				// it's likely that the user is logged in using the username/password authentication method and that their
				// GitLab OAuth access token has expired - try to refresh credentials and retry the operation once
				jGitCredentialsManager.refreshTransientCredentials();
				return push(jGitCredentialsManager, branch, skipBranchChecks, refreshCredentialsTryCount + 1, tryCount);
			}

			if (e instanceof TransportException && e.getMessage().contains("authentication not supported") && tryCount < 3) {
				// sometimes GitLab refuses to accept the push and returns this error message
				// subsequent push attempts succeed however, so we retry the push up to 3 times before giving up
				try {
					Thread.sleep(100L * (tryCount + 1));
				}
				catch (InterruptedException ignored) {}

				return push(jGitCredentialsManager, branch, skipBranchChecks, refreshCredentialsTryCount, tryCount + 1);
			}

			// give up, refreshing credentials didn't work, retries exhausted, or unexpected error
			throw new IOException(
					String.format("Failed to push, tried %d times", tryCount + 1),
					e
			);
		}
	}


	@Override
	public Set<String> getAdditiveBranchDifferences(String otherBranchName) throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getAdditiveBranchDifferences` on a detached instance");
		}

		try {
			checkout(username, true); // the user branch has to be present at this point

			DiffCommand diffCommand = gitApi.diff();

			ObjectId thisUserBranchHeadRevisionTree = gitApi.getRepository().resolve("refs/heads/" + username + "^{tree}");
			ObjectId otherBranchRevisionTree = gitApi.getRepository().resolve(otherBranchName + "^{tree}");

			Set<String> changedPaths = new HashSet<>();

			if (thisUserBranchHeadRevisionTree != null && otherBranchRevisionTree != null) {
				ObjectReader reader = gitApi.getRepository().newObjectReader();

				CanonicalTreeParser thisUserBranchHeadRevisionTreeParser = new CanonicalTreeParser();
				thisUserBranchHeadRevisionTreeParser.reset(reader, thisUserBranchHeadRevisionTree);

				CanonicalTreeParser otherBranchRevisionTreeParser = new CanonicalTreeParser();
				otherBranchRevisionTreeParser.reset(reader, otherBranchRevisionTree);

				diffCommand.setOldTree(thisUserBranchHeadRevisionTreeParser);
				diffCommand.setNewTree(otherBranchRevisionTreeParser);

				List<DiffEntry> diffResult = diffCommand.call();

				for (DiffEntry diffEntry : diffResult) {
					if (!diffEntry.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
						changedPaths.add(diffEntry.getNewPath());
					}
				}
			}

			return changedPaths;
		}
		catch (GitAPIException | IOException e) {
			throw new IOException("Failed to get additive branch differences", e);
		}
	}


	@Override
	public List<CommitInfo> getOurUnpublishedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getOurUnpublishedChanges` on a detached instance");
		}

		try {
			List<CommitInfo> result = new ArrayList<>();

			if (gitApi.getRepository().resolve(Constants.HEAD) == null) {
				return result; // no HEAD -> new empty project, no commits yet
			}

			List<String> remoteBranches = getRemoteBranches();
			if (remoteBranches.isEmpty()) {
				return result; // project has never been synchronized
			}

			ObjectId originMaster = gitApi.getRepository().resolve("refs/remotes/origin/" + Constants.MASTER);
			if (originMaster == null) {
				return result; // can't find origin/master
			}

			Iterable<RevCommit> commits = gitApi.log()
					.addRange(originMaster, gitApi.getRepository().resolve("refs/heads/" + username))
					.call();

			for (RevCommit commit : commits) {
				result.add(
						new CommitInfo(
								commit.getId().getName(), 
								commit.getShortMessage(), 
								commit.getFullMessage(), 
								commit.getAuthorIdent().getWhen(),
								commit.getAuthorIdent().getName())
				);
			}

			return result;
		}
		catch (GitAPIException | IOException e) {
			throw new IOException("Failed to get our unpublished changes", e);
		}
	}

	@Override
	public List<CommitInfo> getTheirPublishedChanges() throws IOException {
		if (!isAttached()) {
			throw new IllegalStateException("Can't call `getTheirPublishedChanges` on a detached instance");
		}

		try {
			List<CommitInfo> result = new ArrayList<>();

			if (gitApi.getRepository().resolve(Constants.HEAD) == null) {
				return result; // no HEAD -> new empty project, no commits yet
			}

			List<String> remoteBranches = getRemoteBranches();
			if (remoteBranches.isEmpty()) {
				return result; // project has never been synchronized
			}

			ObjectId originMaster = gitApi.getRepository().resolve("refs/remotes/origin/" + Constants.MASTER);
			if (originMaster == null) {
				return result; // can't find origin/master
			}

			Iterable<RevCommit> commits = gitApi.log()
					.addRange(gitApi.getRepository().resolve("refs/heads/" + username), originMaster)
					.call();

			for (RevCommit commit : commits) {
				result.add(
						new CommitInfo(
								commit.getId().getName(), 
								commit.getShortMessage(), 
								commit.getFullMessage(), 
								commit.getAuthorIdent().getWhen(),
								commit.getAuthorIdent().getName())
				);
			}

			return result;
		}
		catch (GitAPIException | IOException e) {
			throw new IOException("Failed to get their published changes", e);
		}
	}


	@Override // AutoCloseable
	public void close() {
		if (gitApi == null) {
			return;
		}

		// TODO: review this - whether the underlying Repository instance is closed apparently depends on how the Git instance (this.gitApi)
		//       was created (see the docstring for Git.close, but note that most of it was copied from AutoCloseable;
		//       also see related TODOs, search for "JGitRepoManager.close")
		// apparently JGit doesn't close Git's internal Repository instance on its close
		// we need to call the close method of the Repository explicitly to avoid open handles to pack files
		// see https://stackoverflow.com/questions/31764311/how-do-i-release-file-system-locks-after-cloning-repo-via-jgit
		// see maybe related https://bugs.eclipse.org/bugs/show_bug.cgi?id=439305
		gitApi.getRepository().close();

		gitApi.close();
		gitApi = null;
	}
} 
