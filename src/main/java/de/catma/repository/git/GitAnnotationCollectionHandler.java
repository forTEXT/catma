package de.catma.repository.git;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.reflect.TypeToken;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.Pair;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GitAnnotationCollectionHandler {
	public static final String ANNNOTATIONS_DIR = "annotations";
	private static final String HEADER_FILE_NAME = "header.json";

	private final Logger logger = Logger.getLogger(GitAnnotationCollectionHandler.class.getName());

	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final File projectDirectory;
	private final String projectId;
	private final String username;
	private final String email;

	private final int maxPageSizeBytes;

	public GitAnnotationCollectionHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			File projectDirectory,
			String projectId,
			String username,
			String email
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.projectDirectory = projectDirectory;
		this.projectId = projectId;
		this.username = username;
		this.email = email;

		this.maxPageSizeBytes = CATMAPropertyKey.MAX_ANNOTATION_PAGE_FILE_SIZE_BYTES.getIntValue();
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
				String.format("Created annotation collection \"%s\" with ID %s", name, collectionId),
				this.username,
				this.email
		);
		
		return revisionHash;
	}

	public void updateTagInstance(String collectionId, JsonLdWebAnnotation updatedAnnotation) throws IOException {
		String collectionDirectory = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionId
		);

		File annotationsDirectory = Paths.get(
				projectDirectory.getAbsolutePath(),
				collectionDirectory,
				ANNNOTATIONS_DIR
		).toFile();
		annotationsDirectory.mkdirs();

		File pageFile = Paths.get(
				projectDirectory.getAbsolutePath(),
				collectionDirectory,
				ANNNOTATIONS_DIR,
				updatedAnnotation.getPageFilename()
		).toFile();

		String serializedPageContent = FileUtils.readFileToString(pageFile, StandardCharsets.UTF_8);

		Type listType = new TypeToken<ArrayList<JsonLdWebAnnotation>>(){}.getType();
		ArrayList<JsonLdWebAnnotation> currentAnnotations = new SerializationHelper<ArrayList<JsonLdWebAnnotation>>()
				.deserialize(serializedPageContent, listType);

		JsonLdWebAnnotation currentAnnotation = currentAnnotations.stream()
				.filter(anno -> anno.getId().equals(updatedAnnotation.getId())).findFirst().orElse(null);

		if (currentAnnotation == null) {
			throw new IOException(
					String.format(
							"Couldn't find annotation with ID %1$s in page file \"%2$s\" of collection with ID %3$s",
							updatedAnnotation.getId(),
							updatedAnnotation.getPageFilename(),
							collectionId
					)
			);
		}

		currentAnnotation.setBody(updatedAnnotation.getBody());

		serializedPageContent = new SerializationHelper<JsonLdWebAnnotation>().serialize(currentAnnotations);

		FileUtils.writeStringToFile(pageFile, serializedPageContent, StandardCharsets.UTF_8);
	}

	public void createTagInstances(String collectionId, List<Pair<JsonLdWebAnnotation, TagInstance>> annotations) throws IOException {
		String collectionDirectory = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionId
		);

		File annotationsDirectory = Paths.get(
				projectDirectory.getAbsolutePath(),
				collectionDirectory,
				ANNNOTATIONS_DIR
		).toFile();
		annotationsDirectory.mkdirs();

		String currentPageFilename = getCurrentPageFilename(annotationsDirectory, false); // <username>_<pagenumber>.json

		File currentPageFile = Paths.get(
				projectDirectory.getAbsolutePath(),
				collectionDirectory,
				ANNNOTATIONS_DIR,
				currentPageFilename
		).toFile();

		RandomAccessFile raPageFile = new RandomAccessFile(currentPageFile, "rw");

		try {
			while (!annotations.isEmpty()) {
				Pair<JsonLdWebAnnotation, TagInstance> entry = annotations.get(0);
				JsonLdWebAnnotation annotation = entry.getFirst();
				TagInstance tagInstance = entry.getSecond();

				int annotationByteSize = annotation.getSerializedItemUTF8ByteSize();

				if (currentPageFile.length() + annotationByteSize > maxPageSizeBytes) {
					// the current page file doesn't have enough space to write the new annotation, we need to create a new one
					raPageFile.close();
					currentPageFilename = getCurrentPageFilename(annotationsDirectory, true);
					currentPageFile = Paths.get(
							projectDirectory.getAbsolutePath(),
							collectionDirectory,
							ANNNOTATIONS_DIR,
							currentPageFilename
					).toFile();
					raPageFile = new RandomAccessFile(currentPageFile, "rw");
				}

				String serializedAnnotation = annotation.asSerializedListItem();

				// condition is '> 2' because a page file can contain only "[]" (if all of the annotations that the page
				// contains are deleted)
				if (currentPageFile.length() > 2) {
					// replace the opening list bracket of the serialized annotation to be written with a comma
					// in preparation for appending the annotation to the list of existing annotations in the page file
					serializedAnnotation = "," + serializedAnnotation.substring(1);

					// seek so that the closing list bracket in the page file will be overwritten with the new annotation
					// + a new closing bracket
					raPageFile.seek(currentPageFile.length() - 2);
				}

				raPageFile.write(serializedAnnotation.getBytes(StandardCharsets.UTF_8));

				annotation.setPageFilename(currentPageFilename);
				tagInstance.setPageFilename(currentPageFilename);
				annotations.remove(entry);
			}
		}
		finally {
			raPageFile.close();
		}

		// not doing Git add/commit because annotations are committed in bulk
	}

	private String getCurrentPageFilename(File annotationsDir, boolean forceNew) {
		File[] pages = annotationsDir.listFiles(
				file -> 
					file.isFile() 
					&& isUserPage(file) 
					&& file.getName().toLowerCase().endsWith(".json"));
		
		Integer pageNumber = 0;
		
		if (pages.length > 0) {
			Arrays.sort(pages, (page1, page2) -> getPageNumber(page1).compareTo(getPageNumber(page2)));
			pageNumber = pages.length-1;
			
			File lastPage = pages[pageNumber];
			
			if (lastPage.length() >= maxPageSizeBytes || forceNew) {
				pageNumber++;
			}
		}

		return username + "_" + pageNumber + ".json";
	}

	private Integer getPageNumber(File page) {
		try {
			String pageNumber = page.getName().substring(
					page.getName().lastIndexOf("_") + 1,
					page.getName().lastIndexOf('.')
			);
			return Integer.valueOf(pageNumber);
		}
		catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			logger.warning(
					String.format("\"%s\" doesn't seem to be a page file! The full path was %s", page.getName(), page.getAbsolutePath())
			);
		}

		return -1;
	}

	private boolean isUserPage(File pagefile) {
		String pageFileUser = pagefile.getName().substring(0, pagefile.getName().lastIndexOf("_"));
		return pageFileUser.equals(username);		
	}

	private boolean isAnnotationFilename(String fileName){
		return !(
				fileName.equalsIgnoreCase(HEADER_FILE_NAME) || fileName.equalsIgnoreCase(".git")
		);
	}

	private ArrayList<TagReference> openTagReferences(
		String collectionId, String collectionName, File parentDirectory, 
		ProgressListener progressListener, AtomicInteger counter)
			throws IOException {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		if (!parentDirectory.exists()) {
			return tagReferences;
		}
		
		String[] contents = parentDirectory.list();
		
		
		for (String pageFilename : contents) {
			File pageFile = new File(parentDirectory, pageFilename);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (pageFile.isDirectory() && !pageFile.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(
					this.openTagReferences(collectionId, collectionName, pageFile, progressListener, counter));
			}
			// if item is <user>_<pagenumber>.json, read it into a list of TagReference objects
			else if (pageFile.isFile() && isAnnotationFilename(pageFile.getName())) {
				String pageContent =  
						new String(Files.readAllBytes(pageFile.toPath()), StandardCharsets.UTF_8);
				
				Type listType = new TypeToken<ArrayList<JsonLdWebAnnotation>>(){}.getType();
				
				ArrayList<JsonLdWebAnnotation> list = 
						new SerializationHelper<ArrayList<JsonLdWebAnnotation>>().deserialize(
								pageContent, listType);

				for (JsonLdWebAnnotation webAnnotation : list) {
					counter.incrementAndGet();
					if (counter.intValue() % 1000 == 0) {
						progressListener.setProgress("Loading annotations from collection \"%s\" (%d)", collectionName, counter.intValue());
					}

					webAnnotation.setPageFilename(pageFile.getName());
					tagReferences.addAll(webAnnotation.toTagReferences(collectionId));
				}
			}
		}

		return tagReferences;
	}

	public boolean collectionExists(String collectionId) {
		String collectionSubdir = String.format(
				"%s/%s", GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, collectionId
		);
		File markupCollectionHeaderFile = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				collectionSubdir,
				HEADER_FILE_NAME
		).toFile();

		return markupCollectionHeaderFile.exists(); 
	}

	public AnnotationCollectionReference getCollectionReference(String collectionId) throws IOException {
		
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
				annotationCollectionHeader.getResponsibleUser());
	}

	public ContentInfoSet getContentInfoSet(String collectionId) throws Exception {
		return getCollectionReference(collectionId).getContentInfoSet();
	}

	public AnnotationCollection getCollection(
			String collectionId,
			TagLibrary tagLibrary,
			ProgressListener progressListener,
			boolean handleOrphans
	) throws IOException {
		AnnotationCollectionReference collectionReference = getCollectionReference(collectionId);
		ContentInfoSet contentInfoSet = collectionReference.getContentInfoSet();

		progressListener.setProgress("Loading collection \"%s\" with ID %s", contentInfoSet.getTitle(), collectionId);

		String relativeCollectionPath = String.format(
				"%s/%s", 
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME, 
				collectionId
		);

		ArrayList<TagReference> tagReferences = openTagReferences(
				collectionId,
				contentInfoSet.getTitle(),
				Paths.get(projectDirectory.getAbsolutePath(), relativeCollectionPath, ANNNOTATIONS_DIR).toFile(),
				progressListener,
				new AtomicInteger()
		);

		// handle orphaned annotations
		if (handleOrphans) {
			logger.info(
					String.format("Checking for orphans in collection \"%s\" with ID %s", contentInfoSet.getTitle(), collectionId)
			);

			Set<TagInstance> orphanedTagInstances = new HashSet<>();
			// this is used later when checking for orphaned properties
			ArrayListMultimap<TagInstance, TagReference> tagReferencesByTagInstance = ArrayListMultimap.create();

			Iterator<TagReference> tagReferenceIterator = tagReferences.iterator();
			while (tagReferenceIterator.hasNext()) {
				TagReference tagReference = tagReferenceIterator.next();
				TagInstance tagInstance = tagReference.getTagInstance();

				if (orphanedTagInstances.contains(tagInstance)) {
					// remove the stale annotation (TagReference) from memory (as tagReferences is later returned)
					tagReferenceIterator.remove();
					continue;
				}

				TagsetDefinition tagsetDefinition = tagLibrary.getTagsetDefinition(tagInstance.getTagsetId());
				String tagDefinitionId = tagReference.getTagDefinitionId();

				if (tagsetDefinition == null || tagsetDefinition.isDeleted(tagDefinitionId)) {
					// tagset/tag has been deleted, add the tag instance to the collection of those that will be removed from persistent storage
					orphanedTagInstances.add(tagInstance);
					// remove the stale annotation (TagReference) from memory as well (as tagReferences is later returned)
					// other annotations are removed in the if statement above
					tagReferenceIterator.remove();
				}
				else {
					// keep for later when checking for orphaned properties
					tagReferencesByTagInstance.put(tagInstance, tagReference);
				}
			}

			if (!orphanedTagInstances.isEmpty()) {
				removeTagInstances(collectionId, orphanedTagInstances);
			}

			// handle orphaned properties
			for (TagInstance tagInstance : tagReferencesByTagInstance.keySet()) {
				TagsetDefinition tagsetDefinition = tagLibrary.getTagsetDefinition(tagInstance.getTagsetId());
				Collection<Property> userDefinedProperties = tagInstance.getUserDefinedProperties();

				// a new HashSet is created to prevent ConcurrentModificationException
				// (tagInstance.getUserDefinedProperties() returns an UnmodifiableCollection)
				for (Property property : new HashSet<>(userDefinedProperties)) {
					if (tagsetDefinition.isDeleted(property.getPropertyDefinitionId())) {
						// property has been deleted, remove the stale property from memory
						tagInstance.removeUserDefinedProperty(property.getPropertyDefinitionId());
						// persist the change
						JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
								tagReferencesByTagInstance.get(tagInstance),
								tagLibrary,
								tagInstance.getPageFilename()
						);
						updateTagInstance(collectionId, annotation);
					}
				}
			}
		}

		return new AnnotationCollection(
				collectionId,
				contentInfoSet,
				tagLibrary,
				tagReferences,
				collectionReference.getSourceDocumentId(),
				collectionReference.getForkedFromCommitURL(),
				collectionReference.getResponsibleUser()
		);
	}

	// TODO: consider performing some kind of page file "compression"
	//       (fill gaps by shifting annotations and utilise max. page file size as far as possible)
	public void removeTagInstances(String collectionId, Collection<TagInstance> deletedTagInstances) throws IOException {
		String collectionSubdir = String.format(
				"%s/%s",
				GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME,
				collectionId
		);

		Multimap<String, TagInstance> deletedTagInstancesByPageFilename = Multimaps.index(deletedTagInstances, TagInstance::getPageFilename);

		for (String pageFilename : deletedTagInstancesByPageFilename.keySet()) {
			File pageFile =	Paths.get(
					projectDirectory.getAbsolutePath(),
					collectionSubdir,
					ANNNOTATIONS_DIR,
					pageFilename
			).toFile();

			String serializedPageContent = FileUtils.readFileToString(pageFile, StandardCharsets.UTF_8);

			Type listType = new TypeToken<ArrayList<JsonLdWebAnnotation>>(){}.getType();
			ArrayList<JsonLdWebAnnotation> currentAnnotations = new SerializationHelper<ArrayList<JsonLdWebAnnotation>>().deserialize(
					serializedPageContent, listType
			);

			Collection<String> tagInstanceUuidsToRemove = deletedTagInstancesByPageFilename.get(pageFilename).stream()
					.map(TagInstance::getUuid).collect(Collectors.toList());

			boolean anyRemoved = currentAnnotations.removeIf(anno -> tagInstanceUuidsToRemove.contains(anno.getTagInstanceUuid()));

			if (anyRemoved) {
				serializedPageContent = new SerializationHelper<JsonLdWebAnnotation>().serialize(currentAnnotations);
				FileUtils.writeStringToFile(pageFile, serializedPageContent, StandardCharsets.UTF_8);
			}
			else {
				logger.warning(String.format(
						"Tag instances to be deleted were not found in the expected page. Collection ID: %1$s, tag instance IDs: %2$s",
						collectionId,
						String.join(",", tagInstanceUuidsToRemove)
				));
			}
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
					"Deleted annotation collection \"%s\" with ID %s",
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
				collectionRef.getResponsibleUser(),
				collectionRef.getForkedFromCommitURL(),
				collectionRef.getSourceDocumentId());
		
		header.setPublisher(contentInfoSet.getPublisher());
		header.setAuthor(contentInfoSet.getAuthor());
		
		SerializationHelper<GitMarkupCollectionHeader> serializationHelper = new SerializationHelper<>();
		String serializedHeader = serializationHelper.serialize(header);
			
		String collectionRevision = this.localGitRepositoryManager.addAndCommit(
				targetHeaderFile, 
				serializedHeader.getBytes(StandardCharsets.UTF_8), 
				String.format("Updated metadata of annotation collection \"%s\" with ID %s",
					collectionRef.getName(), collectionRef.getId()),
				this.username,
				this.email);

		return collectionRevision;
	}	
}
