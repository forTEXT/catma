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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LatestContributionsResourceProvider implements GitProjectResourceProvider {
	private final Logger logger = Logger.getLogger(LatestContributionsResourceProvider.class.getName());

	private final String projectId;
	private final ProjectReference projectReference;
	private final File projectPath;
	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final RemoteGitManagerRestricted remoteGitServerManager;
	private final Set<LatestContribution> latestContributions;

	public LatestContributionsResourceProvider(
			String projectId,
			ProjectReference projectReference,
			File projectPath,
			LocalGitRepositoryManager localGitRepositoryManager,
			RemoteGitManagerRestricted remoteGitServerManager,
			Set<LatestContribution> latestContributions
	) {
		this.projectId = projectId;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.latestContributions = latestContributions;
	}

	@Override
	public boolean isReadOnly() {
		return true;
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
		Map<String, TagsetDefinition> tagsetsById = new HashMap<>();

		for (File tagsetDir : tagsetDirs) {
			try {
				String tagsetId = tagsetDir.getName();
				TagsetDefinition tagsetDefinition = gitTagsetHandler.getTagset(tagsetId);
				tagsetsById.put(tagsetDefinition.getUuid(), tagsetDefinition);
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

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (LatestContribution latestContribution : latestContributions) {
				if (latestContribution.getTagsetIds().isEmpty()) {
					continue;
				}

				localGitRepoManager.checkout(latestContribution.getBranch(), false);

				for (String tagsetId : latestContribution.getTagsetIds()) {
					try {
						TagsetDefinition tagsetDefinition = gitTagsetHandler.getTagset(tagsetId);

						if (tagsetsById.containsKey(tagsetDefinition.getUuid())) {
							tagsetsById.get(tagsetDefinition.getUuid()).mergeAdditive(tagsetDefinition);
						}
						else {
							tagsetDefinition.setContribution(true);
							tagsetsById.put(tagsetDefinition.getUuid(), tagsetDefinition);
						}
					}
					catch (IOException e) {
						logger.log(
								Level.SEVERE,
								String.format(
										"Failed to load latest contributions for tagset with ID %1$s in project \"%2$s\" with ID %3$s on branch \"%4$s\"",
										tagsetId,
										projectReference.getName(),
										projectId,
										latestContribution.getBranch()
								),
								e
						);
					}
				}
			}

			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), false);
		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format("Failed to load latest contributions for project \"%s\" with ID %s", projectReference.getName(), projectId),
					e
			);
		}

		return new ArrayList<>(tagsetsById.values());
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
		Map<String, AnnotationCollectionReference> collectionReferencesByCollectionId = new HashMap<>();

		for (File collectionDir : collectionDirs) {
			try {
				String collectionId = collectionDir.getName();
				AnnotationCollectionReference collectionReference = gitAnnotationCollectionHandler.getCollectionReference(collectionId);
				collectionReferencesByCollectionId.put(collectionId, collectionReference);
			}
			catch (Exception e) {
				logger.log(
						Level.SEVERE,
						String.format(
								"Failed to load collection reference for collection at path %1$s in project \"%2$s\" with ID %3$s",
								collectionDir,
								projectReference.getName(),
								projectId
						),
						e
				);
			}
		}

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (LatestContribution latestContribution : latestContributions) {
				if (latestContribution.getCollectionIds().isEmpty()) {
					continue;
				}

				localGitRepoManager.checkout(latestContribution.getBranch(), false);

				for (String collectionId : latestContribution.getCollectionIds()) {
					try {
						if (collectionReferencesByCollectionId.containsKey(collectionId)) {
							collectionReferencesByCollectionId.get(collectionId).setContribution(true);
						}
						else {
							AnnotationCollectionReference collectionReference = gitAnnotationCollectionHandler.getCollectionReference(collectionId);
							collectionReference.setContribution(true);
							collectionReferencesByCollectionId.put(collectionId, collectionReference);
						}
					}
					catch (IOException e) {
						logger.log(
								Level.SEVERE,
								String.format(
										"Failed to load latest contributions for collection with ID %1$s in project \"%2$s\" with ID %3$s on branch \"%4$s\"",
										collectionId,
										projectReference.getName(),
										projectId,
										latestContribution.getBranch()
								),
								e
						);
					}
				}
			}

			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), false);
		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format("Failed to load latest contributions for project \"%s\" with ID %s", projectReference.getName(), projectId),
					e
			);
		}

		return new ArrayList<>(collectionReferencesByCollectionId.values());
	}

	@Override
	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary,
			ProgressListener progressListener,
			boolean withOrphansHandling
	) {
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
		Map<String, AnnotationCollection> collectionsById = new HashMap<>();

		for (File collectionDir : collectionDirs) {
			try {
				String collectionId = collectionDir.getName();
				AnnotationCollection collection = gitAnnotationCollectionHandler.getCollection(
						collectionId, tagLibrary, progressListener, false
				);
				collectionsById.put(collectionId, collection);
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

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (LatestContribution latestContribution : latestContributions) {
				if (latestContribution.getCollectionIds().isEmpty()) {
					continue;
				}

				localGitRepoManager.checkout(latestContribution.getBranch(), false);

				for (String collectionId : latestContribution.getCollectionIds()) {
					try {
						AnnotationCollection collection = gitAnnotationCollectionHandler.getCollection(
								collectionId, tagLibrary, progressListener, false
						);
						collection.setContribution(true);

						if (collectionsById.containsKey(collectionId)) {
							collectionsById.get(collectionId).mergeAdditive(collection);
						}
						else {
							collectionsById.put(collectionId, collection);
						}
					}
					catch (IOException e) {
						logger.log(
								Level.SEVERE,
								String.format(
										"Failed to load latest contributions for collection with ID %1$s in project \"%2$s\" with ID %3$s on branch \"%4$s\"",
										collectionId,
										projectReference.getName(),
										projectId,
										latestContribution.getBranch()
								),
								e
						);
					}
				}
			}

			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), false);
		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format("Failed to load latest contributions for project \"%s\" with ID %s", projectReference.getName(), projectId),
					e
			);
		}

		return new ArrayList<>(collectionsById.values());
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

		AnnotationCollection collectionToReturn = null;

		if (gitAnnotationCollectionHandler.collectionExists(collectionId)) {
			collectionToReturn = gitAnnotationCollectionHandler.getCollection(
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

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (LatestContribution latestContribution : latestContributions) {
				if (!latestContribution.getCollectionIds().contains(collectionId)) {
					continue;
				}

				localGitRepoManager.checkout(latestContribution.getBranch(), false);

				logger.info(
						String.format(
								"Loading latest contributions for collection with ID %s from branch \"%s\"",
								collectionId,
								latestContribution.getBranch()
						)
				);

				try {
					AnnotationCollection collection = gitAnnotationCollectionHandler.getCollection(
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
					collection.setContribution(true);

					if (collectionToReturn != null) {
						collectionToReturn.mergeAdditive(collection);
					}
					else {
						collectionToReturn = collection;
					}
				}
				catch (IOException e) {
					logger.log(
							Level.SEVERE,
							String.format(
									"Failed to load latest contributions for collection with ID %1$s in project \"%2$s\" with ID %3$s on branch \"%4$s\"",
									collectionId,
									projectReference.getName(),
									projectId,
									latestContribution.getBranch()
							),
							e
					);
				}
			}

			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), false);
		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format("Failed to load latest contributions for project \"%s\" with ID %s", projectReference.getName(), projectId),
					e
			);
		}

		return collectionToReturn;
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
		Map<String, SourceDocument> documentsById = new HashMap<>();

		for (File documentDir : documentDirs) {
			try {
				String sourceDocumentId = documentDir.getName();
				SourceDocument sourceDocument = gitSourceDocumentHandler.open(sourceDocumentId);
				documentsById.put(sourceDocumentId, sourceDocument);
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

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (LatestContribution latestContribution : latestContributions) {
				if (latestContribution.getDocumentIds().isEmpty()) {
					continue;
				}

				localGitRepoManager.checkout(latestContribution.getBranch(), false);

				for (String documentId : latestContribution.getDocumentIds()) {
					try {
						SourceDocument document = gitSourceDocumentHandler.open(documentId);

						if (documentsById.containsKey(document.getUuid())) {
							documentsById.put(document.getUuid(), document);
						}

						document.setSourceContentHandler(new BranchAwareSourceContentHandler(
								localGitRepositoryManager,
								remoteGitServerManager.getUsername(),
								projectReference,
								latestContribution.getBranch(),
								document.getSourceContentHandler()
						));
					}
					catch (IOException e) {
						logger.log(
								Level.SEVERE,
								String.format(
										"Failed to load latest contributions for document with ID %1$s in project \"%2$s\" with ID %3$s on branch \"%4$s\"",
										documentId,
										projectReference.getName(),
										projectId,
										latestContribution.getBranch()
								),
								e
						);
					}
				}
			}

			localGitRepoManager.checkout(remoteGitServerManager.getUsername(), false);
		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format("Failed to load latest contributions for project \"%s\" with ID %s", projectReference.getName(), projectId),
					e
			);
		}

		return new ArrayList<>(documentsById.values());
	}

	@Override
	public SourceDocument getDocument(String documentId) throws IOException {
		GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
				localGitRepositoryManager,
				projectPath,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getEmail()
		);

		try (LocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			localGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			for (LatestContribution latestContribution : latestContributions) {
				if (!latestContribution.getDocumentIds().contains(documentId)) {
					continue;
				}

				localGitRepoManager.checkout(latestContribution.getBranch(), false);

				SourceDocument document = gitSourceDocumentHandler.open(documentId);

				document.setSourceContentHandler(new BranchAwareSourceContentHandler(
						localGitRepositoryManager,
						remoteGitServerManager.getUsername(),
						projectReference,
						latestContribution.getBranch(),
						document.getSourceContentHandler()
				));

				localGitRepoManager.checkout(remoteGitServerManager.getUsername(), false);

				return document;
			}
		}

		return gitSourceDocumentHandler.open(documentId);
	}
}
