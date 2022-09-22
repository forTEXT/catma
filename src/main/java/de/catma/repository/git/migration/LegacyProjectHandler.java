package de.catma.repository.git.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.Constants.GroupOrderBy;
import org.gitlab4j.api.Constants.ImpersonationState;
import org.gitlab4j.api.Constants.SortOrder;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupFilter;
import org.gitlab4j.api.models.Permissions;
import org.gitlab4j.api.models.PersonalAccessToken;
import org.gitlab4j.api.models.PersonalAccessToken.Scope;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectAccess;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.catma.document.annotation.TagReference;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GitTagsetHandler;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;

public class LegacyProjectHandler {

	private final GitLabApi privilegedGitLabApi;

	
	public LegacyProjectHandler(GitLabApi privilegedGitLabApi) {
		super();
		this.privilegedGitLabApi = privilegedGitLabApi;
	}

	public List<Group> getLegacyUserProjectReferences(GitLabApi restrictedGitLabApi) throws Exception {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();
		return groupApi.getGroups(new GroupFilter().withMinAccessLevel(AccessLevel.forValue(RBACRole.GUEST.getAccessLevel())));
	}
	
	public Pager<Group> getLegacyProjectReferences() throws Exception {
		GroupApi groupApi = privilegedGitLabApi.getGroupApi();
		GroupFilter groupFilter = 
				new GroupFilter()
					.withSortOder(SortOrder.ASC)
					.withOrderBy(GroupOrderBy.ID);
		return groupApi.getGroups(groupFilter, 10);
	}	
	
	public RBACRole getLegacyResourcePermissions(GitLabApi restrictedGitLabApi, String projectId, String resource) throws Exception {
		
		String resourceId = resource.substring(resource.lastIndexOf('/')+1);
		
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		Project project = projectApi.getProject(projectId, resourceId);
		Permissions permissions = project.getPermissions();
		
		int level = RBACRole.GUEST.getAccessLevel();
		
		ProjectAccess groupAccess = permissions.getGroupAccess();
		if (groupAccess != null) {
			AccessLevel groupAccessLevel = groupAccess.getAccessLevel();
			if (groupAccessLevel.value.intValue() > level) {
				level = groupAccessLevel.value.intValue();
			}
		}
		
		ProjectAccess projectAccess = permissions.getProjectAccess(); 
		if (projectAccess != null) {
			AccessLevel projectAccessLevel = projectAccess.getAccessLevel();
			if (projectAccessLevel.value.intValue() > level) {
				level = projectAccessLevel.value.intValue();
			}
		}
		return RBACRole.forValue(level);
	}
	
	public Set<Member> getLegacyProjectMembers(String projectId) throws IOException {
		try {
			Group group = privilegedGitLabApi.getGroupApi().getGroup(projectId);
			
			return privilegedGitLabApi.getGroupApi().getMembers(group.getId())
					.stream()
					.map(member -> new GitMember(member))
					.collect(Collectors.toSet());
		} catch (GitLabApiException e) {
			throw new IOException("group unknown",e);
		}
	}

	
	public Pair<User, String> aquireUser(String username) throws Exception {
		String tokenName = "migration_admin_token";
		UserApi userApi = this.privilegedGitLabApi.getUserApi();
		
		org.gitlab4j.api.models.User user = userApi.getUser(username);
		
		if (user == null) {
			return null;
		}
		else {
			List<PersonalAccessToken> impersonationTokens = userApi.getImpersonationTokens(
				user.getId(), ImpersonationState.ACTIVE
			);
	
			// revoke the default token, if it exists, actively
			for (PersonalAccessToken token : impersonationTokens) {
				if (token.getName().equals(tokenName)) {
					privilegedGitLabApi.getUserApi().revokeImpersonationToken(user.getId(), token.getId());
					break;
				}
			}
	
			PersonalAccessToken pat = userApi.createImpersonationToken(
					user.getId(), tokenName, null, new Scope[] {Scope.API}
				);
			
			String impersonationToken = pat.getToken();
			
			return new Pair<User, String>(new GitUser(user), impersonationToken);
		}
	}

