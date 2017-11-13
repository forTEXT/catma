package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.ITagsetHandler;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTagDefinition;
import de.catma.repository.git.serialization.models.TagsetDefinitionHeader;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagsetHandler implements ITagsetHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private static final String TAGSET_REPOSITORY_NAME_FORMAT = "%s_tagset";

	public static String getTagsetRepositoryName(String tagsetId) {
		return String.format(TAGSET_REPOSITORY_NAME_FORMAT, tagsetId);
	}

	public TagsetHandler(ILocalGitRepositoryManager localGitRepositoryManager,
						 IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	@Override
	public String create(@Nonnull String projectId,
						 @Nullable String tagsetId,
						 @Nonnull String name,
						 @Nullable String description
	) throws TagsetHandlerException {

		if (tagsetId == null) {
			IDGenerator idGenerator = new IDGenerator();
			tagsetId = idGenerator.generate();
		}

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the remote tagset repository
			String tagsetRepoName = TagsetHandler.getTagsetRepositoryName(tagsetId);

			IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(tagsetRepoName, tagsetRepoName, projectId);

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

			TagsetDefinitionHeader header = new TagsetDefinitionHeader(name, description);
			String serializedHeader = new SerializationHelper<TagsetDefinitionHeader>().serialize(header);

			localGitRepoManager.addAndCommit(
					targetHeaderFile,
					serializedHeader.getBytes(StandardCharsets.UTF_8),
					StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
					gitLabUser.getEmail()
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new TagsetHandlerException("Failed to create tagset", e);
		}

		return tagsetId;
	}

	@Override
	public void delete(@Nonnull String projectId, @Nonnull String tagsetId) throws TagsetHandlerException {
		throw new TagsetHandlerException("Not implemented");
	}

	private ArrayList<TagDefinition> openTagDefinitions(File parentDirectory) throws IOException {
		ArrayList<TagDefinition> tagDefinitions = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());

		for(String item : contents){
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagDefinitions list
			if(target.isDirectory() && !target.getName().equalsIgnoreCase(".git")){
				tagDefinitions.addAll(this.openTagDefinitions(target));
				continue;
			}

			// if item is propertydefs.json, read it into a TagDefinition
			if(target.isFile() && target.getName().equalsIgnoreCase("propertydefs.json")){
				String serialized = FileUtils.readFileToString(target, StandardCharsets.UTF_8);
				GitTagDefinition gitTagDefinition = new SerializationHelper<GitTagDefinition>()
						.deserialize(
								serialized,
								GitTagDefinition.class
						);

				tagDefinitions.add(gitTagDefinition.getTagDefinition());
			}
		}

		return tagDefinitions;
	}

	@Override
	public TagsetDefinition open(@Nonnull String projectId, @Nonnull String tagsetId) throws TagsetHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectRootRepositoryName);

			String tagsetSubmoduleName = String.format(
					"%s/%s", ProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetId
			);

			File tagsetHeaderFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				String.format("%s/header.json", tagsetSubmoduleName, tagsetId)
			);

			String serialized = FileUtils.readFileToString(tagsetHeaderFile, StandardCharsets.UTF_8);
			TagsetDefinitionHeader tagsetDefinitionHeader = new SerializationHelper<TagsetDefinitionHeader>()
					.deserialize(
							serialized,
							TagsetDefinitionHeader.class
					);

			TagsetDefinition tagsetdefinition = new TagsetDefinition(
				null, tagsetId, tagsetDefinitionHeader.getName(), null
			);
			String tagsetDefinitionRevisionHash = localGitRepoManager.getSubmoduleHeadRevisionHash(tagsetSubmoduleName);
			tagsetdefinition.setRevisionHash(tagsetDefinitionRevisionHash);

			ArrayList<TagDefinition> tagDefinitions = this.openTagDefinitions(tagsetHeaderFile.getParentFile());

			for(TagDefinition tagdefinition : tagDefinitions){
				tagsetdefinition.addTagDefinition(tagdefinition);
			}

			return tagsetdefinition;
		}
		catch (LocalGitRepositoryManagerException | IOException e) {
			throw new TagsetHandlerException("Failed to open the Tagset repo", e);
		}
	}

	/**
	 * Creates a tag definition within the tagset identified by <code>tagsetId</code>.
	 * <p>
	 * NB: This method purposefully does NOT perform any Git add/commit operations as it is expected to be called a very
	 * large number of times when a graph worktree is written to disk.
	 *
	 * @param projectId the ID of the project that contains the tagset within which the tag definition should be created
	 * @param tagsetId the ID of the tagset within which to create the tag definition
	 * @param tagDefinition a {@link TagDefinition} object
	 * @return the tag definition UUID
	 * @throws TagsetHandlerException if an error occurs while creating the tag definition
	 */
	@Override
	public String createTagDefinition(@Nonnull String projectId,
									  @Nonnull String tagsetId,
									  @Nonnull TagDefinition tagDefinition
	) throws TagsetHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectRootRepositoryName);

			String targetPropertyDefinitionsFileRelativePath = String.format(
					"%s%s/%s",
					StringUtils.isEmpty(tagDefinition.getParentUuid()) ? "" : String.format(
							"%s/", tagDefinition.getParentUuid()
					),
					tagDefinition.getUuid(),
					"propertydefs.json"
			);

			File targetPropertyDefinitionsFileAbsolutePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					ProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME,
					tagsetId,
					targetPropertyDefinitionsFileRelativePath
			).toFile();

			GitTagDefinition gitTagDefinition = new GitTagDefinition(tagDefinition);
			String serializedGitTagDefinition = new SerializationHelper<GitTagDefinition>().serialize(gitTagDefinition);

			try (FileOutputStream fileOutputStream = FileUtils.openOutputStream(
					targetPropertyDefinitionsFileAbsolutePath
			)) {
				fileOutputStream.write(serializedGitTagDefinition.getBytes(StandardCharsets.UTF_8));
			}

			// not doing Git add/commit, see method doc comment
		}
		catch (LocalGitRepositoryManagerException|IOException e) {
			throw new TagsetHandlerException("Failed to add tag definition", e);
		}

		return tagDefinition.getUuid();
	}
}
