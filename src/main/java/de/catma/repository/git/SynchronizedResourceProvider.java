package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.CredentialsProvider;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class SynchronizedResourceProvider implements IGitProjectResourceProvider {
	private final Logger logger = Logger.getLogger(SynchronizedResourceProvider.class.getName());
			
	private final String projectId;
	private final ProjectReference projectReference;
	private final File projectPath;

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	
	public SynchronizedResourceProvider(String projectId, ProjectReference projectReference,
			File projectPath, ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager, CredentialsProvider credentialsProvider) {
		super();
		this.projectId = projectId;
		this.projectReference = projectReference;
		this.projectPath = projectPath;
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = credentialsProvider;
	}

	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final CredentialsProvider credentialsProvider;

	

	@Override
	public List<TagsetDefinition> getTagsets() {
		ArrayList<TagsetDefinition> result = new ArrayList<>();
		File tagsetsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.TAGSETS_DIRECTORY_NAME)
			.toFile();
		
		if (!tagsetsDir.exists()) {
			return result;
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
				result.add(tagset);
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
		
		return result;
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

		for (File collectionDir : collectionDirs) {
			String collectionId = collectionDir.getName();
			try {
				collectionReferences.add(
					gitMarkupCollectionHandler.getCollectionReference(collectionId));
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
		
		return collectionReferences;
	}

	@Override
	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary, ProgressListener progressListener, 
			boolean withOrphansHandling) throws IOException {
		
		ArrayList<AnnotationCollection> collections = new ArrayList<>();
		File collectionsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME)
			.toFile();
		
		if (!collectionsDir.exists()) {
			return collections;
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
				collections.add(
					gitMarkupCollectionHandler.getCollection(
							collectionId, 
							tagLibrary, 
							progressListener,
							withOrphansHandling));
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
		
		if (withOrphansHandling) {
			try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
				localRepoManager.open(this.projectReference.getNamespace(), this.projectId);
				if (localRepoManager.hasUncommitedChanges() || localRepoManager.hasUntrackedChanges()) {
					localRepoManager.addAllAndCommit(
							String.format(
								"Auto committing removal of orphan Annotations "
								+ "and orphan Properties for Project %1$s", this.projectId),
							this.remoteGitServerManager.getUsername(), 
							this.remoteGitServerManager.getEmail(), 
							false);
					localRepoManager.push(credentialsProvider);
				}
			}
		}		
		
		return collections;
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

		return gitMarkupCollectionHandler.getCollection(
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
	@Override
	public List<SourceDocument> getDocuments() {
		ArrayList<SourceDocument> documents = new ArrayList<>();
		
		File documentsDir = Paths.get(
				this.projectPath.getAbsolutePath(),
				GitProjectHandler.DOCUMENTS_DIRECTORY_NAME)
			.toFile();
		
		if (!documentsDir.exists()) {
			return documents;
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
				documents.add(gitSourceDocumentHandler.open(sourceDocumentId));
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
		
		return documents;
	}
	
	@Override
	public SourceDocument getDocument(String documentId) throws IOException {
		GitSourceDocumentHandler gitSourceDocumentHandler =	new GitSourceDocumentHandler(
				this.localGitRepositoryManager, 
				this.projectPath,
				this.remoteGitServerManager.getUsername(),
				this.remoteGitServerManager.getEmail());

		return gitSourceDocumentHandler.open(documentId);
	}
}
