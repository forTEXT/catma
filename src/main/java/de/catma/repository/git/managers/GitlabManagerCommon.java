package de.catma.repository.git.managers;

import de.catma.project.ProjectReference;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerCommon;
import de.catma.user.Group;
import de.catma.user.SharedGroup;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MembershipSourceType;
import org.gitlab4j.api.models.Project;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class GitlabManagerCommon implements RemoteGitManagerCommon {
	protected abstract Logger getLogger();

	@Override
	public abstract GitLabApi getGitLabApi();

	private Project fetchProject(ProjectReference projectReference) throws GitLabApiException, IOException {
		Project project = getGitLabApi().getProjectApi().getProject(
				projectReference.getNamespace(), projectReference.getProjectId()
		);

		if (project == null) {
			throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
		}

		return project;
	}

	private Date convertExpirationLocalDateToDate(LocalDate expiresAt) {
		return expiresAt == null ? null : Date.from(expiresAt.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public final RBACRole getRoleOnGroup(RBACSubject subject, Group group) throws IOException {
		try {
			org.gitlab4j.api.models.Group gitlabGroup = getGitLabApi().getGroupApi().getGroup(group.getId());

			if (gitlabGroup == null) {
				throw new IOException(String.format("Unknown group \"%s\"", group.getName()));
			}

			Member member = getGitLabApi().getGroupApi().getMember(group.getId(), subject.getUserId(), true);
			if (member == null ) {
				throw new IOException(String.format("Member \"%s\" not found in group \"%s\"", subject, group.getName()));
			}

			return RBACRole.forValue(member.getAccessLevel().value);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to get role on group \"%s\" for member \"%s\"", group.getName(), subject),
					e
			);
		}
	}

	@Override
	public final RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException {
		try {
			Project project = fetchProject(projectReference);

			Member member = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId(), true);
			if (member == null ) {
				throw new IOException(String.format("Member \"%s\" not found in project \"%s\"", subject, projectReference.getName()));
			}

			return RBACRole.forValue(member.getAccessLevel().value);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to get role on project \"%s\" for member \"%s\"", projectReference.getName(), subject),
					e
			);
		}
	}

	@Override
	public final boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, ProjectReference projectReference) {
		try {
			Project project = fetchProject(projectReference);

			Member projectMember = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId(), true);
			if (projectMember == null || permission == null) {
				return false;
			}

			return projectMember.getAccessLevel().value >= permission.getRoleRequired().getAccessLevel();
		}
		catch (IOException e) {
			// unknown project
			getLogger().warning(e.getMessage());
			return false;
		}
		catch (GitLabApiException e) {
			getLogger().log(
					Level.SEVERE,
					String.format("Failed to retrieve permissions for project \"%s\"", projectReference.getName()),
					e
			);
			return false;
		}
	}

	@Override
	public final RBACSubject assignOnGroup(RBACSubject subject, Long groupId, LocalDate expiresAt) throws IOException {
		try {
			// check that the user is not already a member of the group (GitLab returns an error if we try to add the same user again)
			if (getGitLabApi().getUserApi().getMemberships(subject.getUserId()).stream().anyMatch(
					membership -> membership.getSourceType() == MembershipSourceType.NAMESPACE && membership.getSourceId().equals(groupId)
			)) {
				return subject;
			}

			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			// we use ASSISTANT/developer as the default role because as far as CATMA is concerned there is not much difference between
			// the developer and maintainer roles in groups
			Member member = getGitLabApi().getGroupApi().addMember(groupId, subject.getUserId(), RBACRole.ASSISTANT.getAccessLevel(), expirationDate);
			return new GitMember(member);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to add member \"%s\" to group with ID %d", subject, groupId),
					e
			);
		}
	}

	@Override
	public final RBACSubject updateAssignmentOnGroup(Long userId, Long groupId, RBACRole role, LocalDate expiresAt) throws IOException {
		try {
			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			Member updatedMember = getGitLabApi().getGroupApi().updateMember(groupId, userId, role.getAccessLevel(), expirationDate);
			return new GitMember(updatedMember);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to update member with ID %d in group with ID %d", userId, groupId),
					e
			);
		}
	}

	@Override
	public final void unassignFromGroup(RBACSubject subject, Long groupId) throws IOException {
		try {
			getGitLabApi().getGroupApi().removeMember(groupId, subject.getUserId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to remove member \"%s\" from group with ID %d", subject, groupId),
					e
			);
		}
	}

	private RBACSubject addProjectMember(RBACSubject subject, RBACRole role, Long projectId, Date expiresAt) throws IOException {
		try {
			return new GitMember(
					getGitLabApi().getProjectApi().addMember(
							projectId,
							subject.getUserId(),
							AccessLevel.forValue(role.getAccessLevel()),
							expiresAt
					)
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to add member \"%s\" to project with ID %s", subject, projectId),
					e
			);
		}
	}

	private RBACSubject updateProjectMember(RBACSubject subject, RBACRole role, long projectId, Date expiresAt) throws IOException {
		try {
			return new GitMember(
					getGitLabApi().getProjectApi().updateMember(
							projectId,
							subject.getUserId(),
							AccessLevel.forValue(role.getAccessLevel()),
							expiresAt
					)
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to update member \"%s\" in project with ID %s", subject, projectId),
					e
			);
		}
	}

	@Override
	public final RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference, LocalDate expiresAt) throws IOException {
		try {
			Project project = fetchProject(projectReference);

			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			try {
				Member projectMember = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
				AccessLevel projectMemberAccessLevel = projectMember.getAccessLevel();

				if (projectMemberAccessLevel == AccessLevel.OWNER || projectMemberAccessLevel.value == role.getAccessLevel()) {
					// if the project member's current access level is OWNER or already the target access level, do nothing
					// we don't touch owners because there is usually only one, and trying to change their role would cause an error
					// TODO: 1. this prevents setting/changing the expiration date unless there is a role change
					//       2. GitLab allows for multiple owners (although we don't support this in the UI yet), so we could check if there is at least one
					//          other owner and allow the change, as long as the member being modified here is not the project creator (because then we would
					//          be talking about transferring the project into another namespace)
					return subject;
				}

				return updateProjectMember(subject, role, project.getId(), expirationDate);
			}
			catch (GitLabApiException e) {
				// if getMember above does not find the member in the project it throws GitLabApiException: 404 Not found
				if (e.getMessage().contains("404")) {
					// we need to add the member to the project
					return addProjectMember(subject, role, project.getId(), expirationDate);
				}
				throw e;
			}
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format(
						"Failed to add or update member \"%s\" to/in project \"%s\"",
						// this ternary handles the special case where subject is actually a lambda, so that we still know which user it is
						// (see ProjectInvitationHandler)
						subject.getClass().getName().contains("$Lambda$") ? String.format("<User ID: %s>", subject.getUserId()) : subject,
						projectReference.getName()
				),
				e
			);
		}
	}

	@Override
	public final void unassignFromProject(RBACSubject subject, ProjectReference projectReference) throws IOException {
		try {
			Project project = fetchProject(projectReference);

			getGitLabApi().getProjectApi().removeMember(project.getId(), subject.getUserId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to remove member \"%s\" from project \"%s\"", subject, projectReference.getName()),
					e
			);
		}
	}

	@Override
	public SharedGroup assignOnProject(SharedGroup sharedGroup, RBACRole role, ProjectReference projectReference, LocalDate expiresAt, boolean reassign)
			throws IOException {
		try {
			Project project = fetchProject(projectReference);

			if (reassign) {
				getGitLabApi().getProjectApi().unshareProject(project.getId(), sharedGroup.groupId());
			}
			else {
				// check that the project is not already shared with the group (GitLab returns an error if we try to share the same project again)
				if (project.getSharedWithGroups().stream().anyMatch(group -> group.getGroupId() == sharedGroup.groupId())) {
					return sharedGroup;
				}
			}

			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			getGitLabApi().getProjectApi().shareProject(
					project.getId(),
					sharedGroup.groupId(), 
					AccessLevel.forValue(role.getAccessLevel()), 
					expirationDate
			);

			return sharedGroup;
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to add or update group \"%s\" to/in project \"%s\"", sharedGroup.name(), projectReference.getName()),
				e
			);
		}
	}

	@Override
	public void unassignFromProject(SharedGroup sharedGroup, ProjectReference projectReference) throws IOException {
		try {
			Project project = fetchProject(projectReference);

			getGitLabApi().getProjectApi().unshareProject(project.getId(), sharedGroup.groupId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to remove group \"%s\" from project \"%s\"", sharedGroup.name(), projectReference.getName()),
					e
			);
		}
	}
}
