package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.CredentialsProvider;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ArrayListMultimap;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class LatestContributionsResourceProvider implements IGitProjectResourceProvider {
	private final Logger logger = Logger.getLogger(LatestContributionsResourceProvider.class.getName());
			
	private final String projectId;
	private final ProjectReference projectReference;
	private final File projectPath;

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	
	private final Set<LatestContribution> latestContributions;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	
	
	
	public LatestContributionsResourceProvider(String projectId, ProjectReference projectReference,
			File projectPath, ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager, CredentialsProvider credentialsProvider,
			Set<LatestContribution> latestContributions) {
		
		super();
		this.projectId = projectId;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.latestContributions = latestContributions;
	}

	

	@Override
	public List<TagsetDefinition> getTagsets() {
		Map<String, TagsetDefinition> tagsetsById = new HashMap<>();
		File tagsetsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.TAGSETS_DIRECTORY_NAME)
			.toFile();
		
		if (!tagsetsDir.exists()) {
			return new ArrayList<>(tagsetsById.values());
		}
		
		File[] tagsetDirs = tagsetsDir.listFiles(file -> file.isDirectory());			
		
		GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					this.localGitRepositoryManager, 
					this.projectPath,
					this.remoteGitServerManager.getUsername(),
					this.remoteGitServerManager.getEmail());
		
		
		for (File tagsetDir : tagsetDirs) {
			try {
				String tagsetId = tagsetDir.getName();
				TagsetDefinition tagset = gitTagsetHandler.getTagset(tagsetId);						 
				tagsetsById.put(tagset.getUuid(), tagset);
			}
			catch (Exception e) {
				logger.log(
						Level.SEVERE,
						String.format(
								"error loading Tagset %1$s for project %2$s",
								tagsetDir,
								projectId), 
						e);
			}
		 
		}
		
		try (ILocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			for (LatestContribution latestContribution : latestContributions) {
				if (!latestContribution.getTagsetIds().isEmpty()) {
					localGitRepManager.checkout(latestContribution.getBranch(), false);
					for (String tagsetId : latestContribution.getTagsetIds()) {
						try {
							TagsetDefinition tagset = 
									gitTagsetHandler.getTagset(tagsetId);
							
							if (tagsetsById.containsKey(tagset.getUuid())) {
								tagsetsById.get(tagset.getUuid()).mergeAdditive(tagset);
							}
							else {
								tagset.setContribution(true);
								tagsetsById.put(tagset.getUuid(), tagset);
							}
						}
						catch (IOException e) {
							logger.log(
									Level.SEVERE,
									String.format(
										"error loading latest contributions for"
										+ "Tagset %1$s for project %2$s branch %3$s",
										tagsetId,
										projectId,
										latestContribution.getBranch()), 
									e);
							
						}
					}
				}
			}
			localGitRepManager.checkout(
					remoteGitServerManager.getUsername(), false);

		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format(
						"error loading latest contributions for"
						+ "project %1$s",
						projectId), 
					e);
			
		}
		
		return new ArrayList<>(tagsetsById.values());
	}


	@Override
	public List<AnnotationCollectionReference> getCollectionReferences() {
		ArrayList<AnnotationCollectionReference> collectionReferences = new ArrayList<>();
		
		File collectionsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME)
			.toFile();
		
		if (!collectionsDir.exists()) {
			return collectionReferences;
		}
		
		File[] collectionDirs = 
				collectionsDir.listFiles(file -> file.isDirectory());
		
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);
		Set<String> collectionIds = new HashSet<>();
		
		for (File collectionDir : collectionDirs) {
			String collectionId = collectionDir.getName();
			try {
				collectionReferences.add(
					gitMarkupCollectionHandler.getCollectionReference(collectionId));
				collectionIds.add(collectionId);
			} catch (Exception e) {
				logger.log(
				Level.SEVERE, 
					String.format(
						"error loading Collection reference %1$s for project %2$s",
						collectionDir,
						projectId), 
					e);
				 
			}
		}
		
		try (ILocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			for (LatestContribution latestContribution : latestContributions) {
				if (!latestContribution.getCollectionIds().isEmpty()) {
					localGitRepManager.checkout(latestContribution.getBranch(), false);
					for (String collectionId : latestContribution.getCollectionIds()) {
						try {
							if (!collectionIds.contains(collectionId)) {
								collectionReferences.add(gitMarkupCollectionHandler.getCollectionReference(collectionId));
								collectionIds.add(collectionId);
							}
						}
						catch (IOException e) {
							logger.log(
									Level.SEVERE,
									String.format(
										"error loading latest contributions for"
										+ "Annotation Collection %1$s for project %2$s branch %3$s",
										collectionId,
										projectId,
										latestContribution.getBranch()), 
									e);
							
						}
					}
				}
			}
			localGitRepManager.checkout(
					remoteGitServerManager.getUsername(), false);

		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format(
						"error loading latest contributions for"
						+ "project %1$s",
						projectId), 
					e);
			
		}

		
		return collectionReferences;
	}

	@Override
	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary, ProgressListener progressListener, 
			boolean withOrphansHandling) throws IOException {
		
		Map<String, AnnotationCollection> collectionsById = Maps.newHashMap();
		File collectionsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME)
			.toFile();
		
		if (!collectionsDir.exists()) {
			return new ArrayList<>(collectionsById.values());
		}
		
		File[] collectionDirs = 
				collectionsDir.listFiles(file -> file.isDirectory());			

		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);

		for (File collectionDir : collectionDirs) {
			String collectionId = collectionDir.getName();
			try {
				AnnotationCollection collection = 
						gitMarkupCollectionHandler.getCollection(
								collectionId, 
								tagLibrary, 
								progressListener,
								false);

				collectionsById.put(collectionId, collection);
				
			} catch (Exception e) {
				logger.log(
				Level.SEVERE, 
					String.format(
						"error loading Collection reference %1$s for project %2$s",
						collectionDir,
						projectId), 
					e);
				 
			}
		}

		try (ILocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			for (LatestContribution latestContribution : latestContributions) {
				if (!latestContribution.getCollectionIds().isEmpty()) {
					localGitRepManager.checkout(latestContribution.getBranch(), false);
					for (String collectionId : latestContribution.getCollectionIds()) {
						try {
							AnnotationCollection collection = gitMarkupCollectionHandler.getCollection(
									collectionId, 
									tagLibrary, 
									progressListener,
									false);
							
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
										"error loading latest contributions for"
										+ "Annotation Collection %1$s for project %2$s branch %3$s",
										collectionId,
										projectId,
										latestContribution.getBranch()), 
									e);
							
						}
					}
				}
			}
			localGitRepManager.checkout(
					remoteGitServerManager.getUsername(), false);

		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format(
						"error loading latest contributions for"
						+ "project %1$s",
						projectId), 
					e);
			
		}
		
		return new ArrayList<>(collectionsById.values());
	}	
	
	@Override
	public AnnotationCollection getCollection(
			String collectionId, 
			TagLibrary tagLibrary) throws IOException {
		GitAnnotationCollectionHandler gitMarkupCollectionHandler = 
				new GitAnnotationCollectionHandler(
						this.localGitRepositoryManager, 
						this.projectPath,
						this.projectId,
						this.remoteGitServerManager.getUsername(),
						this.remoteGitServerManager.getEmail()
		);


		AnnotationCollection collection = null;
		if (gitMarkupCollectionHandler.collectionExists(collectionId)) {
			collection = gitMarkupCollectionHandler.getCollection(
					collectionId, 
					tagLibrary, 
					new ProgressListener() {
						
						@Override
						public void setProgress(String value, Object... args) {
							logger.info(
								String.format(
										"Loading AnnotationCollection with %1$s: %2$s", 
										collectionId, 
										String.format(value, args)));
						}
					},
					false);
		}
		
		try (ILocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			for (LatestContribution latestContribution : latestContributions) {
				if (latestContribution.getCollectionIds().contains(collectionId)) {
					localGitRepManager.checkout(latestContribution.getBranch(), false);
					try {
						AnnotationCollection contribution = gitMarkupCollectionHandler.getCollection(
								collectionId, 
								tagLibrary, 
								new ProgressListener() {
									
									@Override
									public void setProgress(String value, Object... args) {
										logger.info(
											String.format(
													"Loading contributions from %1$s for AnnotationCollection with %2$s: %3$s",
													latestContribution.getBranch(),
													collectionId, 
													String.format(value, args)));
									}
								},
								false);
						
						if (collection != null) {							
							collection.mergeAdditive(contribution);
						}
						else {
							collection = contribution;
						}
					}
					catch (IOException e) {
						logger.log(
								Level.SEVERE,
								String.format(
									"error loading latest contributions for"
									+ "Annotation Collection %1$s for project %2$s branch %3$s",
									collectionId,
									projectId,
									latestContribution.getBranch()), 
								e);
						
					}
				}
			}
			localGitRepManager.checkout(
					remoteGitServerManager.getUsername(), false);

		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format(
						"error loading latest contributions for"
						+ "project %1$s",
						projectId), 
					e);
			
		}

		
		return collection;
	}
	
	@Override
	public List<SourceDocument> getDocuments() {
		Map<String, SourceDocument> documentsById = Maps.newHashMap();
		
		File documentsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.DOCUMENTS_DIRECTORY_NAME)
			.toFile();
		
		if (!documentsDir.exists()) {
			return new ArrayList<>(0);
		}

		File[] documentDirs = 
				documentsDir.listFiles(file -> file.isDirectory());			

		GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
				this.localGitRepositoryManager, 
				this.projectPath,
				this.remoteGitServerManager.getUsername(),
				this.remoteGitServerManager.getEmail());

		for (File documentDir : documentDirs) {
			String sourceDocumentId = documentDir.getName().toString();
			try {
				documentsById.put(
						sourceDocumentId, gitSourceDocumentHandler.open(sourceDocumentId));
			} catch (Exception e) {
				logger.log(
					Level.SEVERE,
					String.format(
						"error loading Document %1$s for Project %2$s",
						documentDir,
						projectId), 
					e);					
			}
		}
		
		
		try (ILocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			for (LatestContribution latestContribution : latestContributions) {
				if (!latestContribution.getDocumentIds().isEmpty()) {
					localGitRepManager.checkout(latestContribution.getBranch(), false);
					for (String documentId : latestContribution.getDocumentIds()) {
						try {
							SourceDocument document = 
									gitSourceDocumentHandler.open(documentId);
							
							if (documentsById.containsKey(document.getUuid())) {
								documentsById.put(document.getUuid(), document);
							}
							
							
							document.setSourceContentHandler(
								new BranchAwareSourceContentHandler(
										this.localGitRepositoryManager, 
										this.remoteGitServerManager.getUsername(),
										this.projectReference,
										latestContribution.getBranch(),
										document.getSourceContentHandler()));
							
						}
						catch (IOException e) {
							logger.log(
									Level.SEVERE,
									String.format(
										"error loading latest contributions for"
										+ "Document %1$s for project %2$s branch %3$s",
										documentId,
										projectId,
										latestContribution.getBranch()), 
									e);
							
						}
					}
				}
			}
			localGitRepManager.checkout(
					remoteGitServerManager.getUsername(), false);

		}
		catch (IOException e) {
			logger.log(
					Level.SEVERE,
					String.format(
						"error loading latest contributions for"
						+ "project %1$s",
						projectId), 
					e);
			
		}
		
		return new ArrayList<SourceDocument>(documentsById.values());
	}
	
	@Override
	public SourceDocument getDocument(String documentId) throws IOException {
		GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
				this.localGitRepositoryManager, 
				this.projectPath,
				this.remoteGitServerManager.getUsername(),
				this.remoteGitServerManager.getEmail());

		try (ILocalGitRepositoryManager localGitRepManager = this.localGitRepositoryManager) {
			localGitRepManager.open(this.projectReference.getNamespace(), this.projectReference.getProjectId());
			for (LatestContribution latestContribution : latestContributions) {
				if (latestContribution.getDocumentIds().contains(documentId)) {
					localGitRepManager.checkout(latestContribution.getBranch(), false);
					SourceDocument document = 
							gitSourceDocumentHandler.open(documentId);
					
					document.setSourceContentHandler(
						new BranchAwareSourceContentHandler(
								this.localGitRepositoryManager, 
								this.remoteGitServerManager.getUsername(),
								this.projectReference,
								latestContribution.getBranch(),
								document.getSourceContentHandler()));

					localGitRepManager.checkout(
							remoteGitServerManager.getUsername(), false);
					
					return document;
				}
			}
		}
		return gitSourceDocumentHandler.open(documentId);
	}
	
	@Override
	public boolean isReadOnly() {
		return true;
	}

}
