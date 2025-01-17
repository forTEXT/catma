package de.catma.repository.git;

import de.catma.document.source.ContentInfoSet;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTagDefinition;
import de.catma.repository.git.serialization.models.GitTagsetHeader;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class GitTagsetHandler {
	private static final String HEADER_FILE_NAME = "header.json";

	private final LocalGitRepositoryManager localGitRepositoryManager;
	private final File projectDirectory;
	private final String username;
	private final String email;

	public GitTagsetHandler(
			LocalGitRepositoryManager localGitRepositoryManager,
			File projectDirectory,
			String username,
			String email
	) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.projectDirectory = projectDirectory;
		this.username = username;
		this.email = email;
	}

	/**
	 * Creates a tagset with the given parameters.
	 * The localGitRepositoryManager needs to be attached to a project Git
	 * repository!
	 * 
	 * @param tagsetFolder The folder of the new tagset.
	 * @param tagsetId The ID of the new tagset.
	 * @param name The name of the new tagset.
	 * @param description The description of the new tagset.
	 * @param forkedFromCommitURL An optional source URL when the tagset is based on a fork.
	 * @return the new revision hash of the project
	 * @throws IOException in case of errors
	 */
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
				String.format("Created tagset \"%s\" with ID %s", name, tagsetId),
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
	
	public TagsetDefinition getTagset(String tagsetId) throws IOException {

		String tagsetSubdir = String.format(
				"%s/%s", GitProjectHandler.TAGSETS_DIRECTORY_NAME, tagsetId
		);

		File tagsetHeaderFile = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				tagsetSubdir,
				HEADER_FILE_NAME
		).toFile();

		String serialized = FileUtils.readFileToString(tagsetHeaderFile, StandardCharsets.UTF_8);
		GitTagsetHeader gitTagsetHeader = new SerializationHelper<GitTagsetHeader>()
				.deserialize(
						serialized,
						GitTagsetHeader.class
				);

		TagsetDefinition tagsetDefinition = new TagsetDefinition(
			tagsetId, gitTagsetHeader.getName(), gitTagsetHeader.getDeletedDefinitions()
		);
		tagsetDefinition.setForkedFromCommitURL(gitTagsetHeader.getForkedFromCommitURL());
		tagsetDefinition.setResponsibleUser(gitTagsetHeader.getResponsibleUser());
		tagsetDefinition.setDescription(gitTagsetHeader.getDescription());
		ArrayList<TagDefinition> tagDefinitions = this.openTagDefinitions(tagsetHeaderFile.getParentFile());

		for(TagDefinition tagdefinition : tagDefinitions){
			tagsetDefinition.addTagDefinition(tagdefinition);
		}

		return tagsetDefinition;
	}

	public ContentInfoSet getContentInfoSet(String tagsetId) throws IOException {

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
	 * Moves a tag definition between one or multiple tagsets identified by <code>tagsetId</code>.
	 *
	 * @param tdFrom {@link TagDefinition} source
	 * @param tdTo {@link TagDefinition} destination
	 * @param commitMsg commit message
	 * @return the new project revision hash
	 * @throws IOException if an error occurs during the move of the tag definition
	 */
	public String moveTagDefinition(
			TagDefinition tdFrom,
			TagDefinition tdTo,
			String commitMsg
	) throws IOException {
		String pdj = "/propertydefs.json";
		String common =	String.format("%s/%s/", this.projectDirectory.getAbsolutePath(), GitProjectHandler.TAGSETS_DIRECTORY_NAME);
		String fromTagsetDir = String.format("%s/%s/", common, tdFrom.getTagsetDefinitionUuid());
		String toTagsetDir = String.format("%s/%s/", common, tdTo.getTagsetDefinitionUuid());

		/* This work like it seems: all the folders having subdirectories have a folder
		at the root of the tagset folder containing the subitems. This mean that the
		hiearchy isn't stored as it is in the UI. */
		String fromPath = fromTagsetDir +
			(StringUtils.isEmpty(tdFrom.getParentUuid()) ? "" : (tdFrom.getParentUuid() + "/"))
			+ tdFrom.getUuid();
		String toPath = toTagsetDir +
			(StringUtils.isEmpty(tdTo.getParentUuid()) ? "" : (tdTo.getParentUuid() + "/"))
			+ tdTo.getUuid();
		File from = Paths.get(fromPath).toFile();
		File to = Paths.get(toPath).toFile();
		if ((StringUtils.isEmpty(tdFrom.getParentUuid()) && StringUtils.isEmpty(tdTo.getParentUuid())) ||
			(!StringUtils.isEmpty(tdFrom.getParentUuid()) && !StringUtils.isEmpty(tdTo.getParentUuid()))) {
			/* if we move from an item in a tree somewhere to an item in a tree somewhere else, or if we
				move a parent around in the root of the hierarchy, its fine, we just move the folder
                                around */
			FileUtils.moveDirectory(from, to);
			this.localGitRepositoryManager.remove(Paths.get(fromPath + pdj).toFile());
			this.localGitRepositoryManager.remove(from);
		}
		if (StringUtils.isEmpty(tdFrom.getParentUuid()) && !StringUtils.isEmpty(tdTo.getParentUuid())) {
			/* if we moved from the root to an object lower in the hierarchy, we need to bring the children back down 
			(if it has any) */
			/* There shouldn't be a subdir with the name of the current tag UUID in its parent folder */
			to.mkdirs();
			FileUtils.moveFile(Paths.get(fromPath+pdj).toFile(), Paths.get(toPath+pdj).toFile());
			/* That leaves the children in the root of the tagset under the directory with the name of
                        their parent, which is what we wanted anyways */
			this.localGitRepositoryManager.remove(Paths.get(fromPath + pdj).toFile());
		}
		if (StringUtils.isEmpty(tdTo.getParentUuid()) && !StringUtils.isEmpty(tdFrom.getParentUuid())) {
			/* If we moved back to the root, our children are already in a directory called with our UUID at the root,
			we need to not squish them, so we only move the propertydefs there */
			FileUtils.moveFile(Paths.get(fromPath+pdj).toFile(), Paths.get(toPath+pdj).toFile());
			this.localGitRepositoryManager.remove(Paths.get(fromPath + pdj).toFile());
		}

		GitTagDefinition gitTagDefinition = new GitTagDefinition(tdTo);
		String serializedGitTagDefinition =
				new SerializationHelper<GitTagDefinition>().serialize(gitTagDefinition);

		String projectRevision = this.localGitRepositoryManager.addAndCommit(
			Paths.get(toPath + pdj).toFile(),
			serializedGitTagDefinition.getBytes(StandardCharsets.UTF_8),
			commitMsg,
			this.username,
			this.email);
		System.out.println("Deletion test commit:" + projectRevision);
		return projectRevision;
	}

	
	/**
	 * Creates a tag definition within the tagset identified by <code>tagsetId</code>.
	 *
	 * @param tagsetId the ID of the tagset within which to create the tag definition
	 * @param tagDefinition a {@link TagDefinition} object
	 * @param commitMsg a commit message
	 * @return the new project revision hash
	 * @throws IOException if an error occurs while creating the tag definition
	 */
	public String createOrUpdateTagDefinition(
			String tagsetId,
			TagDefinition tagDefinition,
			String commitMsg
	) throws IOException {

		/* This work like it seems: all the folders having subdirectories have a folder
		at the root of the tagset folder containing the subitems. This mean that the
		hiearchy isn't stored as it is in the UI. */
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

	public String removeTagDefinition(TagDefinition tagDefinition) 
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
		
		String targetTagDefinitionFolderRelativePath =
			(StringUtils.isEmpty(tagDefinition.getParentUuid()) ? "" : (tagDefinition.getParentUuid()+"/"))
			+ tagDefinition.getUuid();

		
		File targetTagDefinitionFolderAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				tagsetSubdir,
				targetTagDefinitionFolderRelativePath
		).toFile();
		
		String projectRevision = this.localGitRepositoryManager.removeAndCommit(
				targetTagDefinitionFolderAbsolutePath,
				!StringUtils.isEmpty(tagDefinition.getParentUuid()), // delete empty parent tag directory
				String.format(
					"Deleted tag \"%1$s\" with ID %2$s from tagset \"%3$s\" with ID %4$s, including corresponding annotations",
					tagDefinition.getName(), 
					tagDefinition.getUuid(),
					gitTagsetHeader.getName(),
					tagDefinition.getTagsetDefinitionUuid()),
				this.username,
				this.email);
				
		return projectRevision;
	}

	public String removePropertyDefinition(
			TagsetDefinition tagsetDefinition,
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
						tagsetDefinition.getResponsibleUser(),
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
			String.format(
				"Deleted property \"%1$s\" with ID %2$s from tag \"%3$s\" with ID %4$s in tagset \"%5$s\" with ID %6$s ",
				propertyDefinition.getName(), propertyDefinition.getUuid(),
				tagDefinition.getName(), tagDefinition.getUuid(),
				tagsetDefinition.getName(), tagsetDefinition.getUuid()),
			this.username,
			this.email);
		
			
		return projectRevision;
	}

	public String updateTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
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
						tagsetDefinition.getResponsibleUser(),
						tagsetDefinition.getForkedFromCommitURL(),
						new TreeSet<>(tagsetDefinition.getDeletedDefinitions()));
		String serializedHeader = new SerializationHelper<GitTagsetHeader>().serialize(header);
		
		this.localGitRepositoryManager.add(
				tagsetHeaderFile,
				serializedHeader.getBytes(StandardCharsets.UTF_8));

		String projectRevision = this.localGitRepositoryManager.addAndCommit(
				tagsetHeaderFile, 
				serializedHeader.getBytes(StandardCharsets.UTF_8), 
				String.format("Updated metadata of tagset \"%s\" with ID %s",
					tagsetDefinition.getName(), tagsetDefinition.getUuid()),
				this.username,
				this.email);

		return projectRevision;
	}


	public String removeTagsetDefinition(TagsetDefinition tagset) throws IOException {
		String tagsetSubdir = String.format(
				"%s/%s", 
				GitProjectHandler.TAGSETS_DIRECTORY_NAME, 
				tagset.getUuid()
		);

		File targetTagsetDefinitionFolderAbsolutePath = Paths.get(
				this.projectDirectory.getAbsolutePath(),
				tagsetSubdir
		).toFile();
		
		String projectRevision = this.localGitRepositoryManager.removeAndCommit(
				targetTagsetDefinitionFolderAbsolutePath,
				false, // do not delete the parent folder
				String.format(
					// TODO: append ", including corresponding annotations" once this is actually happening
					//       see TODO in GraphWorktreeProject.removeTagsetDefinition
					"Deleted tagset \"%s\" with ID %s",
					tagset.getName(), 
					tagset.getUuid()),
				this.username,
				this.email);
		
			
		return projectRevision;
		
	}

}
