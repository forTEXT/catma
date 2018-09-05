<<<<<<< HEAD:src/main/java/de/catma/repository/git/GitMarkupCollectionHandler.java
package de.catma.repository.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import de.catma.document.AccessMode;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagLibrary;

public class GitMarkupCollectionHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private static final String MARKUPCOLLECTION_REPOSITORY_NAME_FORMAT = "%s_markupcollection";

	public static String getMarkupCollectionRepositoryName(String markupCollectionId) {
		return String.format(MARKUPCOLLECTION_REPOSITORY_NAME_FORMAT, markupCollectionId);
	}

	public GitMarkupCollectionHandler(ILocalGitRepositoryManager localGitRepositoryManager,
									  IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	/**
	 * Creates a new markup collection.
	 * <p>
	 * NB: You probably don't want to call this method directly (it doesn't create the submodule in the project root
	 * repo). Instead call the <code>createMarkupCollection</code> method of the {@link GitProjectManager} class.
	 *
	 * @param projectId the ID of the project within which the new markup collection must be created
	 * @param markupCollectionId the ID of the new markup collection. If none is provided, a new ID will be
	 *                           generated.
	 * @param name the name of the new markup collection
	 * @param description the description of the new markup collection
	 * @param sourceDocumentId the ID of the source document to which the new markup collection relates
	 * @param sourceDocumentVersion the version of the source document to which the new markup collection relates
	 * @return the Collection's revisionHash
	 * @throws IOException if an error occurs while creating the markup collection
	 */
	public String create(
			@Nonnull String projectId,
			@Nullable String markupCollectionId,
			@Nonnull String name,
			@Nullable String description,
			@Nonnull String sourceDocumentId,
			@Nonnull String sourceDocumentVersion
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the remote markup collection repository
			String markupCollectionRepoName = GitMarkupCollectionHandler.getMarkupCollectionRepositoryName(
					markupCollectionId
			);

			IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(
							markupCollectionRepoName, markupCollectionRepoName, projectId
					);

			// clone the repository locally
			localGitRepoManager.clone(
					projectId,
					createRepositoryResponse.repositoryHttpUrl,
					null,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
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
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
			
			
		}

	}

	public void delete(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws IOException {
		throw new UnsupportedOperationException("Not implemented");
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
	public void addTagset(@Nonnull String projectId,
						  @Nonnull String markupCollectionId,
						  @Nonnull String tagsetId,
						  @Nonnull String tagsetVersion
	) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			File targetMarkupCollectionHeaderFilePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
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
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
		}
	}

	public void removeTagset(@Nonnull String projectId, @Nonnull String markupCollectionId, @Nonnull String tagsetId)
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
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull JsonLdWebAnnotation annotation
	) throws IOException {

		// TODO: check that the markup collection references the tagset for the tag instance being added
		// TODO: check that the tag instance is for the correct document
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// write the serialized tag instance to the markup collection submodule
			File targetTagInstanceFilePath = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				String.format(
						"%s/%s/annotations/%s.json",
						GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
						markupCollectionId,
						annotation.getTagInstanceUuid()
				)
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

	private ArrayList<TagReference> openTagReferences(String projectId, String markupCollectionId, File parentDirectory)
			throws IOException {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());

		for (String item : contents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(this.openTagReferences(projectId, markupCollectionId, target));
				continue;
			}

			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			if (target.isFile() && isTagInstanceFilename(target.getName())) {
				String serialized = FileUtils.readFileToString(target, StandardCharsets.UTF_8);
				JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
						.deserialize(serialized, JsonLdWebAnnotation.class);

				tagReferences.addAll(
					jsonLdWebAnnotation.toTagReferenceList(
						projectId, markupCollectionId, this.localGitRepositoryManager, this.remoteGitServerManager
					)
				);
			}
		}

		return tagReferences;
	}
	
	public UserMarkupCollectionReference getUserMarkupCollectionReference(String projectId, String markupCollectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleName = String.format(
					"%s/%s", GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
			);

			File markupCollectionSubmodulePath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleName
			);

			String markupCollectionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					markupCollectionSubmoduleName
			);

			localGitRepoManager.detach();  // can't call open on an attached instance

			File markupCollectionHeaderFile = new File(
					markupCollectionSubmodulePath,
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

			return  new UserMarkupCollectionReference(
				markupCollectionId, markupCollectionRevisionHash, 
				contentInfoSet, markupCollectionHeader.getSourceDocumentId(), "TODO"); //TODO
		}

	}

	public UserMarkupCollection open(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleName = String.format(
					"%s/%s", GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
			);

			File markupCollectionSubmodulePath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleName
			);

			String markupCollectionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					markupCollectionSubmoduleName
			);

			localGitRepoManager.detach();  // can't call open on an attached instance

			ArrayList<TagReference> tagReferences = this.openTagReferences(
					projectId, markupCollectionId, markupCollectionSubmodulePath
			);

			File markupCollectionHeaderFile = new File(
					markupCollectionSubmodulePath,
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

			TagLibrary tagLibrary = new TagLibrary(markupCollectionId, contentInfoSet.getTitle());

			UserMarkupCollection userMarkupCollection = new UserMarkupCollection(
					markupCollectionId, contentInfoSet, tagLibrary, tagReferences, AccessMode.WRITE
			);
			userMarkupCollection.setRevisionHash(markupCollectionRevisionHash);

			return userMarkupCollection;
		}
	}

	public void deleteTagInstance(String projectId, String collectionId, String deletedTagInstanceId) throws IOException {
		// TODO: check that the tag instance is for the correct document
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));

			// write the serialized tag instance to the markup collection submodule
			File targetTagInstanceFilePath = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				String.format(
						"%s/%s/annotations/%s.json",
						GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
						collectionId,
						deletedTagInstanceId
				)
			);
			if (!targetTagInstanceFilePath.delete()) {
				throw new IOException(String.format("Could not delete annotation %s", targetTagInstanceFilePath.toString()));
			}
			
			// not doing Git add/commit
		}
	}
}
=======
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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import de.catma.document.AccessMode;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagLibrary;

