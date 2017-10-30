package de.catma.repository.git;

import de.catma.document.AccessMode;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IMarkupCollectionHandler;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.MarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagLibrary;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarkupCollectionHandler implements IMarkupCollectionHandler {
	static final String MARKUPCOLLECTION_ROOT_REPOSITORY_NAME_FORMAT = "%s_markupcollection";

	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	public MarkupCollectionHandler(ILocalGitRepositoryManager localGitRepositoryManager,
								   IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	String getMarkupCollectionRepoName(String markupCollectionId){
		return String.format(MARKUPCOLLECTION_ROOT_REPOSITORY_NAME_FORMAT, markupCollectionId);
	}

	/**
	 * Creates a new markup collection.
	 *
	 * @param name the name of the new markup collection
	 * @param description the description of the new markup collection
	 * @param sourceDocumentId the ID of the source document to which the new markup collection relates
	 * @param sourceDocumentVersion the version of the source document to which the new markup collection relates
	 * @param projectId the ID of the project within which the new markup collection must be created
	 * @param markupCollectionId the ID of the new markup collection. If none is provided, a new ID will be
	 *                           generated.
	 * @return the <code>markupCollectionId</code> if one was provided, otherwise a new markup collection ID
	 * @throws MarkupCollectionHandlerException if an error occurs while creating the markup collection
	 */
	@Override
	public String create(String name, String description, String sourceDocumentId, String sourceDocumentVersion,
						 String projectId, @Nullable String markupCollectionId)
			throws MarkupCollectionHandlerException {
		if (markupCollectionId == null) {
			IDGenerator idGenerator = new IDGenerator();
			markupCollectionId = idGenerator.generate();
		}

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the markup collection repository
			String markupCollectionRepoName = getMarkupCollectionRepoName(markupCollectionId);
			IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(
							markupCollectionRepoName, markupCollectionRepoName, projectId
					);

			// clone the repository locally
			RemoteGitServerManager remoteGitServerManagerImpl =
					(RemoteGitServerManager)this.remoteGitServerManager;
			User gitLabUser = remoteGitServerManagerImpl.getGitLabUser();
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl
					.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
				createRepositoryResponse.repositoryHttpUrl,
				null,
				gitLabUser.getUsername(),
				gitLabUserImpersonationToken
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(
				localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);
			MarkupCollectionHeader header = new MarkupCollectionHeader(
				name, description, sourceDocumentId, sourceDocumentVersion
			);
			String serializedHeader = new SerializationHelper<MarkupCollectionHeader>().serialize(header);
			localGitRepoManager.addAndCommit(
				targetHeaderFile, serializedHeader.getBytes(StandardCharsets.UTF_8),
				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
				gitLabUser.getEmail()
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new MarkupCollectionHandlerException("Failed to create markup collection", e);
		}

		return markupCollectionId;
	}

	@Override
	public void delete(String markupCollectionId) throws MarkupCollectionHandlerException {
		throw new MarkupCollectionHandlerException("Not implemented");
	}

	/**
	 * Adds an existing tagset, identified by <code>tagsetId</code> and <code>tagsetVersion</code>, to the markup
	 * collection identified by <code>markupCollectionId</code>.
	 *
	 * @param markupCollectionId the ID of the markup collection to add the tagset to
	 * @param tagsetId the ID of the tagset to add
	 * @param tagsetVersion the version of the tagset to add
	 * @throws MarkupCollectionHandlerException if an error occurs while adding the tagset
	 */
	@Override
	public void addTagset(String markupCollectionId, String tagsetId, String tagsetVersion)
			throws MarkupCollectionHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the markup collection repository
			localGitRepoManager.open(getMarkupCollectionRepoName(markupCollectionId));

			// update header.json
			File headerFile = new File(
				localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);
			SerializationHelper<MarkupCollectionHeader> serializationHelper = new SerializationHelper<>();
			MarkupCollectionHeader markupCollectionHeader = serializationHelper.deserialize(
				FileUtils.readFileToString(headerFile, StandardCharsets.UTF_8), MarkupCollectionHeader.class
			);
			markupCollectionHeader.addTagset(new AbstractMap.SimpleEntry<>(tagsetId, tagsetVersion));
			String serializedHeader = serializationHelper.serialize(markupCollectionHeader);

			RemoteGitServerManager remoteGitServerManagerImpl =
					(RemoteGitServerManager)this.remoteGitServerManager;
			User gitLabUser = remoteGitServerManagerImpl.getGitLabUser();

			localGitRepoManager.addAndCommit(
				headerFile, serializedHeader.getBytes(StandardCharsets.UTF_8),
				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
				gitLabUser.getEmail()
			);
		}
		catch (LocalGitRepositoryManagerException|IOException e) {
			throw new MarkupCollectionHandlerException("Failed to add tagset", e);
		}
	}

	@Override
	public void removeTagset(String markupCollectionId, String tagsetId) throws MarkupCollectionHandlerException {
		// it should only be possible to remove a tagset if there are no tag instances referring to any of its tag
		// definitions
		throw new MarkupCollectionHandlerException("Not implemented");
	}

	/**
	 * Adds a tag instance (annotation) to the markup collection identified by <code>markupCollectionId</code>.
	 *
	 * @param markupCollectionId the ID of the markup collection to add the tag instance to
	 * @param annotation a {@link JsonLdWebAnnotation} object representing the tag instance
	 * @throws MarkupCollectionHandlerException if an error occurs while adding the tag instance
	 */
	@Override
	public void addTagInstance(String markupCollectionId, JsonLdWebAnnotation annotation)
			throws MarkupCollectionHandlerException {
		// TODO: check that the markup collection references the tagset for the tag instance being added
		// TODO: check that the tag instance is for the correct document
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the markup collection repository
			localGitRepoManager.open(getMarkupCollectionRepoName(markupCollectionId));

			// write the serialized tag instance to the repository
			File targetSerializedTagInstanceFilePath = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				String.format("%s.json", annotation.getId().substring(annotation.getId().lastIndexOf("/")))
			);
			String serializedTagInstance = new SerializationHelper<JsonLdWebAnnotation>().serialize(annotation);

			RemoteGitServerManager remoteGitServerManagerImpl = (RemoteGitServerManager)this.remoteGitServerManager;
			User gitLabUser = remoteGitServerManagerImpl.getGitLabUser();

			localGitRepoManager.addAndCommit(
				targetSerializedTagInstanceFilePath,
				serializedTagInstance.getBytes(StandardCharsets.UTF_8),
				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
				gitLabUser.getEmail()
			);
		}
		catch (LocalGitRepositoryManagerException e) {
			throw new MarkupCollectionHandlerException("Failed to add tag instance", e);
		}
	}

	private boolean isTagInstanceFilename(String fileName){
		// TODO: split filename parts and check if it is a CATMA uuid
		return !fileName.equalsIgnoreCase("header.json");
	}

	private ArrayList<TagReference> openTagReferences(File parentDirectory) throws IOException, JsonLdWebAnnotationException {
		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());

		for(String item : contents){
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagDefinitions list
			if(target.isDirectory() && !target.getName().equalsIgnoreCase(".git")){
				tagReferences.addAll(this.openTagReferences(target));
				continue;
			}

			// if item is propertydefs.json, read it into a TagDefinition
			if(target.isFile() && isTagInstanceFilename(target.getName())){
				String serialized = FileUtils.readFileToString(target, StandardCharsets.UTF_8);
				JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
						.deserialize(
								serialized,
								JsonLdWebAnnotation.class
						);

				tagReferences.addAll(jsonLdWebAnnotation.toTagReferenceList());
			}
		}

		return tagReferences;
	}

	@Override
	public UserMarkupCollection open(String markupCollectionId)  throws MarkupCollectionHandlerException {

		// we are hoping to get rid of tag libraries altogether
		TagLibrary tagLibrary = new TagLibrary(null, "");



		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the markup collection repository
			localGitRepoManager.open(getMarkupCollectionRepoName(markupCollectionId));

			File repositoryWorkTreeFile = localGitRepoManager.getRepositoryWorkTree();

			ArrayList<TagReference> tagReferences = this.openTagReferences(repositoryWorkTreeFile);

			File targetHeaderFile = new File(
					localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);
			String serialized = FileUtils.readFileToString(targetHeaderFile, StandardCharsets.UTF_8);
			MarkupCollectionHeader header  = new SerializationHelper<MarkupCollectionHeader>()
					.deserialize(
							serialized,
							MarkupCollectionHeader.class
					);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					header.getAuthor(),
					header.getDescription(),
					header.getPublisher(),
					header.getName()
			);

			UserMarkupCollection markupCollection = new UserMarkupCollection(
				null, markupCollectionId, contentInfoSet, tagLibrary, tagReferences, AccessMode.WRITE
			);

			return markupCollection;
		}
		catch (LocalGitRepositoryManagerException | IOException | JsonLdWebAnnotationException e) {
			throw new MarkupCollectionHandlerException("Failed to open MarkupCollection repo", e);
		}
	}
}