	public void setUserWritablePermissions(Path dir) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
				filePath.toFile().setWritable(true, true);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	public void deleteUserTempPath(Path userTempPath) throws IOException {
		setUserWritablePermissions(userTempPath);
		FileUtils.deleteDirectory(userTempPath.toFile());
	}
	
	public String getProjectRootRepositoryUrl(
		GitLabApi restrictedGitLabApi, String projectId, String projectRootRepoName) throws Exception {
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		Project rootProject = projectApi.getProject(
			projectId, 
			projectRootRepoName);
		
		return GitlabUtils.rewriteGitLabServerUrl(rootProject.getHttpUrlToRepo());
	}
	
	public TagLibrary getTagLibrary(JGitRepoManager repoManager, File projectPath, User user) throws Exception {
		TagManager tagManager = new TagManager(new TagLibrary());
		
		ArrayList<TagsetDefinition> result = new ArrayList<>();
		File tagsetsDir = Paths.get(
				projectPath.getAbsolutePath(),
				GitProjectHandler.TAGSETS_DIRECTORY_NAME)
			.toFile();
		
		if (!tagsetsDir.exists()) {
			return tagManager.getTagLibrary();
		}
		
		File[] tagsetDirs = tagsetsDir.listFiles(file -> file.isDirectory());			
		
		GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					repoManager, 
					projectPath,
					user.getIdentifier(),
					user.getEmail());

		for (File tagsetDir : tagsetDirs) {
			String tagsetId = tagsetDir.getName();
			TagsetDefinition tagset = gitTagsetHandler.getTagset(tagsetId);						 
			result.add(tagset);
		}
			
		tagManager.load(result);
		
		return tagManager.getTagLibrary();
	}
	
	public List<Pair<JsonLdWebAnnotation, TagInstance>> loadLegacyTagInstances(
			String projectId, String markupCollectionId, File annotationDirectory, TagLibrary tagLibrary) throws Exception {
		
		ArrayList<TagReference> tagReferenceList = loadLegacyTagReferences(projectId, markupCollectionId, annotationDirectory);
		Multimap<TagInstance, TagReference> tagInstances = 
				Multimaps.index(tagReferenceList, TagReference::getTagInstance);
		
		List<Pair<JsonLdWebAnnotation, TagInstance>> annotationToTagInstanceMapping =
				Lists.newArrayList();
		
		for (TagInstance tagInstance : tagInstances.keySet()) {
			
			 Collection<TagReference> references = tagInstances.get(tagInstance);
			 
			JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
					references,
					tagLibrary,
					tagInstance.getPageFilename());
			annotationToTagInstanceMapping.add(new Pair<>(annotation, tagInstance));
		}
		
		return annotationToTagInstanceMapping;
	}
	
	private ArrayList<TagReference> loadLegacyTagReferences(
			String projectId, String markupCollectionId, File parentDirectory)
				throws Exception {

		ArrayList<TagReference> tagReferences = new ArrayList<>();

		List<String> contents = Arrays.asList(parentDirectory.list());
		
		for (String item : contents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(
					this.loadLegacyTagReferences(projectId, markupCollectionId,  target));
			}
			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			else if (target.isFile() && isTagInstanceFilename(target.getName())) {

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
	
	private boolean isTagInstanceFilename(String fileName){
		return !(
				fileName.equalsIgnoreCase("header.json") || fileName.equalsIgnoreCase(".git")
		);
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

	public void removeC6MigrationBranches(String projectId, String branchName) {
		try {
			List<Project> projects = 
					privilegedGitLabApi.getGroupApi().getProjects(projectId);
			for (Project project : projects) {
				List<Branch> branches = 
					privilegedGitLabApi.getRepositoryApi().getBranches(project.getId());
				Branch migrationBranch = 
						branches.stream()
							.filter(branch -> branch.getName().equals(branchName))
							.findFirst().orElse(null);
				if (migrationBranch != null) {
					privilegedGitLabApi.getRepositoryApi().deleteBranch(project.getId(), migrationBranch.getName());
				}
			}
		} catch (GitLabApiException e) {
			Logger.getLogger(getClass().getName()).log(
				Level.WARNING, String.format("Could delete %1$s branches!", branchName), e);
		}
	}
}
