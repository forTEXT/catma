package de.catma.repository.git.managers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MembershipSourceType;
import org.gitlab4j.api.models.Project;

import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;
import de.catma.user.Group;
import de.catma.user.SharedGroup;

public abstract class GitlabManagerCommon implements IRBACManager {
	protected abstract Logger getLogger();

	public abstract GitLabApi getGitLabApi();

	@Override
	public final boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, ProjectReference projectReference ) {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);

			if (project == null) {
				getLogger().warning(String.format("Unknown project \"%s\"", projectReference.getName()));
				return false;
			}

			Member projectMember = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId(), true);
			if (projectMember == null || permission == null) {
				return false;
			}

			return projectMember.getAccessLevel().value >= permission.getRoleRequired().getAccessLevel();
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

			java.util.Date expirationDate = expiresAt==null?null:
				java.util.Date.from(expiresAt.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

			// we use ASSISTANT/developer as the default role because as far as CATMA is concerned there is not much difference between
			// the developer and maintainer roles in groups
			Member member = getGitLabApi().getGroupApi().addMember(groupId, subject.getUserId(), RBACRole.ASSISTANT.getAccessLevel(), expirationDate);
			return new GitMember(member);
		} catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to add member %s to group with the ID %d with the ASSISTANT role", subject, groupId), e);
		}
	}

	@Override
	public final RBACSubject updateAssignmentOnGroup(Long userId, Long groupId, RBACRole role, LocalDate expiresAt) throws IOException {
		try {
			java.util.Date expirationDate = expiresAt==null?null:
				java.util.Date.from(expiresAt.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

			Member projectMember = getGitLabApi().getGroupApi().getMember(groupId, userId);

			Member updatedMember = getGitLabApi().getGroupApi().updateMember(groupId, userId, role.getAccessLevel(), expirationDate); 
			return new GitMember(updatedMember);
		} catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to update member with ID %d in group with the ID %d and the %s role", userId, groupId, role.toString()), e);
		}
	}
	
	
	@Override
	public final RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference, LocalDate expiresAt) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);

			if (project == null) {
				throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
			}
			java.util.Date expirationDate = expiresAt==null?null:
				java.util.Date.from(expiresAt.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

			try {
				Member projectMember = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
				AccessLevel projectMemberAccessLevel = projectMember.getAccessLevel();

				if (projectMemberAccessLevel == AccessLevel.OWNER || projectMemberAccessLevel.value == role.getAccessLevel()) {
					// AccessLevel is either OWNER which means we would change/lose the owner,
					// or it is already the target AccessLevel.
					// In both cases we refuse to update the role and simply do nothing.
					return subject;
				}

				return updateProjectMember(subject, role, project.getId(), expirationDate);
			}
			catch (GitLabApiException e) {
				// TODO: refactor this, don't just assume the error means we need to create a new project member
				//       if getMember above does not find the member in the project it throws GitLabApiException: 404 Not found
				getLogger().log(
						Level.WARNING,
						String.format(
								"Couldn't update project member \"%s\" for project \"%s\". "
								+ "Will try to create a new project member.",
								// this ternary handles the special case where subject is actually a lambda, so that we still know which user it is
								// (see ProjectInvitationHandler)
								subject.getClass().getName().contains("$Lambda$") ? String.format("<User ID: %s>", subject.getUserId()) : subject,
								projectReference.getName()
						),
						e
				);
				return createProjectMember(subject, role, project.getId(), expirationDate);
			}
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to fetch project \"%s\"", projectReference.getName())
			);
		}
	}
	
	@Override
	public SharedGroup assignOnProject(SharedGroup sharedGroup, RBACRole role, ProjectReference projectReference, LocalDate expiresAt, boolean reassign)
			throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);

			if (project == null) {
				throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
			}
			
			if (reassign) {
				getGitLabApi().getProjectApi().unshareProject(project.getId(), sharedGroup.groupId());
			}
			else {
				// check that the project is not already shared with the group (GitLab returns an error if we try to share the same project again)
				if (project.getSharedWithGroups().stream().anyMatch(group -> group.getGroupId() == sharedGroup.groupId())) {
					return sharedGroup;
				}
			}

			java.util.Date expirationDate = expiresAt==null?null:
				java.util.Date.from(expiresAt.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
			
			getGitLabApi().getProjectApi().shareProject(
					project.getId(), 
					sharedGroup.groupId(), 
					AccessLevel.forValue(role.getAccessLevel()), 
					expirationDate);
			
		
			return sharedGroup;
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to assign group '%s' on project '%s'", sharedGroup.name(), projectReference.getName()), e
			);
		}
	}

	@Override
	public final void unassignFromProject(RBACSubject subject, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);

			if (project == null) {
				throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
			}

			getGitLabApi().getProjectApi().removeMember(project.getId(), subject.getUserId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to remove member \"%s\" from project \"%s\"",
							subject,
							projectReference.getName()
					),
					e
			);
		}
	}
	
	@Override
	public void unassignFromProject(SharedGroup sharedGroup, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);
	
			if (project == null) {
				throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
			}
			
			getGitLabApi().getProjectApi().unshareProject(project.getId(), sharedGroup.groupId());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to unshare project '%s' for group '%s'",
							projectReference.getName(),
							sharedGroup.name()
					),
					e
			);			
		}
	}

	private RBACSubject createProjectMember(RBACSubject subject, RBACRole role, Long projectId, Date expiresAt) throws IOException {
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
					String.format("Failed to create project member \"%s\" in project with ID %s", subject, projectId),
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
							expiresAt)
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to update project member \"%s\" in project with ID %s", subject, projectId),
					e
			);
		}
	}

	@Override
	public final RBACRole getRoleOnProject(RBACSubject subject, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);
			if (project == null) {
				throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
			}

			Member member = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId(), true);
			if (member == null ) {
				throw new IOException(String.format("Member \"%s\" not found in project \"%s\"", subject, projectReference.getName()));
			}

			return RBACRole.forValue(member.getAccessLevel().value);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to get role on project \"%s\" for member \"%s\"",
							projectReference.getName(),
							subject
					),
					e
			);
		}
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
					String.format(
							"Failed to get role on group \"%s\" for member \"%s\"",
							group.getName(),
							subject
					),
					e
			);
		}
	}
}
