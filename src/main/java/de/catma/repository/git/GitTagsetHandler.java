package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.catma.document.source.ContentInfoSet;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTagDefinition;
import de.catma.repository.git.serialization.models.GitTagsetHeader;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

class GitTagsetHandler {
	private static final String HEADER_FILE_NAME = "header.json";
	
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final String username;
	private final String email;
	private final File projectDirectory;

	public GitTagsetHandler(
			ILocalGitRepositoryManager localGitRepositoryManager,
			File projectDirectory,
			String username, String email) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.projectDirectory = projectDirectory;
		this.username = username;
		this.email = email;
	}


	public String create(File tagsetFolder,
						 String tagsetId,
						 String name,
						 String description,
						 String forkedFromCommitURL
	) throws IOException {
			
		tagsetFolder.mkdirs();
		
		// write header.json into the local repo
		File targetHeaderFile = new File(tagsetFolder, HEADER_FILE_NAME);

		GitTagsetHeader header = 
				new GitTagsetHeader(
						name, description, 
						this.username, 
						forkedFromCommitURL,
						new TreeSet<>());
		String serializedHeader = new SerializationHelper<GitTagsetHeader>().serialize(header);

		String revisionHash = this.localGitRepositoryManager.addAndCommit(
				targetHeaderFile,
				serializedHeader.getBytes(StandardCharsets.UTF_8),
				String.format("Added Tagset %1$s with ID %2$s", name, tagsetId),
				this.username,
				this.email
		);
		
		return revisionHash;
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
	
	public TagsetDefinition getTagset(String projectId, String tagsetId) throws IOException {

		String tagsetSubdir = String.format(
				"%s/%s", GitProjectHandler.TAGSETS_DIRECTORY_NAME, tagsetId
		);

		File tagsetHeaderFile = new File(
			this.projectDirectory,
			tagsetSubdir + "/" + HEADER_FILE_NAME
		);

		String serialized = FileUtils.readFileToString(tagsetHeaderFile, StandardCharsets.UTF_8);
		GitTagsetHeader gitTagsetHeader = new SerializationHelper<GitTagsetHeader>()
				.deserialize(
						serialized,
						GitTagsetHeader.class
				);

		TagsetDefinition tagsetdefinition = new TagsetDefinition(
			tagsetId, gitTagsetHeader.getName(), gitTagsetHeader.getDeletedDefinitions()
		);

		ArrayList<TagDefinition> tagDefinitions = this.openTagDefinitions(tagsetHeaderFile.getParentFile());

		for(TagDefinition tagdefinition : tagDefinitions){
			tagsetdefinition.addTagDefinition(tagdefinition);
		}

		return tagsetdefinition;
	}

	public ContentInfoSet getContentInfoSet(String projectId, String tagsetId) throws IOException {

		String tagsetSubdir = String.format(
				"%s/%s", GitProjectHandler.TAGSETS_DIRECTORY_NAME, tagsetId
		);

		File tagsetHeaderFile = new File(
			this.projectDirectory,
			tagsetSubdir + "/" + HEADER_FILE_NAME
		);

		String serialized = FileUtils.readFileToString(tagsetHeaderFile, StandardCharsets.UTF_8);
		GitTagsetHeader gitTagsetHeader = new SerializationHelper<GitTagsetHeader>()
				.deserialize(
						serialized,
						GitTagsetHeader.class
				);

		ContentInfoSet contentInfoSet = new ContentInfoSet();
		contentInfoSet.setTitle(gitTagsetHeader.getName());

		return contentInfoSet;
	}
	
	/**
	 * Creates a tag definition within the tagset identified by <code>tagsetId</code>.
	 *
	 * @param projectId the ID of the project that contains the tagset within which the tag definition should be created
	 * @param tagsetId the ID of the tagset within which to create the tag definition
	 * @param tagDefinition a {@link TagDefinition} object
	 * @param commitMsg a commit message
	 * @return the new project revision hash
	 * @throws IOException if an error occurs while creating the tag definition
	 */
	public String createOrUpdateTagDefinition(
			String projectId,
			String tagsetId,
			TagDefinition tagDefinition,
			String commitMsg
	) throws IOException {

		String targetPropertyDefinitionsFileRelativePath =
			(StringUtils.isEmpty(tagDefinition.getParentUuid()) ? "" : (tagDefinition.getParentUuid() + "/"))
			+ tagDefinition.getUuid()
			+ "/propertydefs.json";
		
		String tagsetSubdir = String.format(
				"%s/%s", GitProjectHandler.TAGSETS_DIRECTORY_NAME, tagsetId
		);

		File targetPropertyDefinitionsFileAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				tagsetSubdir,
				targetPropertyDefinitionsFileRelativePath
		).toFile();

		GitTagDefinition gitTagDefinition = new GitTagDefinition(tagDefinition);
		String serializedGitTagDefinition = 
				new SerializationHelper<GitTagDefinition>().serialize(gitTagDefinition);

		String projectRevision = this.localGitRepositoryManager.addAndCommit(
			targetPropertyDefinitionsFileAbsolutePath, 
			serializedGitTagDefinition.getBytes(StandardCharsets.UTF_8), 
			commitMsg,
			this.username,
			this.email);
		
		return projectRevision;
	}

	public String removeTagDefinition(String projectId, TagDefinition tagDefinition) 
			throws IOException {
		
		String tagsetSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.TAGSETS_DIRECTORY_NAME, 
				tagDefinition.getTagsetDefinitionUuid()
		);

		// write header.json with deletion journal
		File tagsetHeaderFile = new File(
			this.projectDirectory,
			tagsetSubdir + "/" + HEADER_FILE_NAME
		);
		
		String serializedTagsetHeader = FileUtils.readFileToString(tagsetHeaderFile, StandardCharsets.UTF_8);
		GitTagsetHeader gitTagsetHeader = new SerializationHelper<GitTagsetHeader>()
				.deserialize(
						serializedTagsetHeader,
						GitTagsetHeader.class
				);

		serializedTagsetHeader = new SerializationHelper<GitTagsetHeader>().serialize(gitTagsetHeader);
		gitTagsetHeader.getDeletedDefinitions().add(tagDefinition.getUuid());
		
		serializedTagsetHeader = 
				new SerializationHelper<GitTagsetHeader>().serialize(gitTagsetHeader);
		
		this.localGitRepositoryManager.add(
				tagsetHeaderFile,
				serializedTagsetHeader.getBytes(StandardCharsets.UTF_8));
		
		String targetTagDefinitionsFolderRelativePath = 
			(StringUtils.isEmpty(tagDefinition.getParentUuid()) ? "" : (tagDefinition.getParentUuid()+"/"))
			+ tagDefinition.getUuid();

		
		File targetTagDefinitionsFolderAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				tagsetSubdir,
				targetTagDefinitionsFolderRelativePath
		).toFile();
		
		String projectRevision = this.localGitRepositoryManager.removeAndCommit(
				targetTagDefinitionsFolderAbsolutePath, 
				!StringUtils.isEmpty(tagDefinition.getParentUuid()), // delete empty parent tag directory
				String.format(
					"Removing Tag %1$s with ID %2$s", 
					tagDefinition.getName(), 
					tagDefinition.getUuid()),
				this.username,
				this.email);
				
		return projectRevision;
	}

	public String removePropertyDefinition(
			String projectId, TagsetDefinition tagsetDefinition,
			TagDefinition tagDefinition, PropertyDefinition propertyDefinition) 
		throws IOException {
		
		String tagsetSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.TAGSETS_DIRECTORY_NAME, 
				tagDefinition.getTagsetDefinitionUuid()
		);

		// write header.json with deletion journal
		File tagsetHeaderFile = new File(
			this.projectDirectory,
			tagsetSubdir + "/" + HEADER_FILE_NAME
		);
		
		GitTagsetHeader header = 
				new GitTagsetHeader(
						tagsetDefinition.getName(), 
						tagsetDefinition.getDescription(),
						tagsetDefinition.getResponsableUser(),
						tagsetDefinition.getForkedFromCommitURL(),
						new TreeSet<>(tagsetDefinition.getDeletedDefinitions()));
		String serializedHeader = 
				new SerializationHelper<GitTagsetHeader>().serialize(header);
		
		this.localGitRepositoryManager.add(
				tagsetHeaderFile,
				serializedHeader.getBytes(StandardCharsets.UTF_8));
		
		String targetPropertyDefinitionsFileRelativePath =
				(StringUtils.isEmpty(tagDefinition.getParentUuid()) ? "" : (tagDefinition.getParentUuid() + "/"))
				+ tagDefinition.getUuid()
				+ "/propertydefs.json";
		
		File targetPropertyDefinitionsFileAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				tagsetSubdir,
				targetPropertyDefinitionsFileRelativePath
		).toFile();

		GitTagDefinition gitTagDefinition = new GitTagDefinition(tagDefinition);
		String serializedGitTagDefinition = 
				new SerializationHelper<GitTagDefinition>().serialize(gitTagDefinition);

		String projectRevision = this.localGitRepositoryManager.addAndCommit(
			targetPropertyDefinitionsFileAbsolutePath, 
			serializedGitTagDefinition.getBytes(StandardCharsets.UTF_8), 
			String.format("Removed Property Definition %1$s with ID %2$s from Tag %3$s with ID %4$s",
				propertyDefinition.getName(), propertyDefinition.getUuid(),
				tagDefinition.getName(), tagDefinition.getUuid()),
			this.username,
			this.email);
		
			
		return projectRevision;
	}

	public String updateTagsetDefinition(String projectId, TagsetDefinition tagsetDefinition) throws Exception {
		String tagsetSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.TAGSETS_DIRECTORY_NAME, 
				tagsetDefinition.getUuid()
		);

		File tagsetHeaderFile = new File(
			this.projectDirectory,
			tagsetSubdir + "/" + HEADER_FILE_NAME
		);
		GitTagsetHeader header = 
				new GitTagsetHeader(
						tagsetDefinition.getName(), 
						tagsetDefinition.getDescription(),
						tagsetDefinition.getResponsableUser(),
						tagsetDefinition.getForkedFromCommitURL(),
						new TreeSet<>(tagsetDefinition.getDeletedDefinitions()));
		String serializedHeader = new SerializationHelper<GitTagsetHeader>().serialize(header);
		
		this.localGitRepositoryManager.add(
				tagsetHeaderFile,
				serializedHeader.getBytes(StandardCharsets.UTF_8));

		String projectRevision = this.localGitRepositoryManager.addAndCommit(
				tagsetHeaderFile, 
				serializedHeader.getBytes(StandardCharsets.UTF_8), 
				String.format("Updated metadata of Tagset %1$s with ID %2$s", 
					tagsetDefinition.getName(), tagsetDefinition.getUuid()),
				this.username,
				this.email);

		return projectRevision;
	}

}