public class GitMarkupCollectionHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private static final String MARKUPCOLLECTION_REPOSITORY_NAME_FORMAT = "%s_markupcollection";

	public static String getMarkupCollectionRepositoryName(String markupCollectionId) {
		return String.format(MARKUPCOLLECTION_REPOSITORY_NAME_FORMAT, markupCollectionId);
	}

	public GitMarkupCollectionHandler(ILocalGitRepositoryManager localGitRepositoryManager,
									  IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	/**
	 * Creates a new markup collection.
	 * <p>
	 * NB: You probably don't want to call this method directly (it doesn't create the submodule in the project root
	 * repo). Instead call the <code>createMarkupCollection</code> method of the {@link GitProjectManager} class.
	 *
	 * @param projectId the ID of the project within which the new markup collection must be created
	 * @param markupCollectionId the ID of the new markup collection. If none is provided, a new ID will be
	 *                           generated.
	 * @param name the name of the new markup collection
	 * @param description the description of the new markup collection
	 * @param sourceDocumentId the ID of the source document to which the new markup collection relates
	 * @param sourceDocumentVersion the version of the source document to which the new markup collection relates
	 * @return the Collection's revisionHash
	 * @throws IOException if an error occurs while creating the markup collection
	 */
	public String create(
			@Nonnull String projectId,
			@Nullable String markupCollectionId,
			@Nonnull String name,
			@Nullable String description,
			@Nonnull String sourceDocumentId,
			@Nonnull String sourceDocumentVersion
	) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the remote markup collection repository
			String markupCollectionRepoName = GitMarkupCollectionHandler.getMarkupCollectionRepositoryName(
					markupCollectionId
			);

			IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(
							markupCollectionRepoName, markupCollectionRepoName, projectId
					);

			// clone the repository locally
			localGitRepoManager.clone(
					projectId,
					createRepositoryResponse.repositoryHttpUrl,
					null,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
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
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
			
			
		}

	}

	public void delete(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws IOException {
		throw new UnsupportedOperationException("Not implemented");
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
	public void addTagset(@Nonnull String projectId,
						  @Nonnull String markupCollectionId,
						  @Nonnull String tagsetId,
						  @Nonnull String tagsetVersion
	) throws IOException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			File targetMarkupCollectionHeaderFilePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
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
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail()
			);
		}
	}

	public void removeTagset(@Nonnull String projectId, @Nonnull String markupCollectionId, @Nonnull String tagsetId)
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
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull JsonLdWebAnnotation annotation
	) throws IOException {

		// TODO: check that the markup collection references the tagset for the tag instance being added
		// TODO: check that the tag instance is for the correct document
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			
			String collectionGitRepositoryName = 
					projectRootRepositoryName 
					+ "/" + GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME 
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

	private ArrayList<TagReference> openTagReferences(String projectId, String markupCollectionId, File parentDirectory)
			throws IOException {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());

		for (String item : contents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(this.openTagReferences(projectId, markupCollectionId, target));
				continue;
			}

			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			if (target.isFile() && isTagInstanceFilename(target.getName())) {
				String serialized = FileUtils.readFileToString(target, StandardCharsets.UTF_8);
				JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
						.deserialize(serialized, JsonLdWebAnnotation.class);

				tagReferences.addAll(
					jsonLdWebAnnotation.toTagReferenceList(
						projectId, markupCollectionId, this.localGitRepositoryManager, this.remoteGitServerManager
					)
				);
			}
		}

		return tagReferences;
	}
	
	public UserMarkupCollectionReference getUserMarkupCollectionReference(String projectId, String markupCollectionId) throws Exception {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleName = String.format(
					"%s/%s", GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
			);

			File markupCollectionSubmodulePath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleName
			);

			String markupCollectionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					markupCollectionSubmoduleName
			);

			localGitRepoManager.detach();  // can't call open on an attached instance

			File markupCollectionHeaderFile = new File(
					markupCollectionSubmodulePath,
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

			return  new UserMarkupCollectionReference(
				markupCollectionId, markupCollectionRevisionHash, 
				contentInfoSet, markupCollectionHeader.getSourceDocumentId(), "TODO"); //TODO
		}

	}

	public UserMarkupCollection open(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectId, projectRootRepositoryName);

			String markupCollectionSubmoduleName = String.format(
					"%s/%s", GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
			);

			File markupCollectionSubmodulePath = new File(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					markupCollectionSubmoduleName
			);

			String markupCollectionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(
					markupCollectionSubmoduleName
			);

			localGitRepoManager.detach();  // can't call open on an attached instance

			ArrayList<TagReference> tagReferences = this.openTagReferences(
					projectId, markupCollectionId, markupCollectionSubmodulePath
			);

			File markupCollectionHeaderFile = new File(
					markupCollectionSubmodulePath,
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

			TagLibrary tagLibrary = new TagLibrary(markupCollectionId, contentInfoSet.getTitle());

			UserMarkupCollection userMarkupCollection = new UserMarkupCollection(
					markupCollectionId, contentInfoSet, tagLibrary, tagReferences, AccessMode.WRITE
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
					+ "/" + GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME 
					+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);

			return localGitRepoManager.commit(
					message,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail());
		}
	}

	public String removeTagInstancesAndCommit(
		String projectId, String collectionId, Collection<String> deletedTagInstanceIds, String commitMsg) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			
			String collectionGitRepositoryName = 
					projectRootRepositoryName 
					+ "/" + GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME 
					+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);
			
			removeTagInstances(localGitRepoManager, deletedTagInstanceIds);

			if (localGitRepoManager.hasUncommitedChanges()) {
				return localGitRepoManager.commit(
						commitMsg,
						remoteGitServerManager.getUsername(),
						remoteGitServerManager.getEmail());

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
					+ "/" + GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME 
					+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);
			
			removeTagInstances(localGitRepoManager, deletedTagInstanceIds);

			// not doing Git add/commit
		}
	}

	public String addAndCommitChanges(String projectId, String collectionId, String commitMsg) throws IOException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			
			String collectionGitRepositoryName = 
					projectRootRepositoryName 
					+ "/" + GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME 
					+ "/" + collectionId;

			localGitRepoManager.open(projectId, collectionGitRepositoryName);
		
			return localGitRepoManager.addAllAndCommit(
					commitMsg, 					
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail());
		}
	}
}
>>>>>>> origin/catma6_mp:src/main/java/de/catma/repository/git/GitMarkupCollectionHandler.java
