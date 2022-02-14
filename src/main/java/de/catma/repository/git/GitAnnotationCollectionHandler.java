package de.catma.repository.git;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.ArrayListMultimap;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class GitAnnotationCollectionHandler {
	
	private static final String HEADER_FILE_NAME = "header.json";
	private static final String ANNNOTATIONS_DIR = "annotations";
	
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final String username;
	private final String email;
	private final File projectDirectory;
	private final String projectId;

	public GitAnnotationCollectionHandler(
			ILocalGitRepositoryManager localGitRepositoryManager, File projectDirectory,
			String projectId,
			String username, String email) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.projectDirectory = projectDirectory;
		this.projectId = projectId;
		this.username = username;
		this.email = email;
	}

	public String create(
			File collectionFolder,
			String collectionId,
			String name,
			String description,
			String sourceDocumentId,
			String forkedFromCommitURL
	) throws IOException {
		
		collectionFolder.mkdirs();


		// write header.json into the local repo
		File targetHeaderFile = new File(collectionFolder, HEADER_FILE_NAME);

		GitMarkupCollectionHeader header = new GitMarkupCollectionHeader(
				name, description, 
				this.username,
				forkedFromCommitURL,
				sourceDocumentId
		);
		String serializedHeader = new SerializationHelper<GitMarkupCollectionHeader>().serialize(header);

		String revisionHash = this.localGitRepositoryManager.addAndCommit(
				targetHeaderFile,
				serializedHeader.getBytes(StandardCharsets.UTF_8),
				String.format("Added Collection %1$s with ID %2$s", name, collectionId),
				this.username,
				this.email
		);
		
		return revisionHash;
	}

	public String createTagInstance(
			String collectionId,
			JsonLdWebAnnotation annotation
	) throws IOException {

		String collectionSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionId
		);
		
		// write the serialized tag instance to the AnnotationCollection's annotation dir
		File targetTagInstanceFilePath = 
				Paths.get(
					this.projectDirectory.getAbsolutePath(),
					collectionSubdir,
					ANNNOTATIONS_DIR,
					annotation.getTagInstanceUuid() + ".json"
		).toFile();
		
		String serializedTagInstance = 
				new SerializationHelper<JsonLdWebAnnotation>().serialize(annotation);

		try (FileOutputStream fileOutputStream = FileUtils.openOutputStream(targetTagInstanceFilePath)) {
			fileOutputStream.write(serializedTagInstance.getBytes(StandardCharsets.UTF_8));
		}

		// not doing Git add/commit because Annotations get commited in bulk

		return annotation.getTagInstanceUuid();
	}


	private boolean isAnnotationFilename(String fileName){
		return !(
				fileName.equalsIgnoreCase(HEADER_FILE_NAME) || fileName.equalsIgnoreCase(".git")
		);
	}

	private ArrayList<TagReference> openTagReferences(
		String collectionId, String collectionName, File parentDirectory, 
		ProgressListener progressListener, AtomicInteger counter)
			throws Exception {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());
		
		for (String item : contents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(
					this.openTagReferences(collectionId, collectionName, target, progressListener, counter));
			}
			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			else if (target.isFile() && isAnnotationFilename(target.getName())) {
				counter.incrementAndGet();
				if (counter.intValue() % 1000 == 0) {
					progressListener.setProgress("Loading Annotations %1$s %2$d", collectionName, counter.intValue());
				}
				String serialized = readFileToString(target, StandardCharsets.UTF_8);
				JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
						.deserialize(serialized, JsonLdWebAnnotation.class);

				tagReferences.addAll(
						jsonLdWebAnnotation.toTagReferenceList(
								this.projectId, 
								collectionId
						)
				);
			}
		}

		return tagReferences;
	}
	
	private String readFileToString(File file, Charset encoding) throws IOException {
		StringBuilder builder = new StringBuilder();
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] buffer = new byte[(int)file.length()];
			int read = 0;
			while((read=fis.read(buffer)) != -1) {
				builder.append(new String(buffer, 0, read, encoding));
				if (read == file.length()) {
					break;
				}
			}
		}
		
		return builder.toString();
	}

	public AnnotationCollectionReference getCollectionReference(String collectionId) throws Exception {
		
		String collectionSubdir = String.format(
				"%s/%s", GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, collectionId
		);
		File markupCollectionHeaderFile = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				collectionSubdir,
				HEADER_FILE_NAME
		).toFile();
		
		String serializedAnnotationCollectionHeaderFile = FileUtils.readFileToString(
				markupCollectionHeaderFile, StandardCharsets.UTF_8
		);

		GitMarkupCollectionHeader annotationCollectionHeader = 
			new SerializationHelper<GitMarkupCollectionHeader>()
				.deserialize(serializedAnnotationCollectionHeaderFile, GitMarkupCollectionHeader.class);

		ContentInfoSet contentInfoSet = new ContentInfoSet(
				annotationCollectionHeader.getAuthor(),
				annotationCollectionHeader.getDescription(),
				annotationCollectionHeader.getPublisher(),
				annotationCollectionHeader.getName()
		);

		return  new AnnotationCollectionReference(
				collectionId,
				contentInfoSet, 
				annotationCollectionHeader.getSourceDocumentId(),
				annotationCollectionHeader.getForkedFromCommitURL(),
				annotationCollectionHeader.getResponsableUser());
	}

	public ContentInfoSet getContentInfoSet(String collectionId) throws Exception {
		return getCollectionReference(collectionId).getContentInfoSet();
	}
	
	public AnnotationCollection getCollection(
			String collectionId, TagLibrary tagLibrary, ProgressListener progressListener)
			throws Exception {
		
		AnnotationCollectionReference collectionReference = 
				getCollectionReference(collectionId);
		ContentInfoSet contentInfoSet = collectionReference.getContentInfoSet();
		
		String collectionSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionId
		);
		AtomicInteger counter = new AtomicInteger();
		ArrayList<TagReference> tagReferences = this.openTagReferences(
				collectionId, contentInfoSet.getTitle(),  
				Paths.get(
						this.projectDirectory.getAbsolutePath(),
						collectionSubdir,
						ANNNOTATIONS_DIR).toFile(), 
				progressListener, counter
		);
		
		// handle orphan Annotations
		ArrayListMultimap<TagInstance, TagReference> tagInstances = ArrayListMultimap.create();
		
		Set<String> orphanAnnotationIds = new HashSet<>();
		Iterator<TagReference> tagReferenceIterator = tagReferences.iterator();
		while (tagReferenceIterator.hasNext()) {
			TagReference tagReference = tagReferenceIterator.next();
			if (!orphanAnnotationIds.contains(tagReference.getTagInstanceId())) {
				String tagsetId = tagReference.getTagInstance().getTagsetId();
				
				TagsetDefinition tagset = tagLibrary.getTagsetDefinition(
						tagsetId);
				
				String tagId = tagReference.getTagDefinitionId();
				if (tagset == null || tagset.isDeleted(tagId)) {
					// Tag/Tagset has been deleted, we remove the stale Annotation as well
					orphanAnnotationIds.add(tagReference.getTagInstanceId());
					tagReferenceIterator.remove();
				}
				else {
					// other orphan Annotations get ignored upon indexing
					// until the corresponding Tag or its "deletion" info come along
					
					tagInstances.put(tagReference.getTagInstance(), tagReference);
				}
			}
		}
		
		if (!orphanAnnotationIds.isEmpty()) {
			removeTagInstances(collectionId, orphanAnnotationIds);
		}
		
		//handle orphan Properties
		for (TagInstance tagInstance : tagInstances.keySet()) {
			TagsetDefinition tagset = tagLibrary.getTagsetDefinition(
					tagInstance.getTagsetId());
			if (tagset != null) {
				Collection<Property> properties = tagInstance.getUserDefinedProperties();
				for (Property property : new HashSet<>(properties)) {
					// deleted property?
					if (tagset.isDeleted(property.getPropertyDefinitionId())) {
						// yes, we remove the stale property
						tagInstance.removeUserDefinedProperty(property.getPropertyDefinitionId());
						// and save the change
						JsonLdWebAnnotation annotation = 
								new JsonLdWebAnnotation(
									CATMAPropertyKey.GitLabServerUrl.getValue(), 
									projectId, 
									tagInstances.get(tagInstance),
									tagLibrary);
						createTagInstance(collectionId, annotation);
					}
				}
			}
		}

		return new AnnotationCollection(
				collectionId, contentInfoSet, tagLibrary, tagReferences,
				collectionReference.getSourceDocumentId(),
				collectionReference.getForkedFromCommitURL(),
				collectionReference.getResponsableUser()
		);
	}

	public void removeTagInstances(String collectionId, Collection<String> deletedTagInstanceIds) throws IOException {
		String collectionSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionId
		);

		for (String deletedTagInstanceId : deletedTagInstanceIds) {
			// remove TagInstance file
			File targetTagInstanceFilePath = Paths.get(
					this.projectDirectory.getAbsolutePath(),
					collectionSubdir,
					ANNNOTATIONS_DIR,
					deletedTagInstanceId+".json"
			).toFile();

			this.localGitRepositoryManager.remove(targetTagInstanceFilePath);
		}
	}
	
	public String removeCollection(AnnotationCollectionReference collection) throws IOException {
		String collectionSubDir = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collection.getId()
		);

		File targetCollectionFolderAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				collectionSubDir
		).toFile();
		
		String projectRevision = this.localGitRepositoryManager.removeAndCommit(
				targetCollectionFolderAbsolutePath, 
				false, // do not delete the parent folder
				String.format(
					"Removing Collection %1$s with ID %2$s", 
					collection.getName(), 
					collection.getId()),
				this.username,
				this.email);
		
			
		return projectRevision;		
	}
	
	public void removeCollectionWithoutCommit(AnnotationCollectionReference collection) throws IOException {
		String collectionSubDir = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collection.getId()
		);

		File targetCollectionFolderAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				collectionSubDir
		).toFile();
		
		this.localGitRepositoryManager.remove(
				targetCollectionFolderAbsolutePath); 
	}
	

	public String updateCollection(AnnotationCollectionReference collectionRef) throws IOException {
		String collectionSubDir = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionRef.getId()
		);

		File targetCollectionFolderAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				collectionSubDir
		).toFile();

			
		ContentInfoSet contentInfoSet = collectionRef.getContentInfoSet();
		File targetHeaderFile = 
				new File(targetCollectionFolderAbsolutePath, HEADER_FILE_NAME);

		GitMarkupCollectionHeader header = new GitMarkupCollectionHeader(
				contentInfoSet.getTitle(), 
				contentInfoSet.getDescription(), 
				collectionRef.getSourceDocumentId(), 
				collectionRef.getResponsableUser(),
				collectionRef.getForkedFromCommitURL());

		SerializationHelper<GitMarkupCollectionHeader> serializationHelper = new SerializationHelper<>();
		String serializedHeader = serializationHelper.serialize(header);
			
		String collectionRevision = this.localGitRepositoryManager.addAndCommit(
				targetHeaderFile, 
				serializedHeader.getBytes(StandardCharsets.UTF_8), 
				String.format("Updated metadata of Collection %1$s with ID %2$s", 
					collectionRef.getName(), collectionRef.getId()),
				this.username,
				this.email);

		return collectionRevision;
	}	
}
