package de.catma.repository.git;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.google.common.collect.ArrayListMultimap;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.project.conflict.AnnotationConflict;
import de.catma.project.conflict.CollectionConflict;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class GitMarkupCollectionHandler {
	private static final String HEADER_FILE_NAME = "header.json";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final CredentialsProvider credentialsProvider;

	public GitMarkupCollectionHandler(ILocalGitRepositoryManager localGitRepositoryManager,
			IRemoteGitManagerRestricted remoteGitServerManager,
			CredentialsProvider credentialsProvider) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Creates a new markup collection.
	 * <p>
	 * NB: You probably don't want to call this method directly (it doesn't create the submodule in the project root
	 * repo). Instead call the <code>createMarkupCollection</code> method of the {@link GitProjectManager} class.
	 *
	 * @param projectId the ID of the project within which the new markup collection must be created
	 * @param collectionId the ID of the new collection
	 * @param name the name of the new markup collection
	 * @param description the description of the new markup collection
	 * @param sourceDocumentId the ID of the source document to which the new markup collection relates
	 * @param sourceDocumentVersion the version of the source document to which the new markup collection relates
	 * @return the Collection's revisionHash
	 * @throws IOException if an error occurs while creating the markup collection
	 */
	public String create(
			String projectId,
			String collectionId,
			String name,
			String description,
			String sourceDocumentId,
			String sourceDocumentVersion
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the remote markup collection repository

			CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(
							collectionId, collectionId, projectId
					);

			// clone the repository locally
			localGitRepoManager.clone(
					projectId,
					createRepositoryResponse.repositoryHttpUrl,
					null,
					credentialsProvider
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(localGitRepoManager.getRepositoryWorkTree(), HEADER_FILE_NAME);

			GitMarkupCollectionHeader header = new GitMarkupCollectionHeader(
					name, description, sourceDocumentId, sourceDocumentVersion
			);
			String serializedHeader = new SerializationHelper<GitMarkupCollectionHeader>().serialize(header);

			return localGitRepoManager.addAndCommit(
					targetHeaderFile,
					serializedHeader.getBytes(StandardCharsets.UTF_8),
					String.format("Added Collection %1$s with ID %2$s", name, collectionId),
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);


		}

	}

	/**
	 * Creates a tag instance (annotation) within the markup collection identified by <code>markupCollectionId</code>.
	 * <p>
	 * NB: This method purposefully does NOT perform any Git add/commit operations as it is expected to be called a very
	 * large number of times when a graph worktree is written to disk.
	 *
	 * @param projectId the ID of the project that contains the markup collection within which the tag instance should
	 *                  be created
	 * @param markupCollectionId the ID of the markup collection within which to create the tag instance
	 * @param annotation a {@link JsonLdWebAnnotation} object representing the tag instance
	 * @return the tag instance UUID contained within the <code>annotation</code> argument
	 * @throws IOException if an error occurs while creating the tag instance
	 */
	public String createTagInstance(
			String projectId,
			String markupCollectionId,
			JsonLdWebAnnotation annotation
	) throws IOException {

		// TODO: check that the markup collection references the tagset for the tag instance being added
		// TODO: check that the tag instance is for the correct document
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + markupCollectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);
			// write the serialized tag instance to the markup collection submodule
			File targetTagInstanceFilePath = new File(
					localGitRepoManager.getRepositoryWorkTree(),
					"annotations/" + annotation.getTagInstanceUuid() + ".json"
			);
			String serializedTagInstance = new SerializationHelper<JsonLdWebAnnotation>().serialize(annotation);

			try (FileOutputStream fileOutputStream = FileUtils.openOutputStream(targetTagInstanceFilePath)) {
				fileOutputStream.write(serializedTagInstance.getBytes(StandardCharsets.UTF_8));
			}

			// not doing Git add/commit, see method doc comment
		}

		return annotation.getTagInstanceUuid();
	}


	private boolean isTagInstanceFilename(String fileName){
		// TODO: split filename parts and check if it is a CATMA uuid
		return !(
				fileName.equalsIgnoreCase(HEADER_FILE_NAME) || fileName.equalsIgnoreCase(".git")
		);
	}

	private ArrayList<TagReference> openTagReferences(
		String projectId, String markupCollectionId, String collectionName, File parentDirectory, 
		ProgressListener progressListener, AtomicInteger counter)
			throws Exception {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());
		
		for (String item : contents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(
					this.openTagReferences(projectId, markupCollectionId, collectionName, target, progressListener, counter));
			}
			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			else if (target.isFile() && isTagInstanceFilename(target.getName())) {
				counter.incrementAndGet();
				if (counter.intValue() % 1000 == 0) {
					progressListener.setProgress("Loading Annotations %1$s %2$d", collectionName, counter.intValue());
				}
				String serialized = readFileToString(target, StandardCharsets.UTF_8);
				JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
						.deserialize(serialized, JsonLdWebAnnotation.class);

				tagReferences.addAll(
						jsonLdWebAnnotation.toTagReferenceList(
								projectId, 
								markupCollectionId
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

	public AnnotationCollectionReference getCollectionReference(String projectId, String markupCollectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleRelDir = String.format(
					"%s/%s", GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
			);

			File markupCollectionSubmoduleAbsPath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleRelDir
			);

			String markupCollectionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					markupCollectionSubmoduleRelDir
			);

			localGitRepoManager.detach();  // can't call open on an attached instance

			File markupCollectionHeaderFile = new File(
					markupCollectionSubmoduleAbsPath,
					HEADER_FILE_NAME
			);

			String serializedMarkupCollectionHeaderFile = FileUtils.readFileToString(
					markupCollectionHeaderFile, StandardCharsets.UTF_8
			);

			GitMarkupCollectionHeader markupCollectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
					.deserialize(serializedMarkupCollectionHeaderFile, GitMarkupCollectionHeader.class);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					markupCollectionHeader.getAuthor(),
					markupCollectionHeader.getDescription(),
					markupCollectionHeader.getPublisher(),
					markupCollectionHeader.getName()
			);

			return  new AnnotationCollectionReference(
					markupCollectionId, markupCollectionRevisionHash,
					contentInfoSet, 
					markupCollectionHeader.getSourceDocumentId(),
					markupCollectionHeader.getSourceDocumentVersion());
		}

	}

	public ContentInfoSet getContentInfoSet(String projectId, String markupCollectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleRelDir = String.format(
					"%s/%s", GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
			);

			File markupCollectionSubmoduleAbsPath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleRelDir
			);

			localGitRepoManager.detach();  // can't call open on an attached instance

			File markupCollectionHeaderFile = new File(
					markupCollectionSubmoduleAbsPath,
					HEADER_FILE_NAME
			);

			String serializedMarkupCollectionHeaderFile = FileUtils.readFileToString(
					markupCollectionHeaderFile, StandardCharsets.UTF_8
			);

			GitMarkupCollectionHeader markupCollectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
					.deserialize(serializedMarkupCollectionHeaderFile, GitMarkupCollectionHeader.class);

			return new ContentInfoSet(
					markupCollectionHeader.getAuthor(),
					markupCollectionHeader.getDescription(),
					markupCollectionHeader.getPublisher(),
					markupCollectionHeader.getName()
			);

		}

	}
	
	public AnnotationCollection getCollection(
			String projectId, String collectionId, TagLibrary tagLibrary, ProgressListener progressListener)
			throws Exception {
		
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleRelDir = 
					GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME + "/" + collectionId;

			File markupCollectionSubmoduleAbsPath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleRelDir
			);

			String markupCollectionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					markupCollectionSubmoduleRelDir
			);

			localGitRepoManager.detach();  // can't call open on an attached instance
			
			
			
			File markupCollectionHeaderFile = new File(
					markupCollectionSubmoduleAbsPath,
					HEADER_FILE_NAME
			);

			String serializedMarkupCollectionHeaderFile = FileUtils.readFileToString(
					markupCollectionHeaderFile, StandardCharsets.UTF_8
			);

			GitMarkupCollectionHeader markupCollectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
					.deserialize(serializedMarkupCollectionHeaderFile, GitMarkupCollectionHeader.class);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					markupCollectionHeader.getAuthor(),
					markupCollectionHeader.getDescription(),
					markupCollectionHeader.getPublisher(),
					markupCollectionHeader.getName()
			);
			
			AtomicInteger counter = new AtomicInteger();
			ArrayList<TagReference> tagReferences = this.openTagReferences(
					projectId, collectionId, contentInfoSet.getTitle(), markupCollectionSubmoduleAbsPath, 
					progressListener, counter
			);
			
			// handle orphan Annotations
			ArrayListMultimap<TagInstance, TagReference> tagInstances = ArrayListMultimap.create();
			
			Set<String> orphanAnnotationIds = new HashSet<>();
			Iterator<TagReference> tagReferenceIterator = tagReferences.iterator();
			while (tagReferenceIterator.hasNext()) {
				TagReference tagReference = tagReferenceIterator.next();
				if (!orphanAnnotationIds.contains(tagReference.getTagInstanceId())) {
					TagsetDefinition tagset = tagLibrary.getTagsetDefinition(
							tagReference.getTagInstance().getTagsetId());
					
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
			removeTagInstances(projectId, collectionId, orphanAnnotationIds);
			
			//handle orphan Properties
			for (TagInstance tagInstance : tagInstances.keySet()) {
				TagsetDefinition tagset = tagLibrary.getTagsetDefinition(
						tagInstance.getTagsetId());
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
						createTagInstance(projectId, collectionId, annotation);
					}
				}
			}
			


			AnnotationCollection userMarkupCollection = new AnnotationCollection(
					collectionId, contentInfoSet, tagLibrary, tagReferences,
					markupCollectionHeader.getSourceDocumentId(), 
					markupCollectionHeader.getSourceDocumentVersion()
			);
			userMarkupCollection.setRevisionHash(markupCollectionRevisionHash);
			return userMarkupCollection;
		}
	}

	public String commit(String projectId, String collectionId, String message) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			return localGitRepoManager.commit(
					message,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					false);
		}
	}

	public String removeTagInstancesAndCommit(
			String projectId, String collectionId, Collection<String> deletedTagInstanceIds, String commitMsg) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			if (localGitRepoManager.hasUntrackedChanges()) {
				localGitRepoManager.addAllAndCommit(
					"Auto-committing changes before performing a deletion of "
					+ "Annotations as part of a Tag deletion operation",
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					false);				
			}
			
			removeTagInstances(localGitRepoManager, deletedTagInstanceIds);

			if (localGitRepoManager.hasUncommitedChanges()) {
				return localGitRepoManager.commit(
						commitMsg,
						remoteGitServerManager.getUsername(),
						remoteGitServerManager.getEmail(),
						false);

			}
			else {
				return localGitRepoManager.getRevisionHash();
			}

		}
	}

	private void removeTagInstances(
			ILocalGitRepositoryManager localGitRepoManager, Collection<String> deletedTagInstanceIds) throws IOException {
		for (String deletedTagInstanceId : deletedTagInstanceIds) {
			// remove TagInstance file
			File targetTagInstanceFilePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					"annotations",
					deletedTagInstanceId+".json"
			).toFile();

			localGitRepoManager.remove(targetTagInstanceFilePath);
		}
	}

	public void removeTagInstances(String projectId, String collectionId, Collection<String> deletedTagInstanceIds) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			removeTagInstances(localGitRepoManager, deletedTagInstanceIds);

			// not doing Git add/commit
		}
	}

	public String addAndCommitChanges(String projectId, String collectionId, String commitMsg, boolean force) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			return localGitRepoManager.addAllAndCommit(
					commitMsg,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(),
					force);
		}
	}
	
	public void checkout(String projectId, String collectionId, String branch, boolean createBranch) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			localGitRepoManager.checkout(branch, createBranch);
		}		
	}	
	
	public String updateCollection(String projectId, AnnotationCollectionReference collectionRef) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionRef.getId();

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			
			ContentInfoSet contentInfoSet = collectionRef.getContentInfoSet();
			
			File targetHeaderFile = new File(localGitRepoManager.getRepositoryWorkTree(), HEADER_FILE_NAME);
			GitMarkupCollectionHeader header = new GitMarkupCollectionHeader(
					contentInfoSet.getTitle(), 
					contentInfoSet.getDescription(), 
					collectionRef.getSourceDocumentId(), 
					collectionRef.getSourceDocumentRevisiohHash());

			SerializationHelper<GitMarkupCollectionHeader> serializationHelper = new SerializationHelper<>();
			String serializedHeader = serializationHelper.serialize(header);
			
			localGitRepoManager.add(
					targetHeaderFile,
					serializedHeader.getBytes(StandardCharsets.UTF_8));

			String collectionRevision = localGitRepoManager.addAndCommit(
					targetHeaderFile, 
					serializedHeader.getBytes(StandardCharsets.UTF_8), 
					String.format("Updated metadata of Collection %1$s with ID %2$s", 
						collectionRef.getName(), collectionRef.getId()),
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail());

			return collectionRevision;
		}
	}	
	
	public MergeResult synchronizeBranchWithRemoteMaster(
			String branch, String projectId, String collectionId, boolean canPushToRemote) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			localGitRepoManager.checkout(Constants.MASTER, false);
			
			localGitRepoManager.fetch(credentialsProvider);
			
			MergeResult mergeWithOriginMasterResult = 
					localGitRepoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
				
			if (!mergeWithOriginMasterResult.getMergeStatus().isSuccessful()) {
				throw new IllegalStateException(
					String.format(
						"Merge of origin/master into master "
						+ "of Collection with ID %1$s "
						+ "of Project with ID %2$s "
						+ "failed. "
						+ "Merge Status is %3$s",
					collectionId,
					projectId,
					mergeWithOriginMasterResult.getMergeStatus().toString()));
			}			
			
			MergeResult mergeResult = localGitRepoManager.merge(branch);
			if (mergeResult.getMergeStatus().isSuccessful()) {
				if (canPushToRemote) {
					localGitRepoManager.push(credentialsProvider);
				}
				
				localGitRepoManager.checkout(branch, false);
				
				localGitRepoManager.rebase(Constants.MASTER);
			}
			
			return mergeResult;

		}		
	}

	public boolean hasUncommittedChanges(String projectId, String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);
			
			return localGitRepoManager.hasUncommitedChanges();
		}
	}

	public Status getStatus(String projectId, String collectionId) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);
			return localGitRepoManager.getStatus();
		}
	}

	public CollectionConflict getCollectionConflict(String projectId, String collectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String collectionSubmoduleRelDir = 
					GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME + "/" + collectionId;
			
			File collectionSubmoduleAbsPath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					collectionSubmoduleRelDir
			);

			localGitRepoManager.detach(); 
			
			String collectionGitRepositoryName =
					projectRootRepositoryName + "/" + collectionSubmoduleRelDir;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			Status status = localGitRepositoryManager.getStatus();
			
			File collectionHeaderFile = new File(
					collectionSubmoduleAbsPath,
					HEADER_FILE_NAME
			);

			String serializedCollectionHeaderFile = FileUtils.readFileToString(
					collectionHeaderFile, StandardCharsets.UTF_8
			);

			CollectionConflict collectionConflict = null; 
			if (status.getConflictingStageState().containsKey(HEADER_FILE_NAME)) {
				GitMarkupCollectionHeader gitCollectionHeader = resolveCollectionHeaderConflict(
					serializedCollectionHeaderFile, 
					status.getConflictingStageState().get(HEADER_FILE_NAME));
				
				serializedCollectionHeaderFile = 
					new SerializationHelper<GitMarkupCollectionHeader>().serialize(gitCollectionHeader);
				
				localGitRepoManager.add(
						collectionHeaderFile.getAbsoluteFile(), 
						serializedCollectionHeaderFile.getBytes(StandardCharsets.UTF_8));
				
				ContentInfoSet contentInfoSet = new ContentInfoSet(
						gitCollectionHeader.getAuthor(),
						gitCollectionHeader.getDescription(),
						gitCollectionHeader.getPublisher(),
						gitCollectionHeader.getName()
				);
				collectionConflict = 
						new CollectionConflict(
							projectId, collectionId, contentInfoSet, 
							gitCollectionHeader.getSourceDocumentId());

				collectionConflict.setHeaderConflict(true);
				status = localGitRepoManager.getStatus();
			}
			else {
				GitMarkupCollectionHeader gitCollectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
						.deserialize(
								serializedCollectionHeaderFile,
								GitMarkupCollectionHeader.class
						);
				ContentInfoSet contentInfoSet = new ContentInfoSet(
						gitCollectionHeader.getAuthor(),
						gitCollectionHeader.getDescription(),
						gitCollectionHeader.getPublisher(),
						gitCollectionHeader.getName()
				);
				collectionConflict = 
						new CollectionConflict(
							projectId, collectionId, contentInfoSet, 
							gitCollectionHeader.getSourceDocumentId());
			}			
			
			for (Entry<String, StageState> entry : status.getConflictingStageState().entrySet()) {
				String relativeAnnotationPathname = entry.getKey();
				String absAnnotationPathname = collectionSubmoduleAbsPath + "/" + relativeAnnotationPathname;

				StageState stageState = entry.getValue();
				
				
				switch (stageState) {
				case BOTH_MODIFIED: {
					String serializedConflictingAnnotation = FileUtils.readFileToString(
						new File(absAnnotationPathname), StandardCharsets.UTF_8);
					
					AnnotationConflict annotationConflict = 
							getBothModifiedAnnotationConflict(
								projectId, collectionId, serializedConflictingAnnotation);
					collectionConflict.addAnnotationConflict(annotationConflict);
					break;
				}
				case DELETED_BY_THEM: { // them is the user on the dev branch here

					// in this case the file comes from us (the team on the master branch)
					String serializedConflictingAnnotation = FileUtils.readFileToString(
							new File(absAnnotationPathname), StandardCharsets.UTF_8);
					
					AnnotationConflict annotationConflict = 
							getDeleteByThemAnnotationConflict(
								projectId, collectionId, serializedConflictingAnnotation);
					collectionConflict.addAnnotationConflict(annotationConflict);
					break;					
				}
				case DELETED_BY_US: { // us is the team on the master branch here
					
					// in this case the file comes from them (the user on the dev branch)
					String serializedConflictingAnnotation = FileUtils.readFileToString(
							new File(absAnnotationPathname), StandardCharsets.UTF_8);
					
					AnnotationConflict annotationConflict = 
							getDeleteByUsAnnotationConflict(
								projectId, collectionId, serializedConflictingAnnotation);
					collectionConflict.addAnnotationConflict(annotationConflict);
					break;						
				}				
				default: System.out.println("not handled"); //TODO:
				}
				
			}
			return collectionConflict;
		}		
	}
	
	private AnnotationConflict getDeleteByThemAnnotationConflict(
			String projectId, String collectionId, 
			String serializedConflictingAnnotation) throws Exception {
		
		JsonLdWebAnnotation masterVersionJsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
				.deserialize(
						serializedConflictingAnnotation,
						JsonLdWebAnnotation.class
				);
		
		List<TagReference> masterTagReferences = 
				masterVersionJsonLdWebAnnotation.toTagReferenceList(projectId, collectionId);

		AnnotationConflict annotationConflict = 
				new AnnotationConflict(
					null, Collections.emptyList(),
					masterTagReferences.get(0).getTagInstance(), masterTagReferences);
		
		return annotationConflict;		
	}

	private AnnotationConflict getDeleteByUsAnnotationConflict(
			String projectId, String collectionId, 
			String serializedConflictingAnnotation) throws Exception {
		
		JsonLdWebAnnotation devVersionJsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
				.deserialize(
						serializedConflictingAnnotation,
						JsonLdWebAnnotation.class
				);
		List<TagReference> devTagReferences = 
				devVersionJsonLdWebAnnotation.toTagReferenceList(projectId, collectionId);

		AnnotationConflict annotationConflict = 
				new AnnotationConflict(
					devTagReferences.get(0).getTagInstance(), devTagReferences,
					null, Collections.emptyList());
		
		return annotationConflict;		
	}

	private GitMarkupCollectionHeader resolveCollectionHeaderConflict(
			String serializedCollectionHeaderFile, StageState stageState) {
		
		if (stageState.equals(StageState.BOTH_MODIFIED)) {
			String masterVersion = serializedCollectionHeaderFile
					.replaceAll("\\Q<<<<<<< HEAD\\E(\\r\\n|\\r|\\n)", "")
					.replaceAll("\\Q=======\\E(\\r\\n|\\r|\\n|.)*?\\Q>>>>>>> \\E.+?(\\r\\n|\\r|\\n)", "");
				
			String devVersion = serializedCollectionHeaderFile
				.replaceAll("\\Q<<<<<<< HEAD\\E(\\r\\n|\\r|\\n|.)*?\\Q=======\\E(\\r\\n|\\r|\\n)", "")
				.replaceAll("\\Q>>>>>>> \\E.+?(\\r\\n|\\r|\\n)", "");
			
			GitMarkupCollectionHeader masterCollectionHeader = 
					new SerializationHelper<GitMarkupCollectionHeader>().deserialize(
							masterVersion, GitMarkupCollectionHeader.class);
			
			GitMarkupCollectionHeader devCollectionHeader = 
					new SerializationHelper<GitMarkupCollectionHeader>().deserialize(
							devVersion, GitMarkupCollectionHeader.class);
			
			String name = masterCollectionHeader.getName();
			if (!name.trim().toLowerCase().equals(devCollectionHeader.getName().trim().toLowerCase())) {
				name += " " + devCollectionHeader.getName();
			}
			
			String description = masterCollectionHeader.getDescription();
			if (!description.trim().toLowerCase().equals(devCollectionHeader.getDescription().trim().toLowerCase())) {
				description += " " + devCollectionHeader.getDescription();
			}
			
			return new GitMarkupCollectionHeader(
				name, description, 
				masterCollectionHeader.getSourceDocumentId(), // cannot change
				masterCollectionHeader.getSourceDocumentVersion() // cannot change yet
			);
		}
		else {
			 return new SerializationHelper<GitMarkupCollectionHeader>().deserialize(
					 serializedCollectionHeaderFile, GitMarkupCollectionHeader.class);
		}
	}
	
	private AnnotationConflict getBothModifiedAnnotationConflict(
			String projectId, String collectionId, String serializedConflictingAnnotation) throws Exception {

		String masterVersion = serializedConflictingAnnotation
			.replaceAll("\\Q<<<<<<< HEAD\\E(\\r\\n|\\r|\\n)", "")
			.replaceAll("\\Q=======\\E(\\r\\n|\\r|\\n|.)*?\\Q>>>>>>> \\E.+?(\\r\\n|\\r|\\n)", "");
		
		String devVersion = serializedConflictingAnnotation
			.replaceAll("\\Q<<<<<<< HEAD\\E(\\r\\n|\\r|\\n|.)*?\\Q=======\\E(\\r\\n|\\r|\\n)", "")
			.replaceAll("\\Q>>>>>>> \\E.+?(\\r\\n|\\r|\\n)", "");
				
		
		JsonLdWebAnnotation masterVersionJsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
				.deserialize(masterVersion, JsonLdWebAnnotation.class);

		JsonLdWebAnnotation devVersionJsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
				.deserialize(devVersion, JsonLdWebAnnotation.class);
		
		List<TagReference> masterTagReferences = 
			masterVersionJsonLdWebAnnotation.toTagReferenceList(projectId, collectionId);
		List<TagReference> devTagReferences = 
			devVersionJsonLdWebAnnotation.toTagReferenceList(projectId, collectionId);

		AnnotationConflict annotationConflict = 
				new AnnotationConflict(
					devTagReferences.get(0).getTagInstance(), devTagReferences,
					masterTagReferences.get(0).getTagInstance(), masterTagReferences);
		
		return annotationConflict;
	}

	public void rebaseToMaster(String projectId, String collectionId, String branch) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			String collectionGitRepositoryName =
					projectRootRepositoryName
							+ "/" + GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME
							+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);		
			localGitRepoManager.checkout(branch, false);
			localGitRepoManager.rebase(Constants.MASTER);
		}
	}

}
