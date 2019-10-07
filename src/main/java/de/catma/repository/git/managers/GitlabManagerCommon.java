package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;

import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectManager;

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
	public final boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, String projectId) {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if(group == null) {
				getLogger().log(Level.WARNING, 
						String.format("CATMA-Project/git-Group unknown %1$s", projectId));
				return false;
			}
			return isMemberAuthorized(
					permission, 
					getGitLabApi().getGroupApi().getMember(group.getId(), subject.getUserId()));
		} catch (GitLabApiException e) {
			getLogger().log(
				Level.SEVERE, 
				String.format("Error retrieving permissions for CATMA-Project/git-Group unknown %1$s", projectId),
				e);
			return false;
		}
	}
	
	@Override
	public final boolean isAuthorizedOnResource(RBACSubject subject, RBACPermission permission, String projectId, String resourceId) {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(projectId, resourceId);
			if(project != null){
				return isMemberAuthorized(permission, getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId()));
			}
			return false;
		} catch (GitLabApiException e) {
			getLogger().log(Level.WARNING, "Can't retrieve permissions from resource: "+ resourceId,e);
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
	public final RBACSubject assignOnProject(RBACSubject subject, RBACRole role, String projectId) throws IOException {
		try {
			
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if (group == null) {
				throw new IOException(
					String.format("CATMA-Project/git-Group unknown %1$s", projectId));
			}
			
			try {
				Member groupMember = 
						getGitLabApi().getGroupApi().getMember(
								group.getId(), subject.getUserId());
				
				if(groupMember.getAccessLevel() != AccessLevel.OWNER && 
						groupMember.getAccessLevel().value.intValue() != role.getAccessLevel()) {
					
					if(role.getAccessLevel() < RBACRole.ASSISTANT.getAccessLevel()){
						assignDefaultAccessToRootProject(subject, group.getId());
					}
					
					return updateGroupMember(subject, role, group.getId());
					
				} else {
					// AccessLevel is either OWNER which means we would change/loose the owner, 
					// or it is already the target AccessLevel.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
			} catch (GitLabApiException e) {
				
				// member does not exist yet, so we create a new group member

				if(role.getAccessLevel() < RBACRole.ASSISTANT.getAccessLevel()){
					assignDefaultAccessToRootProject(subject, group.getId());
				}
				
				return createGroupMember(subject, role, group.getId());
			}
		} catch (GitLabApiException e) {
			throw new IOException(
				String.format("Error accessing CATMA-Project/git-Group %1$s", projectId));
		}	
	};
	
	@Override
	public final void unassignFromProject(RBACSubject subject, String projectId) throws IOException {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if(group == null) {
				throw new IOException(
						String.format("CATMA-Project/git-Group unknown %1$s", projectId));
			}
			getGitLabApi().getGroupApi().removeMember(group.getId(), subject.getUserId());
		} catch (GitLabApiException e) {
			throw new IOException(
					String.format(
						"Error accessing CATMA-Project/git-Group %1$s or user %2$s", 
						projectId, 
						subject==null?"null":subject.toString()), e);
		}	
	}
	
	@Override
	public final void unassignFromResource(RBACSubject subject, String projectId, String resourceId) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(projectId, resourceId);
			if(project == null) {
				throw new IOException("Resource unkown "+ resourceId);
			}
			getGitLabApi().getProjectApi().removeMember(project.getId(), subject.getUserId());
		} catch (GitLabApiException e) {
			throw new IOException("Project or user unkown: "+ resourceId + "subject:" + subject, e);
		}	
	}
	
	@Override
	public final RBACSubject assignOnResource(
			RBACSubject subject, 
			RBACRole role, 
			String catmaProjectGroupId, 
			String gitResourceProjectId) throws IOException {
		
		try {
			Project gitResourceProject = 
					getGitLabApi().getProjectApi().getProject(
							catmaProjectGroupId, gitResourceProjectId);
			
			if(gitResourceProject == null) {
				throw new IOException(
					String.format(
						"git-Resource-Project or git-Root-Project unknown! CATMA Project %1$s git-Resource-Project %2$s", 
						catmaProjectGroupId, gitResourceProjectId));
			}

			try {
				Member gitResourceProjectMember = 
						getGitLabApi().getProjectApi().getMember(
								gitResourceProject.getId(), subject.getUserId());
				
				if(gitResourceProjectMember.getAccessLevel() != AccessLevel.OWNER &&
						gitResourceProjectMember.getAccessLevel().value.intValue() != role.getAccessLevel()) {
					
					return updateProjectMember(subject, role, gitResourceProject.getId());
					
				} else {
					// AccessLevel is either OWNER which means we would change/loose the owner, 
					// or it is already the target AccessLevel.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
				
			} catch (GitLabApiException e) {
				return createProjectMember(subject, role, gitResourceProject.getId());
			}
		} catch (GitLabApiException e) {
			throw new IOException(
				String.format(
					"Error accessing git-Resource-Project or git-Root-Project! CATMA Project %1$s git-Resource-Project %2$s", 
					catmaProjectGroupId, gitResourceProjectId),
				e);			
		}	
		
	};
	
	private de.catma.user.Member createGroupMember(RBACSubject subject, RBACRole role, Integer groupId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getGroupApi().addMember(groupId, subject.getUserId(), AccessLevel.forValue(role.getAccessLevel())));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ groupId,e);
		}		
	}
	
	private RBACSubject assignDefaultAccessToRootProject(RBACSubject subject, Integer groupId) throws IOException {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(groupId);
			Project rootProject = getGitLabApi().getProjectApi().getProject(group.getName(), GitProjectManager.getProjectRootRepositoryName(group.getName()));
			
			try {
				Member member = getGitLabApi().getProjectApi().getMember(rootProject.getId(), subject.getUserId());
				if(member.getAccessLevel().value < RBACRole.ASSISTANT.getAccessLevel() ){
					return new GitMember(getGitLabApi().getProjectApi().updateMember(rootProject.getId(), subject.getUserId(), AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel())));
				} else {
					// Role is either OWNER which means we would oust the owner, or it is already the same.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
			} catch (GitLabApiException e) {
				return new GitMember(getGitLabApi().getProjectApi().addMember(rootProject.getId(), subject.getUserId(), AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel())));
			}
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ groupId,e);
		}	
	}
	
	private de.catma.user.Member updateGroupMember(RBACSubject subject, RBACRole role, Integer groupId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getGroupApi().updateMember(groupId, subject.getUserId(), AccessLevel.forValue(role.getAccessLevel())));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ groupId, e);
		}		
	}
	
	private de.catma.user.Member createProjectMember(RBACSubject subject, RBACRole role, Integer projectId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getProjectApi().addMember(projectId, subject.getUserId(), AccessLevel.forValue(role.getAccessLevel())));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ projectId,e);
		}		
	}
	
	
	private de.catma.user.Member updateProjectMember(RBACSubject subject, RBACRole role, Integer projectId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getProjectApi().updateMember(projectId, subject.getUserId(), AccessLevel.forValue(role.getAccessLevel())));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ projectId, e);
		}		
	}
	
	@Override
	public final RBACRole getRoleOnResource(RBACSubject subject, String projectId, String resourceId) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(projectId, resourceId);
			if(project == null) {
				throw new IOException("resource or rootproject unkown "+ resourceId);
			}
				Member member = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
			
			if(member == null ){
				throw new IOException("member not found " + subject);
			}
			return RBACRole.forValue(member.getAccessLevel().value);
			
		} catch (GitLabApiException e) {
			throw new IOException("resource or rootproject unkown "+ resourceId, e);
		}		
	}
	
	@Override
	public final RBACRole getRoleOnProject(RBACSubject subject, String projectId) throws IOException {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if(group == null) {
				throw new IOException("Project unkown "+ projectId);
			}
			Member member = getGitLabApi().getGroupApi().getMember(group.getId(), subject.getUserId());
			
			if(member == null ){
				throw new IOException("member not found " + subject);
			}
			return RBACRole.forValue(member.getAccessLevel().value);
			
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ projectId, e);
		}	
	}
	
}
