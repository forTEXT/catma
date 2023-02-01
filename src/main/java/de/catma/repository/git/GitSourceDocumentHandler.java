package de.catma.repository.git;

import com.google.common.collect.Maps;
import de.catma.document.source.*;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.TermInfo;
import de.catma.project.conflict.SourceDocumentConflict;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTermInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GitSourceDocumentHandler {
	private static final String HEADER_FILE_NAME = "header.json";

	private final Logger logger = Logger.getLogger(GitSourceDocumentHandler.class.getName());

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;

	private final CredentialsProvider credentialsProvider;

	public GitSourceDocumentHandler(
			ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager,
			CredentialsProvider credentialsProvider
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Creates a new source document within the project identified by <code>projectId</code>.
	 * <p>
	 * NB: You probably don't want to call this method directly (it doesn't create the submodule in the project root repo). Instead, call the
	 * <code>createSourceDocument</code> method of the {@link GitProjectHandler} class.
	 *
	 * @param projectId the ID of the project within which the source document must be created
	 * @param sourceDocumentId the ID of the source document to create
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original, unmodified source document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified source document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted, UTF-8 encoded source document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded source document
	 * @param terms the collection of terms extracted from the converted source document
	 * @param tokenizedSourceDocumentFileName name of the file within which the terms/tokens should be stored in serialized form
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object
	 * @return the revision hash
	 * @throws IOException if an error occurs while creating the source document
	 */
	public String create(
			String projectId, String sourceDocumentId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			Map<String, List<TermInfo>> terms,
			String tokenizedSourceDocumentFileName,
			SourceDocumentInfo sourceDocumentInfo
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			logger.info("Adding document to the local project " + projectId);

			// create the source document repository
			CreateRepositoryResponse response = this.remoteGitServerManager.createRepository(sourceDocumentId, sourceDocumentId, projectId);

			// clone the repository locally
			localGitRepoManager.clone(projectId, response.repositoryHttpUrl, null, credentialsProvider);

			// write files into the local repo
			File targetOriginalSourceDocumentFile = new File(localGitRepoManager.getRepositoryWorkTree(), originalSourceDocumentFileName);
			File targetConvertedSourceDocumentFile = new File(localGitRepoManager.getRepositoryWorkTree(), convertedSourceDocumentFileName);
			File targetTokenizedSourceDocumentFile = new File(localGitRepoManager.getRepositoryWorkTree(), tokenizedSourceDocumentFileName);

			localGitRepoManager.add(targetOriginalSourceDocumentFile, IOUtils.toByteArray(originalSourceDocumentStream));
			byte[] convertedSourceDocumentBytes = IOUtils.toByteArray(convertedSourceDocumentStream);
			localGitRepoManager.add(targetConvertedSourceDocumentFile, convertedSourceDocumentBytes);

			Map<String, List<GitTermInfo>> gitTermInfos = Maps.newHashMap();
			terms.forEach((term, termInfos) -> gitTermInfos.put(
				term,
				termInfos.stream().map(GitTermInfo::new).collect(Collectors.toList())
			));
			localGitRepoManager.add(
				targetTokenizedSourceDocumentFile, 
				new SerializationHelper<Map<String, List<GitTermInfo>>>().serialize(gitTermInfos).getBytes(StandardCharsets.UTF_8)
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(localGitRepoManager.getRepositoryWorkTree(), HEADER_FILE_NAME);

			sourceDocumentInfo.getTechInfoSet().setCharset(StandardCharsets.UTF_8);
			sourceDocumentInfo.getTechInfoSet().setFileType(FileType.TEXT);
			sourceDocumentInfo.getTechInfoSet().setFileOSType(FileOSType.getFileOSType(new String(convertedSourceDocumentBytes, StandardCharsets.UTF_8)));
			sourceDocumentInfo.getTechInfoSet().setMimeType("text/plain");
			String serializedSourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().serialize(sourceDocumentInfo);

			localGitRepoManager.add(targetHeaderFile, serializedSourceDocumentInfo.getBytes(StandardCharsets.UTF_8));

			// commit newly added files
			String commitMessage = String.format(
					"Adding %s, %s and %s", originalSourceDocumentFileName, convertedSourceDocumentFileName, targetHeaderFile.getName()
			);
			String revisionHash = localGitRepoManager.commit(
					commitMessage, remoteGitServerManager.getUsername(), remoteGitServerManager.getEmail(), false
			);

			logger.info("Finished adding document to the local project " + projectId);

			return revisionHash;
		}
	}

	public SourceDocument open(String projectId, String sourceDocumentId) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String sourceDocumentSubmoduleName = String.format("%s/%s", GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId);
			File sourceDocumentSubmodulePath = new File(localGitRepoManager.getRepositoryWorkTree().toString(), sourceDocumentSubmoduleName);

			File headerFile = new File(sourceDocumentSubmodulePath, HEADER_FILE_NAME);

			String serializedHeaderFile = FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8);
			SourceDocumentInfo sourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().deserialize(serializedHeaderFile, SourceDocumentInfo.class);

			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
			SourceContentHandler sourceContentHandler = new StandardContentHandler();
			sourceContentHandler.setSourceDocumentInfo(sourceDocumentInfo);
			SourceDocument sourceDocument = sourceDocumentHandler.loadSourceDocument(sourceDocumentId, sourceContentHandler);

			String sourceDocumentRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(sourceDocumentSubmoduleName);
			sourceDocument.setRevisionHash(sourceDocumentRevisionHash);

			return sourceDocument;
		}
	}

	public void checkout(String projectId, String sourceDocumentId, String branch, boolean createBranch) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			localGitRepoManager.checkout(branch, createBranch);
		}
	}

	public String update(String projectId, SourceDocument sourceDocument) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocument.getUuid()
			);
			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			File headerFile = new File(localGitRepoManager.getRepositoryWorkTree(), HEADER_FILE_NAME);
			SourceDocumentInfo currentSourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().deserialize(
					FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8), SourceDocumentInfo.class
			);

			SourceDocumentInfo newSourceDocumentInfo = sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
			// the source document file URI is updated when a document is loaded into the graph (see GraphWorktreeProject.getSourceDocumentURI)
			// however, we don't actually want to write that to disk, as it's different for every user
			newSourceDocumentInfo.getTechInfoSet().setURI(currentSourceDocumentInfo.getTechInfoSet().getURI());

			String serializedHeader = new SerializationHelper<SourceDocumentInfo>().serialize(newSourceDocumentInfo);

			return localGitRepoManager.addAndCommit(
					headerFile,
					serializedHeader.getBytes(StandardCharsets.UTF_8),
					String.format(
							"Updated metadata of document \"%s\" with ID %s", newSourceDocumentInfo.getContentInfoSet().getTitle(), sourceDocument.getUuid()
					),
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
		}
	}

	public MergeResult synchronizeBranchWithRemoteMaster(
			String branchName, String projectId, String sourceDocumentId, boolean canPushToRemote
	) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			localGitRepoManager.checkout(Constants.MASTER, false);

			localGitRepoManager.fetch(credentialsProvider);

			MergeResult mergeWithOriginMasterResult = localGitRepoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);

			if (!mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
				throw new IllegalStateException(
						String.format(
								"Merge of origin/master into master of document with ID %1$s of project with ID %2$s failed. Merge status is %3$s",
								sourceDocumentId,
								projectId,
								mergeWithOriginMasterResult.getMergeStatus().toString()
						)
				);
			}

			MergeResult mergeResult = localGitRepoManager.merge(branchName);
			if (mergeResult.getMergeStatus().isSuccessful()) {
				if (canPushToRemote) {
					localGitRepoManager.push(credentialsProvider);
				}

				localGitRepoManager.checkout(branchName, false);

				localGitRepoManager.rebase(Constants.MASTER);
			}

			return mergeResult;
		}
	}

	public Status getStatus(String projectId, String sourceDocumentId) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			return localGitRepoManager.getStatus();
		}
	}

	public SourceDocumentConflict getSourceDocumentConflict(String projectId, String sourceDocumentId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// TODO: refactor how GitTagsetHandler and GitMarkupCollectionHandler open the submodule repo in their respective getConflict functions
			//       no need to open the root repo first if we aren't going to do anything with it (check where else we may be doing this unnecessarily)
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);
			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			Status status = localGitRepoManager.getStatus();

			File headerFile = new File(localGitRepoManager.getRepositoryWorkTree(), HEADER_FILE_NAME);
			String serializedHeaderFile = FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8);

			SourceDocumentConflict sourceDocumentConflict;
			if (status.getConflictingStageState().containsKey(HEADER_FILE_NAME)) {
				SourceDocumentInfo sourceDocumentInfo = resolveSourceDocumentHeaderConflict(
						serializedHeaderFile, status.getConflictingStageState().get(HEADER_FILE_NAME)
				);

				serializedHeaderFile = new SerializationHelper<SourceDocumentInfo>().serialize(sourceDocumentInfo);

				localGitRepoManager.add(headerFile.getAbsoluteFile(), serializedHeaderFile.getBytes(StandardCharsets.UTF_8));

				sourceDocumentConflict = new SourceDocumentConflict(projectId, sourceDocumentId, sourceDocumentInfo.getContentInfoSet());
				sourceDocumentConflict.setHeaderConflict(true);

				status = localGitRepoManager.getStatus();
			}
			else {
				// for now there shouldn't be conflicts on anything other than the header file (nothing else about a document can currently be edited by users)
				throw new IllegalStateException("Unexpected document conflict");
			}

			// for now there shouldn't be conflicts on anything other than the header file (nothing else about a document can currently be edited by users)
			if (!status.getConflicting().isEmpty()) {
				throw new IllegalStateException("Unexpected document conflict");
			}

			return sourceDocumentConflict;
		}
	}

	private SourceDocumentInfo resolveSourceDocumentHeaderConflict(String serializedHeaderFile, StageState stageState) {
		if (stageState.equals(StageState.BOTH_MODIFIED)) {
			// TODO: factor out, duplicated in GitTagsetHandler and GitMarkupCollectionHandler
			String masterVersion = serializedHeaderFile
					.replaceAll("\\Q<<<<<<< HEAD\\E(\\r\\n|\\r|\\n)", "")
					.replaceAll("\\Q=======\\E(\\r\\n|\\r|\\n|.)*?\\Q>>>>>>> \\E.+?(\\r\\n|\\r|\\n)", "");

			String devVersion = serializedHeaderFile
					.replaceAll("\\Q<<<<<<< HEAD\\E(\\r\\n|\\r|\\n|.)*?\\Q=======\\E(\\r\\n|\\r|\\n)", "")
					.replaceAll("\\Q>>>>>>> \\E.+?(\\r\\n|\\r|\\n)", "");
			// /

			SourceDocumentInfo masterSourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().deserialize(masterVersion, SourceDocumentInfo.class);
			SourceDocumentInfo devSourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().deserialize(devVersion, SourceDocumentInfo.class);

			ContentInfoSet masterContentInfoSet = masterSourceDocumentInfo.getContentInfoSet();
			ContentInfoSet devContentInfoSet = devSourceDocumentInfo.getContentInfoSet();

			String title = masterContentInfoSet.getTitle() == null ? "" : masterContentInfoSet.getTitle().trim();
			String devTitle = devContentInfoSet.getTitle() == null ? "" : devContentInfoSet.getTitle().trim();
			if (!title.equalsIgnoreCase(devTitle) && devTitle.length() > 0) {
				title = String.format("%s %s", title, devTitle);
			}

			return new SourceDocumentInfo(
					masterSourceDocumentInfo.getIndexInfoSet(), // cannot change yet
					new ContentInfoSet(
							masterContentInfoSet.getAuthor(), // cannot change yet
							masterContentInfoSet.getDescription(), // cannot change yet
							masterContentInfoSet.getPublisher(), // cannot change yet
							title
					),
					masterSourceDocumentInfo.getTechInfoSet() // cannot change yet
			);
		}
		else {
			return new SerializationHelper<SourceDocumentInfo>().deserialize(serializedHeaderFile, SourceDocumentInfo.class);
		}
	}

	public String addAllAndCommit(String projectId, String sourceDocumentId, String commitMessage, boolean force) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			return localGitRepoManager.addAllAndCommit(commitMessage, remoteGitServerManager.getUsername(), remoteGitServerManager.getEmail(), force);
		}
	}

	public void rebaseToMaster(String projectId, String sourceDocumentId, String branch) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);
			localGitRepoManager.checkout(branch, false);
			localGitRepoManager.rebase(Constants.MASTER);
		}
	}

	public ContentInfoSet getContentInfoSet(String projectId, String sourceDocumentId) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String sourceDocumentGitRepositoryName = String.format(
					"%s/%s/%s", projectRootRepositoryName, GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);
			localGitRepoManager.open(projectId, sourceDocumentGitRepositoryName);

			File headerFile = new File(localGitRepoManager.getRepositoryWorkTree(), HEADER_FILE_NAME);
			String serializedHeaderFile = FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8);
			SourceDocumentInfo sourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().deserialize(serializedHeaderFile, SourceDocumentInfo.class);

			return sourceDocumentInfo.getContentInfoSet();
		}
	}
}
