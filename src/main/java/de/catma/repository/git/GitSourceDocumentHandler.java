package de.catma.repository.git;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import de.catma.document.source.*;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.TermInfo;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTermInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GitSourceDocumentHandler {
	private static final String HEADER_FILE_NAME = "header.json";
	private static final String UTF8_CONVERSION_FILE_EXTENSION = "txt";
	private static final String TOKENIZED_FILE_EXTENSION = "json";

	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final File projectDirectory;
	private final String username;
	private final String email;

	public GitSourceDocumentHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			File projectDirectory,
			String username,
			String email
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.projectDirectory = projectDirectory;
		this.username = username;
		this.email = email;
	}

	public String create(
			File sourceDocumentDirectory,
			String sourceDocumentId,
			InputStream originalSourceDocumentStream,
			String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream,
			String convertedSourceDocumentFileName,
			Map<String, List<TermInfo>> terms,
			String tokenizedSourceDocumentFileName,
			SourceDocumentInfo sourceDocumentInfo
	) throws IOException {
		sourceDocumentDirectory.mkdirs();

		// write files into the local repo
		File targetOriginalSourceDocumentFile = new File(sourceDocumentDirectory, originalSourceDocumentFileName);
		File targetConvertedSourceDocumentFile = new File(sourceDocumentDirectory, convertedSourceDocumentFileName);
		File targetTokenizedSourceDocumentFile = new File(sourceDocumentDirectory, tokenizedSourceDocumentFileName);

		localGitRepositoryManager.add(targetOriginalSourceDocumentFile, IOUtils.toByteArray(originalSourceDocumentStream));
		byte[] convertedSourceDocumentBytes = IOUtils.toByteArray(convertedSourceDocumentStream);
		localGitRepositoryManager.add(targetConvertedSourceDocumentFile, convertedSourceDocumentBytes);

		Map<String, List<GitTermInfo>> gitTermInfos = Maps.newHashMap();
		terms.forEach((term, termInfos) -> gitTermInfos.put(
				term,
				termInfos.stream().map(GitTermInfo::new).collect(Collectors.toList())
		));
		localGitRepositoryManager.add(
				targetTokenizedSourceDocumentFile,
				new SerializationHelper<Map<String, List<GitTermInfo>>>().serialize(gitTermInfos).getBytes(StandardCharsets.UTF_8)
		);

		// write header.json into the local repo
		File targetHeaderFile = new File(sourceDocumentDirectory, HEADER_FILE_NAME);

		sourceDocumentInfo.getTechInfoSet().setCharset(StandardCharsets.UTF_8);
		sourceDocumentInfo.getTechInfoSet().setFileType(FileType.TEXT);
		sourceDocumentInfo.getTechInfoSet().setFileOSType(
				FileOSType.getFileOSType(new String(convertedSourceDocumentBytes, StandardCharsets.UTF_8))
		);
		sourceDocumentInfo.getTechInfoSet().setMimeType("text/plain");
		// the source document file URI in the supplied SourceDocumentInfo initially points to a temp file (same as originalSourceDocumentStream)
		// we update it here to point to the converted file within the current user's local copy of the repo (not persisted)
		sourceDocumentInfo.getTechInfoSet().setURI(targetConvertedSourceDocumentFile.toURI());

		String serializedSourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().serialize(sourceDocumentInfo);
		localGitRepositoryManager.add(targetHeaderFile, serializedSourceDocumentInfo.getBytes(StandardCharsets.UTF_8));

		// commit newly added files
		String commitMessage = String.format(
				"Created document \"%s\" with ID %s",
				sourceDocumentInfo.getContentInfoSet().getTitle(), 
				sourceDocumentId
		);

		String revisionHash = localGitRepositoryManager.commit(commitMessage, username, email, false);
		return revisionHash;
	}

	public SourceDocument open(String sourceDocumentId) throws IOException {
		String sourceDocumentDirectory = String.format("%s/%s", GitProjectHandler.DOCUMENTS_DIRECTORY_NAME, sourceDocumentId);
		File headerFile = Paths.get(
				projectDirectory.getAbsolutePath(),
				sourceDocumentDirectory,
				HEADER_FILE_NAME
		).toFile();

		String serializedHeaderFile = FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8);
		SourceDocumentInfo sourceDocumentInfo = new SerializationHelper<SourceDocumentInfo>().deserialize(serializedHeaderFile, SourceDocumentInfo.class);

		// set URI as it's not persisted (also see create)
		File convertedSourceDocumentFile = Paths.get(
				projectDirectory.getAbsolutePath(),
				sourceDocumentDirectory,
				sourceDocumentId + "." + UTF8_CONVERSION_FILE_EXTENSION
		).toFile();
		sourceDocumentInfo.getTechInfoSet().setURI(convertedSourceDocumentFile.toURI());

		SourceContentHandler sourceContentHandler = new StandardContentHandler();
		sourceContentHandler.setSourceDocumentInfo(sourceDocumentInfo);

		SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler();
		SourceDocument sourceDocument = sourceDocumentHandler.loadSourceDocument(sourceDocumentId, sourceContentHandler);
		return sourceDocument;
	}

	public Map openIndex(String sourceDocumentId) throws IOException {
		String sourceDocumentDirectory = String.format("%s/%s", GitProjectHandler.DOCUMENTS_DIRECTORY_NAME, sourceDocumentId);
		File indexFile = Paths.get(
				projectDirectory.getAbsolutePath(),
				sourceDocumentDirectory,
				sourceDocumentId + "." + TOKENIZED_FILE_EXTENSION
		).toFile();
		return new Gson().fromJson(FileUtils.readFileToString(indexFile, StandardCharsets.UTF_8), Map.class);
	}

	public String update(SourceDocumentReference sourceDocumentRef) throws IOException {
		String sourceDocumentDirectory = String.format("%s/%s", GitProjectHandler.DOCUMENTS_DIRECTORY_NAME, sourceDocumentRef.getUuid());
		File headerFile = Paths.get(
				projectDirectory.getAbsolutePath(),
				sourceDocumentDirectory,
				HEADER_FILE_NAME
		).toFile();

		SourceDocumentInfo newSourceDocumentInfo = sourceDocumentRef.getSourceDocumentInfo();
		String serializedHeader = new SerializationHelper<SourceDocumentInfo>().serialize(newSourceDocumentInfo);

		return localGitRepositoryManager.addAndCommit(
				headerFile,
				serializedHeader.getBytes(StandardCharsets.UTF_8),
				String.format(
						"Updated metadata of document \"%s\" with ID %s", 
						newSourceDocumentInfo.getContentInfoSet().getTitle(), 
						sourceDocumentRef.getUuid()
				),
				username,
				email
		);
	}

	public String removeDocument(SourceDocumentReference sourceDocumentRef) throws IOException {
		String sourceDocumentDirectory = String.format("%s/%s", GitProjectHandler.DOCUMENTS_DIRECTORY_NAME, sourceDocumentRef.getUuid());
		File sourceDocumentDirectoryAbsolutePath = Paths.get(
				projectDirectory.getAbsolutePath(),
				sourceDocumentDirectory
		).toFile();

		String revisionHash = localGitRepositoryManager.removeAndCommit(
				sourceDocumentDirectoryAbsolutePath,
				false, // do not delete the parent directory
				String.format(
						"Deleted document \"%s\" with ID %s",
						sourceDocumentRef.getSourceDocumentInfo().getContentInfoSet().getTitle(),
						sourceDocumentRef.getUuid()
				),
				username,
				email
		);
		return revisionHash;
	}
}
