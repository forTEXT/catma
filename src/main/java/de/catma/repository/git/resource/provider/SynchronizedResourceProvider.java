package de.catma.repository.git.resource.provider;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.repository.git.GitAnnotationCollectionHandler;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GitSourceDocumentHandler;
import de.catma.repository.git.GitTagsetHandler;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.resource.provider.interfaces.GitProjectResourceProvider;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SynchronizedResourceProvider implements GitProjectResourceProvider {
	private final Logger logger = Logger.getLogger(SynchronizedResourceProvider.class.getName());

	private final String projectId;
	private final ProjectReference projectReference;
	private final File projectPath;
	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final RemoteGitManagerRestricted remoteGitServerManager;
	private final CredentialsProvider credentialsProvider;

	public SynchronizedResourceProvider(
			String projectId,
			ProjectReference projectReference,
			File projectPath,
			LocalGitRepositoryManager localGitRepositoryManager,
			RemoteGitManagerRestricted remoteGitServerManager,
			CredentialsProvider credentialsProvider
	) {
		this.projectId = projectId;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public List<TagsetDefinition> getTagsets() {
		File tagsetsDirectory = Paths.get(
				projectPath.getAbsolutePath(),
				GitProjectHandler.TAGSETS_DIRECTORY_NAME
		).toFile();

		if (!tagsetsDirectory.exists()) {
			return new ArrayList<>();
		}

		GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
				localGitRepositoryManager,
				projectPath,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		File[] tagsetDirs = tagsetsDirectory.listFiles(File::isDirectory);
		ArrayList<TagsetDefinition> tagsets = new ArrayList<>();

		for (File tagsetDir : tagsetDirs) {
			try {
				String tagsetId = tagsetDir.getName();
				TagsetDefinition tagset = gitTagsetHandler.getTagset(tagsetId);
				tagsets.add(tagset);
			}
			catch (Exception e) {
				logger.log(
						Level.SEVERE,
						String.format(
								"Failed to load tagset at path %1$s for project \"%2$s\" with ID %3$s",
								tagsetDir,
								projectReference.getName(),
								projectId
						),
						e
				);
			}
		}

		return tagsets;
	}

	@Override
	public List<AnnotationCollectionReference> getCollectionReferences() {
		File collectionsDirectory = Paths.get(
				projectPath.getAbsolutePath(),
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME
		).toFile();

		if (!collectionsDirectory.exists()) {
			return new ArrayList<>();
		}

		GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
				localGitRepositoryManager,
				projectPath,
				projectId,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		File[] collectionDirs = collectionsDirectory.listFiles(File::isDirectory);
		ArrayList<AnnotationCollectionReference> collectionReferences = new ArrayList<>();

		for (File collectionDir : collectionDirs) {
			try {
				String collectionId = collectionDir.getName();
				AnnotationCollectionReference collectionReference = gitAnnotationCollectionHandler.getCollectionReference(collectionId);
				collectionReferences.add(collectionReference);
			}
			catch (Exception e) {
				logger.log(
						Level.SEVERE,
						String.format(
								"Failed to load collection reference for collection at path %1$s for project \"%2$s\" with ID %3$s",
								collectionDir,
								projectReference.getName(),
								projectId
						),
						e
				);
			}
		}

		return collectionReferences;
	}

	@Override
	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary,
			ProgressListener progressListener,
			boolean withOrphansHandling
	) throws IOException {
		File collectionsDirectory = Paths.get(
				projectPath.getAbsolutePath(),
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME
		).toFile();

		if (!collectionsDirectory.exists()) {
			return new ArrayList<>();
		}

		GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
				localGitRepositoryManager,
				projectPath,
				projectId,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		File[] collectionDirs = collectionsDirectory.listFiles(File::isDirectory);
		ArrayList<AnnotationCollection> collections = new ArrayList<>();

		for (File collectionDir : collectionDirs) {
			try {
				String collectionId = collectionDir.getName();
				AnnotationCollection collection = gitAnnotationCollectionHandler.getCollection(
						collectionId, tagLibrary, progressListener, withOrphansHandling
				);
				collections.add(collection);
			}
			catch (Exception e) {
				logger.log(
						Level.SEVERE,
						String.format(
								"Failed to load collection at path %1$s for project \"%2$s\" with ID %3$s",
								collectionDir,
								projectReference.getName(),
								projectId
						),
						e
				);
			}
		}

		if (withOrphansHandling) {
			try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
				localGitRepoManager.open(projectReference.getNamespace(), projectId);

				if (!localGitRepoManager.hasUncommitedChanges() && !localGitRepoManager.hasUntrackedChanges()) {
					return collections;
				}

				localGitRepoManager.addAllAndCommit(
						"Auto-committing deletion of orphaned annotations and properties",
						remoteGitServerManager.getUsername(),
						remoteGitServerManager.getEmail(),
						false
				);
				localGitRepoManager.push(credentialsProvider);
			}
		}

		return collections;
	}

	@Override
	public AnnotationCollection getCollection(String collectionId, TagLibrary tagLibrary) throws IOException {
		GitAnnotationCollectionHandler gitAnnotationCollectionHandler = new GitAnnotationCollectionHandler(
				localGitRepositoryManager,
				projectPath,
				projectId,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		return gitAnnotationCollectionHandler.getCollection(
				collectionId, 
				tagLibrary, 
				new ProgressListener() {
					@Override
					public void setProgress(String value, Object... args) {
						logger.info(String.format(value, args));
					}
				},
				false
		);
	}

	@Override
	public List<SourceDocument> getDocuments() {
		File documentsDirectory = Paths.get(
				projectPath.getAbsolutePath(),
				GitProjectHandler.DOCUMENTS_DIRECTORY_NAME
		).toFile();

		if (!documentsDirectory.exists()) {
			return new ArrayList<>();
		}

		GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
				localGitRepositoryManager,
				projectPath,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		File[] documentDirs = documentsDirectory.listFiles(File::isDirectory);
		ArrayList<SourceDocument> documents = new ArrayList<>();

		for (File documentDir : documentDirs) {
			try {
				String sourceDocumentId = documentDir.getName();
				SourceDocument document = gitSourceDocumentHandler.open(sourceDocumentId);
				documents.add(document);
			}
			catch (Exception e) {
				logger.log(
						Level.SEVERE,
						String.format(
								"Failed to load document at path %1$s for project \"%2$s\" with ID %3$s",
								documentDir,
								projectReference.getName(),
								projectId
						),
						e
				);
			}
		}

		return documents;
	}

	@Override
	public SourceDocument getDocument(String documentId) throws IOException {
		GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
				localGitRepositoryManager,
				projectPath,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		return gitSourceDocumentHandler.open(documentId);
	}
}
