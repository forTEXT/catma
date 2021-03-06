package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.google.common.collect.Maps;

import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.TermInfo;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTermInfo;

public class GitSourceDocumentHandler {
	private Logger logger = Logger.getLogger(GitSourceDocumentHandler.class.getName());
	
    private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;

	private final CredentialsProvider credentialsProvider;


	public GitSourceDocumentHandler(ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager,
			CredentialsProvider credentialsProvider) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Creates a new source document within the project identified by <code>projectId</code>.
	 * <p>
	 * NB: You probably don't want to call this method directly (it doesn't create the submodule in the project root
	 * repo). Instead call the <code>createSourceDocument</code> method of the {@link GitProjectManager} class.
	 *
	 * @param projectId the ID of the project within which the source document must be created
	 * @param sourceDocumentId the ID of the source document to create.
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original,
	 *                                     unmodified source document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified source
	 *                                       document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted,
	 *                                      UTF-8 encoded source document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded source
	 *                                        document
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object
	 * @param terms 
	 * @return the revision hash
	 * @throws IOException if an error occurs while creating the source document
	 */
	public String create(String projectId, String sourceDocumentId,
						 InputStream originalSourceDocumentStream,
						 String originalSourceDocumentFileName,
						 InputStream convertedSourceDocumentStream,
						 String convertedSourceDocumentFileName,
						 Map<String, List<TermInfo>> terms,
						 String tokenizedSourceDocumentFileName,
						 SourceDocumentInfo sourceDocumentInfo 
						 
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			logger.info("Adding SourceDocument to the local project " + projectId);
			
			// create the source document repository

			CreateRepositoryResponse response = 
				this.remoteGitServerManager.createRepository(
					sourceDocumentId, sourceDocumentId, projectId
				);

			// clone the repository locally

			localGitRepoManager.clone(
				projectId,
				response.repositoryHttpUrl,
				null,
				credentialsProvider
			);

			// write the original and converted source document files into the local repo
			File targetOriginalSourceDocumentFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				originalSourceDocumentFileName
			);
			File targetConvertedSourceDocumentFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				convertedSourceDocumentFileName
			);
			
			File targetTokenizedSourceDocumentFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				tokenizedSourceDocumentFileName
			);

			byte[] bytes = IOUtils.toByteArray(originalSourceDocumentStream);
			localGitRepoManager.add(targetOriginalSourceDocumentFile, bytes);
			bytes = IOUtils.toByteArray(convertedSourceDocumentStream);
			localGitRepoManager.add(targetConvertedSourceDocumentFile, bytes);

			Map<String, List<GitTermInfo>> gitTermInfos = Maps.newHashMap();
			terms.forEach((term, termInfos) -> {
				gitTermInfos.put(
					term, 
					termInfos
						.stream()
						.map(termInfo -> new GitTermInfo(termInfo))
						.collect(Collectors.toList()));
			});
			
			localGitRepoManager.add(
				targetTokenizedSourceDocumentFile, 
				new SerializationHelper<Map<String, List<GitTermInfo>>>()
					.serialize(gitTermInfos)
					.getBytes(StandardCharsets.UTF_8));
			
			// write header.json into the local repo
			File targetHeaderFile = new File(
				localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);
			sourceDocumentInfo.getTechInfoSet().setCharset(Charset.forName("UTF-8"));
			sourceDocumentInfo.getTechInfoSet().setFileType(FileType.TEXT);
			sourceDocumentInfo.getTechInfoSet().setFileOSType(FileOSType.getFileOSType(new String(bytes, "UTF-8")));
			sourceDocumentInfo.getTechInfoSet().setMimeType("text/plain");
//			GitSourceDocumentInfo gitSourceDocumentInfo = new GitSourceDocumentInfo(sourceDocumentInfo);
			String serializedGitSourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>()
					.serialize(sourceDocumentInfo);
			localGitRepoManager.add(
				targetHeaderFile, serializedGitSourceDocumentInfo.getBytes(StandardCharsets.UTF_8)
			);

			// commit newly added files
			String commitMessage = String.format("Adding %s, %s and %s", originalSourceDocumentFileName,
					convertedSourceDocumentFileName, targetHeaderFile.getName());
			String revisionHash = localGitRepoManager.commit(
				commitMessage,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail(),
				false
			);
			
			logger.info("Finished adding SourceDocument to the local project " + projectId);
			
			return revisionHash;
		}
	}

	public SourceDocument open(@Nonnull String projectId, @Nonnull String sourceDocumentId)
			throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String sourceDocumentSubmoduleName = String.format(
					"%s/%s", GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME, sourceDocumentId
			);

			File sourceDocumentSubmodulePath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					sourceDocumentSubmoduleName
			);

			File headerFile = new File(
					sourceDocumentSubmodulePath, "header.json"
			);

			String serializedHeaderFile = FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8);
			SourceDocumentInfo sourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>()
					.deserialize(
							serializedHeaderFile,
							SourceDocumentInfo.class
					);


			de.catma.document.source.SourceDocumentHandler docHandler =
					new de.catma.document.source.SourceDocumentHandler();
			
			SourceContentHandler contentHandler = new StandardContentHandler();
			
//			SourceDocumentInfo sourceDocumentInfo = gitSourceDocumentInfo.getSourceDocumentInfo();
			contentHandler.setSourceDocumentInfo(sourceDocumentInfo);
			
			SourceDocument sourceDocument = docHandler.loadSourceDocument(sourceDocumentId, contentHandler);

			String sourceDocumentRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					sourceDocumentSubmoduleName
			);
			sourceDocument.setRevisionHash(sourceDocumentRevisionHash);

			return sourceDocument;
		}
	}
}
