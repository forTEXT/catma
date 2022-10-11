package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;

import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;

public abstract class GitlabManagerCommon implements IRBACManager {
	
	/**
	 * Gets a logger 
	 * @return
	 */
	protected abstract Logger getLogger();
	
	/**
	 * The connected API 
	 * @return
	 */
	public abstract GitLabApi getGitLabApi();
	
	@Override
	public final boolean isAuthorizedOnProject(
			RBACSubject subject, RBACPermission permission, ProjectReference projectReference ) {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId());
			if(project == null) {
				getLogger().log(Level.WARNING, 
						String.format("Unknown project \"%s\"", projectReference));
				return false;
			}
			return isMemberAuthorized(
					permission, 
					getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId()));
		} catch (GitLabApiException e) {
			getLogger().log(
				Level.SEVERE, 
				String.format("Failed to retrieve permissions for project \"%s\"", projectReference),
				e);
			return false;
		}
	}

	
	private boolean isMemberAuthorized(RBACPermission permission, Member member){
		if(member == null)
			return false;
		if(permission == null)
			return false;
		
		return member.getAccessLevel().value >= permission.getRoleRequired().getAccessLevel();
	}
	
	@Override
	public final RBACSubject assignOnProject(
			RBACSubject subject, RBACRole role, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId());
			if(project == null) {
				throw new IOException(
						String.format("Unknown project \"%s\"", projectReference));
			}
			try {
				Member projectMember = 
						getGitLabApi().getProjectApi().getMember(
								project.getId(), subject.getUserId());
				
				if(projectMember.getAccessLevel() != AccessLevel.OWNER &&
						projectMember.getAccessLevel().value.intValue() != role.getAccessLevel()) {
					
					return updateProjectMember(subject, role, project.getId());
					
				} else {
					// AccessLevel is either OWNER which means we would change/loose the owner, 
					// or it is already the target AccessLevel.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
				
			} catch (GitLabApiException e) {
				// TODO: refactor this, don't just assume the error means we need to create a new project member
				//       if getMember above does not find the member in the project it throws GitLabApiException: 404 Not found
				Logger.getLogger(GitlabManagerCommon.class.getName()).log(
						Level.WARNING, String.format(
							"Could not update project member \"%s\" for project \"%s\". "
							+ "Will try to create a new project member.",
							subject, projectReference), e);
				return createProjectMember(subject, role, project.getId());
			}
		} catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to access project \"%s\"", projectReference));
		}	
	};
	
	@Override
	public final void unassignFromProject(
			RBACSubject subject, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId());
			if(project == null) {
				throw new IOException(
						String.format("Unknown project \"%s\"", projectReference));
			}
			getGitLabApi().getProjectApi().removeMember(project.getId(), subject.getUserId());
		} catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to remove member \"%s\" from project \"%s\"",
							subject,
							projectReference), e);
		}	
	}
	
	private de.catma.user.Member createProjectMember(
			RBACSubject subject, RBACRole role, Long projectId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getProjectApi().addMember(
					projectId, subject.getUserId(), AccessLevel.forValue(role.getAccessLevel())));
		} catch (GitLabApiException e) {
			throw new IOException(
					String.format("Error creating project member \"%s\" in project #%s", subject, projectId),
					e
			);
		}		
	}

	private de.catma.user.Member updateProjectMember(
			RBACSubject subject, RBACRole role, long projectId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getProjectApi().updateMember(
					projectId, subject.getUserId(), AccessLevel.forValue(role.getAccessLevel())));
		} catch (GitLabApiException e) {
			throw new IOException(
					String.format("Error updating project member \"%s\" in project #%s", subject, projectId),
					e
			);
		}		
	}

	@Override
	public final RBACRole getRoleOnProject(
			RBACSubject subject, ProjectReference projectReference) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(
					projectReference.getNamespace(), projectReference.getProjectId());
			if(project == null) {
				throw new IOException(
						String.format("Unknown project \"%s\"", projectReference));
			}
			Member member = getGitLabApi().getProjectApi().getMember(
					project.getId(), subject.getUserId());
			
			if(member == null ){
				throw new IOException(
						String.format("Member \"%s\" not found in project \"%s\"", subject, projectReference)
				);
			}
			if (project.getOwner().getId().equals(subject.getUserId())) {
				return RBACRole.OWNER;
			}
			return RBACRole.forValue(member.getAccessLevel().value);
			
		} catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to get role on project \"%s\" for member \"%s\"",
						projectReference,
						subject),
				e);
		}	
	}
}
