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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.*;
import org.gitlab4j.api.models.PersonalAccessToken.Scope;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.catma.document.annotation.TagReference;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GitTagsetHandler;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitLabUtils;
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
					.withSortOder(Constants.SortOrder.ASC)
					.withOrderBy(Constants.GroupOrderBy.ID);
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
			throw new IOException("Unknown group",e);
		}
	}

	public int getLegacyProjectIssuesCount(String projectId) throws IOException {
		try {
			IssuesStatistics issuesStats = privilegedGitLabApi.getIssuesApi().getGroupIssuesStatistics(
					projectId, new IssuesStatisticsFilter().withScope(Constants.IssueScope.ALL)
			);
			return issuesStats.getCounts().getAll();
		}
		catch (GitLabApiException e) {
			throw new IOException("Unknown group", e);
		}
	}

	public Pair<User, String> acquireUser(String username) throws Exception {
		String tokenName = "migration_admin_token";
		UserApi userApi = this.privilegedGitLabApi.getUserApi();
		
		org.gitlab4j.api.models.User user = userApi.getUser(username);
		
		if (user == null) {
			return null;
		}
		else {
			List<PersonalAccessToken> impersonationTokens = userApi.getImpersonationTokens(
				user.getId(), Constants.ImpersonationState.ACTIVE
			);
	
			// revoke the default token, if it exists, actively
			for (PersonalAccessToken token : impersonationTokens) {
				if (token.getName().equals(tokenName)) {
					privilegedGitLabApi.getUserApi().revokeImpersonationToken(user.getId(), token.getId());
					break;
				}
			}
	
			PersonalAccessToken pat = userApi.createImpersonationToken(
					user.getId(), tokenName, Date.from(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2).toInstant()), new Scope[] {Scope.API}
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
		
		return GitLabUtils.rewriteGitLabServerUrl(rootProject.getHttpUrlToRepo());
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
			String projectId,
			String markupCollectionId,
			File annotationDirectory,
			TagLibrary tagLibrary
	) throws Exception {
		ArrayList<TagReference> legacyTagReferences = loadLegacyTagReferences(markupCollectionId, annotationDirectory);
		Multimap<TagInstance, TagReference> legacyTagReferencesByTagInstance = Multimaps.index(legacyTagReferences, TagReference::getTagInstance);

		List<Pair<JsonLdWebAnnotation, TagInstance>> annotationTagInstanceMap =	Lists.newArrayList();

		for (TagInstance tagInstance : legacyTagReferencesByTagInstance.keySet()) {
			Collection<TagReference> tagReferences = legacyTagReferencesByTagInstance.get(tagInstance);
			JsonLdWebAnnotation annotation = new JsonLdWebAnnotation(
					tagReferences,
					tagLibrary,
					tagInstance.getPageFilename()
			);
			annotationTagInstanceMap.add(new Pair<>(annotation, tagInstance));
		}

		return annotationTagInstanceMap;
	}

	private ArrayList<TagReference> loadLegacyTagReferences(String markupCollectionId, File parentDirectory) throws Exception {
		ArrayList<TagReference> tagReferences = new ArrayList<>();

		String[] directoryContents = parentDirectory.list();

		for (String item : directoryContents) {
			File target = new File(parentDirectory, item);

			// if it is a directory, recurse into it adding results to the current tagReferences list
			if (target.isDirectory() && !target.getName().equalsIgnoreCase(".git")) {
				tagReferences.addAll(
						loadLegacyTagReferences(markupCollectionId, target)
				);
			}
			// if item is <CATMA_UUID>.json, read it into a list of TagReference objects
			else if (target.isFile() && isTagInstanceFilename(target.getName())) {
				String serialized = readFileToString(target, StandardCharsets.UTF_8);
				JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>()
						.deserialize(serialized, JsonLdWebAnnotation.class);

				tagReferences.addAll(
						jsonLdWebAnnotation.toTagReferences(markupCollectionId)
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
				Level.WARNING, String.format("Couldn't delete branch \"%s\"", branchName), e);
		}
	}
}
