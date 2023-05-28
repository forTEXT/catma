package de.catma.repository.git.managers;

import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

			Member projectMember = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
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
	public final RBACSubject assignOnProject(RBACSubject subject, RBACRole role, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId()
			);

			if (project == null) {
				throw new IOException(String.format("Unknown project \"%s\"", projectReference.getName()));
			}

			try {
				Member projectMember = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
				AccessLevel projectMemberAccessLevel = projectMember.getAccessLevel();

				if (projectMemberAccessLevel == AccessLevel.OWNER || projectMemberAccessLevel.value == role.getAccessLevel()) {
					// AccessLevel is either OWNER which means we would change/lose the owner,
					// or it is already the target AccessLevel.
					// In both cases we refuse to update the role and simply do nothing.
					return subject;
				}

				return updateProjectMember(subject, role, project.getId());
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
				return createProjectMember(subject, role, project.getId());
			}
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to fetch project \"%s\"", projectReference.getName())
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

	private de.catma.user.Member createProjectMember(RBACSubject subject, RBACRole role, Long projectId) throws IOException {
		try {
			return new GitMember(
					getGitLabApi().getProjectApi().addMember(
							projectId,
							subject.getUserId(),
							AccessLevel.forValue(role.getAccessLevel())
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

	private de.catma.user.Member updateProjectMember(RBACSubject subject, RBACRole role, long projectId) throws IOException {
		try {
			return new GitMember(
					getGitLabApi().getProjectApi().updateMember(
							projectId,
							subject.getUserId(),
							AccessLevel.forValue(role.getAccessLevel())
					)
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

			Member member = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
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
}
