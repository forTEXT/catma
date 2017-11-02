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
import de.catma.repository.git.serialization.models.HeaderBase;
import de.catma.repository.git.serialization.models.TagsetDefinitionHeader;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagsetHandler implements ITagsetHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private final IDGenerator idGenerator;

	private static final String TAGSET_REPOSITORY_NAME_FORMAT = "%s_tagset";

	public static String getTagsetRepositoryName(String tagsetId) {
		return String.format(TAGSET_REPOSITORY_NAME_FORMAT, tagsetId);
	}

	public static String getTagsetUuidFromRepositoryName(String tagsetRepositoryName) {
		Pattern pattern = Pattern.compile(TAGSET_REPOSITORY_NAME_FORMAT.replace("%s", "(.*)"));
		Matcher matcher = pattern.matcher(tagsetRepositoryName);
		matcher.matches();
		return matcher.group(1);
	}

	public TagsetHandler(ILocalGitRepositoryManager localGitRepositoryManager,
							 IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;

		this.idGenerator = new IDGenerator();
	}

	@Override
	public String create(String name, String description, Version version, String projectId) throws TagsetHandlerException {
		String tagsetId = idGenerator.generate();

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the tagset repository
			IRemoteGitServerManager.CreateRepositoryResponse response;

			// TODO: nice name for gitlab
			String tagsetRepoName = TagsetHandler.getTagsetRepositoryName(tagsetId);
			response = this.remoteGitServerManager.createRepository(
					tagsetRepoName, tagsetRepoName, projectId
			);

			// clone the repository locally
			GitLabServerManager gitLabServerManager =
					(GitLabServerManager)this.remoteGitServerManager;

			User gitLabUser = gitLabServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = gitLabServerManager
					.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
					response.repositoryHttpUrl,
					null,
					gitLabUser.getUsername(),
					gitLabUserImpersonationToken
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(
					localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);

			TagsetDefinitionHeader header = new TagsetDefinitionHeader(name, description, version);
			String serializedHeader = new SerializationHelper<HeaderBase>().serialize(header);
			byte[] headerBytes = serializedHeader.getBytes(StandardCharsets.UTF_8);

			localGitRepoManager.add(
					targetHeaderFile, headerBytes
			);

			// commit newly added files
			String commitMessage = String.format("Adding %s", targetHeaderFile.getName());
			String committerName = StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername();
			localGitRepoManager.commit(commitMessage, committerName, gitLabUser.getEmail());
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new TagsetHandlerException("Failed to create Tagset repo", e);
		}

		return tagsetId;
	}

	@Override
	public void delete(String tagsetId) throws TagsetHandlerException {
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
	public TagsetDefinition open(String tagsetId, String projectId) throws TagsetHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectRootRepositoryName);

			String tagsetRepositoryName = TagsetHandler.getTagsetRepositoryName(tagsetId);

			File tagsetHeaderFile = new File(
				localGitRepoManager.getRepositoryWorkTree(),
				String.format("tagsets/%s/header.json", tagsetRepositoryName)
			);

			String serialized = FileUtils.readFileToString(tagsetHeaderFile, StandardCharsets.UTF_8);
			TagsetDefinitionHeader tagsetDefinitionHeader = new SerializationHelper<TagsetDefinitionHeader>()
					.deserialize(
							serialized,
							TagsetDefinitionHeader.class
					);

			//Integer id, String uuid, String tagsetName, Version version
			TagsetDefinition tagsetdefinition = new TagsetDefinition(
				null, tagsetId, tagsetDefinitionHeader.getName(), tagsetDefinitionHeader.version()
			);

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

	@Override
	public String addTagDefinition(String tagsetId, TagDefinition tagDefinition) throws TagsetHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {

			localGitRepoManager.open(TagsetHandler.getTagsetRepositoryName(tagsetId));

			String propertyDefinitionPath = String.format("%s/%s", tagDefinition.getUuid(), "propertydefs.json");

			if(StringUtils.isNotEmpty(tagDefinition.getParentUuid())){
				propertyDefinitionPath = String.format("%s/%s", tagDefinition.getParentUuid(), propertyDefinitionPath);
			}

			// write header.json into the local repo
			File propertyDefFile = new File(
					localGitRepoManager.getRepositoryWorkTree(), propertyDefinitionPath
			);
			propertyDefFile.getParentFile().mkdirs();

			GitTagDefinition getTagDefinition = new GitTagDefinition(tagDefinition);
			String serializedTagDefinition = new SerializationHelper<GitTagDefinition>().serialize(getTagDefinition);
			byte[] propertyDefBytes = serializedTagDefinition.getBytes(StandardCharsets.UTF_8);

			// commit newly added files
			GitLabServerManager gitLabServerManager =
					(GitLabServerManager)this.remoteGitServerManager;

			User gitLabUser = gitLabServerManager.getGitLabUser();

			String committerName = StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername();
			localGitRepoManager.addAndCommit(
					propertyDefFile, propertyDefBytes, committerName, gitLabUser.getEmail()
			);

			return tagDefinition.getUuid();
		}
		catch (LocalGitRepositoryManagerException e) {
			throw new TagsetHandlerException("Failed to create add the TagDefinition to the repo", e);
		}
	}
}
