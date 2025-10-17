package de.catma.repository.git.managers;

import de.catma.project.ProjectReference;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerCommon;
import de.catma.user.Group;
import de.catma.user.SharedGroup;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class GitlabManagerCommon implements RemoteGitManagerCommon {
	protected abstract Logger getLogger();

	public abstract GitLabApi getGitLabApi();

	private Date convertExpirationLocalDateToDate(LocalDate expiresAt) {
		return expiresAt == null ? null : Date.from(expiresAt.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public final RBACRole getRoleOnGroup(RBACSubject subject, Group group) throws IOException {
		try {
			// includeInherited=false because we don't allow for group hierarchies
			Member member = getGitLabApi().getGroupApi().getMember(group.getId(), subject.getUserId(), false);
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
			// includeInherited=true because membership might be via a group
			Member member = getGitLabApi().getProjectApi().getMember(projectReference.getFullPath(), subject.getUserId(), true);
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
			// includeInherited=true because membership might be via a group
			Member projectMember = getGitLabApi().getProjectApi().getMember(projectReference.getFullPath(), subject.getUserId(), true);
			if (projectMember == null || permission == null) {
				return false;
			}

			return projectMember.getAccessLevel().value >= permission.getRoleRequired().getAccessLevel();
		}
		catch (GitLabApiException e) {
			// TODO: throw and handle in caller
			getLogger().log(
					Level.SEVERE,
					String.format("Failed to retrieve permissions for project \"%s\"", projectReference.getName()),
					e
			);
			return false;
		}
	}

	@Override
	public final void assignOnGroup(RBACSubject subject, Long groupId, LocalDate expiresAt) throws IOException {
		try {
			// check that the user is not already a member of the group (GitLab returns an error if we try to add the same user again)
			if (getGitLabApi().getGroupApi().getMembers(groupId).stream().anyMatch(
					member -> member.getId().equals(subject.getUserId())
			)) {
				return;
			}

			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			// we use ASSISTANT/developer as the default role because as far as CATMA is concerned there is not much difference between
			// the developer and maintainer roles in groups
			getGitLabApi().getGroupApi().addMember(groupId, subject.getUserId(), RBACRole.ASSISTANT.getAccessLevel(), expirationDate);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to add member \"%s\" to group with ID %d", subject, groupId),
					e
			);
		}
	}

	@Override
	public final void updateAssignmentOnGroup(Long userId, Long groupId, RBACRole role, LocalDate expiresAt) throws IOException {
		try {
			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			getGitLabApi().getGroupApi().updateMember(groupId, userId, role.getAccessLevel(), expirationDate);
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

	private void addProjectMember(RBACSubject subject, RBACRole role, ProjectReference projectReference, Date expiresAt) throws IOException {
		try {
			getGitLabApi().getProjectApi().addMember(
					projectReference.getFullPath(), subject.getUserId(), AccessLevel.forValue(role.getAccessLevel()), expiresAt
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to add member \"%s\" to project \"%s\"", subject, projectReference.getName()),
					e
			);
		}
	}

	private void updateProjectMember(RBACSubject subject, RBACRole role, ProjectReference projectReference, Date expiresAt) throws IOException {
		try {
			getGitLabApi().getProjectApi().updateMember(
					projectReference.getFullPath(), subject.getUserId(), AccessLevel.forValue(role.getAccessLevel()), expiresAt
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to update member \"%s\" in project \"%s\"", subject, projectReference.getName()),
					e
			);
		}
	}

	@Override
	public final void assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference, LocalDate expiresAt) throws IOException {
		try {
			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			if (getGitLabApi().getProjectApi().getMembers(projectReference.getFullPath()).stream().anyMatch(
					member -> member.getId().equals(subject.getUserId())
			)) {
				// the user is already a member of the project
				updateProjectMember(subject, role, projectReference, expirationDate);
				return;
			}

			// we need to add the member to the project
			addProjectMember(subject, role, projectReference, expirationDate);
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
			getGitLabApi().getProjectApi().removeMember(projectReference.getFullPath(), subject.getUserId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to remove member \"%s\" from project \"%s\"", subject, projectReference.getName()),
					e
			);
		}
	}

	@Override
	public final void assignOnProject(SharedGroup sharedGroup, RBACRole role, ProjectReference projectReference, LocalDate expiresAt, boolean reassign)
			throws IOException {
		try {
			if (reassign) {
				getGitLabApi().getProjectApi().unshareProject(projectReference.getFullPath(), sharedGroup.groupId());
			}
			else {
				// check that the project is not already shared with the group (GitLab returns an error if we try to share the same project again)
				Project project = getGitLabApi().getProjectApi().getProject(projectReference.getFullPath());
				if (project.getSharedWithGroups().stream().anyMatch(group -> group.getGroupId() == sharedGroup.groupId())) {
					return;
				}
			}

			Date expirationDate = convertExpirationLocalDateToDate(expiresAt);

			getGitLabApi().getProjectApi().shareProject(
					projectReference.getFullPath(),
					sharedGroup.groupId(), 
					AccessLevel.forValue(role.getAccessLevel()), 
					expirationDate
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to add or update group \"%s\" to/in project \"%s\"", sharedGroup.name(), projectReference.getName()),
				e
			);
		}
	}

	@Override
	public final void unassignFromProject(SharedGroup sharedGroup, ProjectReference projectReference) throws IOException {
		try {
			getGitLabApi().getProjectApi().unshareProject(projectReference.getFullPath(), sharedGroup.groupId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to remove group \"%s\" from project \"%s\"", sharedGroup.name(), projectReference.getName()),
					e
			);
		}
	}
}
