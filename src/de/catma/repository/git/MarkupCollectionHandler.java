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
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.MarkupCollectionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagLibrary;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarkupCollectionHandler implements IMarkupCollectionHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private static final String MARKUPCOLLECTION_REPOSITORY_NAME_FORMAT = "%s_markupcollection";

	public static String getMarkupCollectionRepositoryName(String markupCollectionId) {
		return String.format(MARKUPCOLLECTION_REPOSITORY_NAME_FORMAT, markupCollectionId);
	}

	public MarkupCollectionHandler(ILocalGitRepositoryManager localGitRepositoryManager,
								   IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	/**
	 * Creates a new markup collection.
	 *
	 * @param projectId the ID of the project within which the new markup collection must be created
	 * @param markupCollectionId the ID of the new markup collection. If none is provided, a new ID will be
	 *                           generated.
	 * @param name the name of the new markup collection
	 * @param description the description of the new markup collection
	 * @param sourceDocumentId the ID of the source document to which the new markup collection relates
	 * @param sourceDocumentVersion the version of the source document to which the new markup collection relates
	 * @return the <code>markupCollectionId</code> if one was provided, otherwise a new markup collection ID
	 * @throws MarkupCollectionHandlerException if an error occurs while creating the markup collection
	 */
	@Override
	public String create(
			@Nonnull String projectId,
			@Nullable String markupCollectionId,
			@Nonnull String name,
			@Nullable String description,
			@Nonnull String sourceDocumentId,
			@Nonnull String sourceDocumentVersion
	) throws MarkupCollectionHandlerException {

		if (markupCollectionId == null) {
			IDGenerator idGenerator = new IDGenerator();
			markupCollectionId = idGenerator.generate();
		}

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the remote markup collection repository
			String markupCollectionRepoName = MarkupCollectionHandler.getMarkupCollectionRepositoryName(
					markupCollectionId
			);

			IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(
							markupCollectionRepoName, markupCollectionRepoName, projectId
					);

			// clone the repository locally
			// TODO: create a provider to retrieve the auth details so that we don't have to cast to the implementation
			GitLabServerManager gitLabServerManager = (GitLabServerManager)this.remoteGitServerManager;

			User gitLabUser = gitLabServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = gitLabServerManager.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
					createRepositoryResponse.repositoryHttpUrl,
					null,
					gitLabUser.getUsername(),
					gitLabUserImpersonationToken
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(localGitRepoManager.getRepositoryWorkTree(), "header.json");

			MarkupCollectionHeader header = new MarkupCollectionHeader(
					name, description, sourceDocumentId, sourceDocumentVersion
			);
			String serializedHeader = new SerializationHelper<MarkupCollectionHeader>().serialize(header);

			localGitRepoManager.addAndCommit(
					targetHeaderFile,
					serializedHeader.getBytes(StandardCharsets.UTF_8),
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
	public void delete(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws MarkupCollectionHandlerException {
		throw new MarkupCollectionHandlerException("Not implemented");
	}

	/**
	 * Adds an existing tagset, identified by <code>tagsetId</code> and <code>tagsetVersion</code>, to the markup
	 * collection identified by <code>markupCollectionId</code>.
	 *
	 * @param projectId the ID of the project that contains the markup collection to which the tagset should be added
	 * @param markupCollectionId the ID of the markup collection to add the tagset to
	 * @param tagsetId the ID of the tagset to add
	 * @param tagsetVersion the version of the tagset to add
	 * @throws MarkupCollectionHandlerException if an error occurs while adding the tagset
	 */
	@Override
	public void addTagset(@Nonnull String projectId,
						  @Nonnull String markupCollectionId,
						  @Nonnull String tagsetId,
						  @Nonnull String tagsetVersion
	) throws MarkupCollectionHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectRootRepositoryName);

			File targetMarkupCollectionHeaderFilePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					ProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					markupCollectionId,
					"header.json"
			).toFile();

			// update header.json
			SerializationHelper<MarkupCollectionHeader> serializationHelper = new SerializationHelper<>();
			MarkupCollectionHeader markupCollectionHeader = serializationHelper.deserialize(
					FileUtils.readFileToString(targetMarkupCollectionHeaderFilePath, StandardCharsets.UTF_8),
					MarkupCollectionHeader.class
			);

			markupCollectionHeader.addTagset(new AbstractMap.SimpleEntry<>(tagsetId, tagsetVersion));

			String serializedHeader = serializationHelper.serialize(markupCollectionHeader);

			// TODO: create a provider to retrieve the auth details so that we don't have to cast to the implementation
			GitLabServerManager gitLabServerManager = (GitLabServerManager)this.remoteGitServerManager;
			User gitLabUser = gitLabServerManager.getGitLabUser();

			localGitRepoManager.addAndCommit(
					targetMarkupCollectionHeaderFilePath, serializedHeader.getBytes(StandardCharsets.UTF_8),
					StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
					gitLabUser.getEmail()
			);
		}
		catch (LocalGitRepositoryManagerException|IOException e) {
			throw new MarkupCollectionHandlerException("Failed to add tagset", e);
		}
	}

	@Override
	public void removeTagset(@Nonnull String projectId, @Nonnull String markupCollectionId, @Nonnull String tagsetId)
			throws MarkupCollectionHandlerException {
		// it should only be possible to remove a tagset if there are no tag instances referring to any of its tag
		// definitions
		throw new MarkupCollectionHandlerException("Not implemented");
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
	 * @throws MarkupCollectionHandlerException if an error occurs while creating the tag instance
	 */
	@Override
	public String createTagInstance(
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull JsonLdWebAnnotation annotation
	) throws MarkupCollectionHandlerException {

		// TODO: check that the markup collection references the tagset for the tag instance being added
		// TODO: check that the tag instance is for the correct document
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the project root repo
			localGitRepoManager.open(ProjectHandler.getProjectRootRepositoryName(projectId));

			// write the serialized tag instance to the markup collection submodule
			File targetTagInstanceFilePath = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				String.format(
						"collections/%s/annotations/%s.json",
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
		catch (LocalGitRepositoryManagerException|IOException e) {
			throw new MarkupCollectionHandlerException("Failed to add tag instance", e);
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
			throws MarkupCollectionHandlerException {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());

		try {
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
		}
		catch (IOException|JsonLdWebAnnotationException e) {
			throw new MarkupCollectionHandlerException("Failed to open tag references", e);
		}

		return tagReferences;
	}

	@Override
	public UserMarkupCollection open(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws MarkupCollectionHandlerException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectRootRepositoryName);

			String markupCollectionSubmoduleName = String.format(
					"%s/%s", ProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
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

			MarkupCollectionHeader markupCollectionHeader = new SerializationHelper<MarkupCollectionHeader>()
					.deserialize(serializedMarkupCollectionHeaderFile, MarkupCollectionHeader.class);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					markupCollectionHeader.getAuthor(),
					markupCollectionHeader.getDescription(),
					markupCollectionHeader.getPublisher(),
					markupCollectionHeader.getName()
			);

			// we are hoping to get rid of tag libraries altogether
			TagLibrary tagLibrary = new TagLibrary(null, null);

			UserMarkupCollection userMarkupCollection = new UserMarkupCollection(
					null, markupCollectionId, contentInfoSet, tagLibrary, tagReferences, AccessMode.WRITE
			);
			userMarkupCollection.setRevisionHash(markupCollectionRevisionHash);

			return userMarkupCollection;
		}
		catch (LocalGitRepositoryManagerException|IOException e) {
			throw new MarkupCollectionHandlerException("Failed to open markup collection", e);
		}
	}
}
