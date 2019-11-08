package de.catma.repository.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.google.common.collect.ArrayListMultimap;

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
			File targetHeaderFile = new File(localGitRepoManager.getRepositoryWorkTree(), "header.json");

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
	 * Adds an existing tagset, identified by <code>tagsetId</code> and <code>tagsetVersion</code>, to the markup
	 * collection identified by <code>markupCollectionId</code>.
	 *
	 * @param projectId the ID of the project that contains the markup collection to which the tagset should be added
	 * @param markupCollectionId the ID of the markup collection to add the tagset to
	 * @param tagsetId the ID of the tagset to add
	 * @param tagsetVersion the version of the tagset to add
	 * @throws IOException if an error occurs while adding the tagset
	 */
	public void addTagset(String projectId,
						  String markupCollectionId,
						  String tagsetId,
						  String tagsetVersion,
						  String commitMsg
	) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			File targetMarkupCollectionHeaderFilePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					GitProjectHandler.ANNOTATION_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					markupCollectionId,
					"header.json"
			).toFile();

			// update header.json
			SerializationHelper<GitMarkupCollectionHeader> serializationHelper = new SerializationHelper<>();
			GitMarkupCollectionHeader markupCollectionHeader = serializationHelper.deserialize(
					FileUtils.readFileToString(targetMarkupCollectionHeaderFilePath, StandardCharsets.UTF_8),
					GitMarkupCollectionHeader.class
			);

			markupCollectionHeader.addTagset(new AbstractMap.SimpleEntry<>(tagsetId, tagsetVersion));

			String serializedHeader = serializationHelper.serialize(markupCollectionHeader);


			localGitRepoManager.addAndCommit(
					targetMarkupCollectionHeaderFilePath, serializedHeader.getBytes(StandardCharsets.UTF_8),
					commitMsg,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
		}
	}

	public void removeTagset(String projectId, String markupCollectionId, String tagsetId)
			throws IOException {
		// it should only be possible to remove a tagset if there are no tag instances referring to any of its tag
		// definitions
		throw new UnsupportedOperationException("Not implemented");
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
				fileName.equalsIgnoreCase("header.json") || fileName.equalsIgnoreCase(".git")
		);
	}

	private ArrayList<TagReference> openTagReferences(
		String projectId, String markupCollectionId, File parentDirectory)
			throws Exception {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());

		for (String item : contents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(
					this.openTagReferences(projectId, markupCollectionId, target));
			}
			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			else if (target.isFile() && isTagInstanceFilename(target.getName())) {
				String serialized = FileUtils.readFileToString(target, StandardCharsets.UTF_8);
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
					"header.json"
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

	public AnnotationCollection getCollection(
			String projectId, String collectionId, TagLibrary tagLibrary)
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

			ArrayList<TagReference> tagReferences = this.openTagReferences(
					projectId, collectionId, markupCollectionSubmoduleAbsPath
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
					
					if (tagset.isDeleted(tagId)) {
						// Tag has been deleted, we remove the stale Annotation as well
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
			
			
			File markupCollectionHeaderFile = new File(
					markupCollectionSubmoduleAbsPath,
					"header.json"
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
			
			File targetHeaderFile = new File(localGitRepoManager.getRepositoryWorkTree(), "header.json");
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
			//TODO: handle header conflict
			File collectionHeaderFile = new File(
					collectionSubmoduleAbsPath,
					"header.json"
			);

			String serializedCollectionHeaderFile = FileUtils.readFileToString(
					collectionHeaderFile, StandardCharsets.UTF_8
			);

			GitMarkupCollectionHeader collectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
					.deserialize(serializedCollectionHeaderFile, GitMarkupCollectionHeader.class);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					collectionHeader.getAuthor(),
					collectionHeader.getDescription(),
					collectionHeader.getPublisher(),
					collectionHeader.getName()
			);
			CollectionConflict collectionConflict = 
					new CollectionConflict(
						projectId, collectionId, contentInfoSet, 
						collectionHeader.getSourceDocumentId());
			
			String collectionGitRepositoryName =
					projectRootRepositoryName + "/" + collectionSubmoduleRelDir;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			Status status = localGitRepositoryManager.getStatus();

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
				default: System.out.println("not handled"); //TODO:
				}
				
			}
			return collectionConflict;
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
