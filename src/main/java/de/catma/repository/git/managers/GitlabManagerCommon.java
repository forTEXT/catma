package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;

import de.catma.interfaces.IdentifiableResource;
import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectManager;

public interface GitlabManagerCommon extends IRBACManager {

	/**
	 * Gets a logger 
	 * @return
	 */
	Logger getLogger();
	
	/**
	 * The connected API 
	 * @return
	 */
	GitLabApi getGitLabApi();
	
	@Override
	public default boolean isAuthorizedOnProject(RBACSubject subject, RBACPermission permission, String projectId) {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if(group == null) {
				getLogger().log(Level.WARNING, "Project unkown "+ projectId);
				return false;
			}
			return isMemberAuthorized(permission, getGitLabApi().getGroupApi().getMember(group.getId(), subject.getUserId()));
		} catch (GitLabApiException e) {
			getLogger().log(Level.WARNING, "Can't retrieve permissions from project: "+ projectId,e);
			return false;
		}
	}
	
	@Override
	public default boolean isAuthorizedOnResource(RBACSubject subject, RBACPermission permission, IdentifiableResource resource) {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(resource.getProjectId(), resource.getResourceId());
			if(project != null){
				return isMemberAuthorized(permission, getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId()));
			}
			return false;
		} catch (GitLabApiException e) {
			getLogger().log(Level.WARNING, "Can't retrieve permissions from resource: "+ resource.getResourceId(),e);
			return false;
		}
	}
	
	public default boolean isMemberAuthorized(RBACPermission permission, Member member){
		if(member == null)
			return false;
		if(permission == null)
			return false;
		
		return member.getAccessLevel().value >= permission.getRoleRequired().value;
		
	}
	
	@Override
	public default RBACSubject assignOnProject(RBACSubject subject, RBACRole role, String projectId) throws IOException {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if(group == null) {
				throw new IOException("Project unkown "+ projectId);
			}
			try {
				Member member = getGitLabApi().getGroupApi().getMember(group.getId(), subject.getUserId());
				if(member.getAccessLevel() != AccessLevel.OWNER && 
						member.getAccessLevel().value.intValue() != role.value.intValue()) {
					if(role.value < RBACRole.ASSISTANT.value){
						assignDefaultAccessToRootProject(subject, group.getId());
					}
					return updateGroupMember(subject, role, group.getId());
				} else {
					// Role is either OWNER which means we would oust the owner, or it is already the same.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
			} catch (GitLabApiException e) {
				if(role.value < RBACRole.ASSISTANT.value){
					assignDefaultAccessToRootProject(subject, group.getId());
				}
				return createGroupMember(subject, role, group.getId());
			}
		} catch (GitLabApiException e) {
				throw new IOException("Project unkown "+ projectId,e);
		}	
	};
	
	@Override
	public default void unassignFromProject(RBACSubject subject, String projectId) throws IOException {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(projectId);
			if(group == null) {
				throw new IOException("Project unkown "+ projectId);
			}
			getGitLabApi().getGroupApi().removeMember(group.getId(), subject.getUserId());
		} catch (GitLabApiException e) {
			throw new IOException("Project or user unkown: "+ projectId + "subject:" + subject, e);
		}	
	}
	
	@Override
	public default void unassignFromResource(RBACSubject subject, IdentifiableResource resource) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(resource.getProjectId(), resource.getResourceId());
			if(project == null) {
				throw new IOException("Resource unkown "+ resource.getResourceId());
			}
			getGitLabApi().getProjectApi().removeMember(project.getId(), subject.getUserId());
		} catch (GitLabApiException e) {
			throw new IOException("Project or user unkown: "+ resource.getResourceId() + "subject:" + subject, e);
		}	
	}
	
	@Override
	public default RBACSubject assignOnResource(RBACSubject subject, RBACRole role, IdentifiableResource resource) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(resource.getProjectId(), resource.getResourceId());
			
			if(project == null) {
				throw new IOException("resource or rootproject unkown "+ resource.getResourceId());
			}
			try {
				Member member = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
				if(member.getAccessLevel() != AccessLevel.OWNER &&
						member.getAccessLevel().value.intValue() != role.value.intValue()){
					return updateProjectMember(subject, role, project.getId());
				} else {
					// Role is either OWNER which means we would oust the owner, or it is already the same.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
			} catch (GitLabApiException e) {
				return createProjectMember(subject, role, project.getId());
			}
		} catch (GitLabApiException e) {
				throw new IOException("Project unkown resourceId: "+ resource.getResourceId() + " groupId: "+ resource.getProjectId(),e);
		}		
	};
	
	public default de.catma.user.Member createGroupMember(RBACSubject subject, RBACRole role, Integer groupId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getGroupApi().addMember(groupId, subject.getUserId(), AccessLevel.forValue(role.value)));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ groupId,e);
		}		
	}
	
	public default RBACSubject assignDefaultAccessToRootProject(RBACSubject subject, Integer groupId) throws IOException {
		try {
			Group group = getGitLabApi().getGroupApi().getGroup(groupId);
			Project rootProject = getGitLabApi().getProjectApi().getProject(group.getName(), GitProjectManager.getProjectRootRepositoryName(group.getName()));
			
			try {
				Member member = getGitLabApi().getProjectApi().getMember(rootProject.getId(), subject.getUserId());
				if(member.getAccessLevel().value < RBACRole.ASSISTANT.value ){
					return new GitMember(getGitLabApi().getProjectApi().updateMember(rootProject.getId(), subject.getUserId(), AccessLevel.forValue(RBACRole.ASSISTANT.value)));
				} else {
					// Role is either OWNER which means we would oust the owner, or it is already the same.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
			} catch (GitLabApiException e) {
				return new GitMember(getGitLabApi().getProjectApi().addMember(rootProject.getId(), subject.getUserId(), AccessLevel.forValue(RBACRole.ASSISTANT.value)));
			}
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ groupId,e);
		}	
	}
	
	public default de.catma.user.Member updateGroupMember(RBACSubject subject, RBACRole role, Integer groupId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getGroupApi().updateMember(groupId, subject.getUserId(), AccessLevel.forValue(role.value)));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ groupId, e);
		}		
	}
	
	public default de.catma.user.Member createProjectMember(RBACSubject subject, RBACRole role, Integer projectId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getProjectApi().addMember(projectId, subject.getUserId(), AccessLevel.forValue(role.value)));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ projectId,e);
		}		
	}
	
	
	public default de.catma.user.Member updateProjectMember(RBACSubject subject, RBACRole role, Integer projectId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getProjectApi().updateMember(projectId, subject.getUserId(), AccessLevel.forValue(role.value)));
		} catch (GitLabApiException e) {
			throw new IOException("Project unkown "+ projectId, e);
		}		
	}
	
	@Override
	public default RBACRole getRoleOnResource(RBACSubject subject, IdentifiableResource resource) throws IOException {
		try {
			Project project = getGitLabApi().getProjectApi().getProject(resource.getProjectId(), resource.getResourceId());
			if(project == null) {
				throw new IOException("resource or rootproject unkown "+ resource.getResourceId());
			}
				Member member = getGitLabApi().getProjectApi().getMember(project.getId(), subject.getUserId());
			
			if(member == null ){
				throw new IOException("member not found " + subject);
			}
			return RBACRole.forValue(member.getAccessLevel().value);
			
		} catch (GitLabApiException e) {
			throw new IOException("resource or rootproject unkown "+ resource, e);
		}		
	}
	
	@Override
	public default RBACRole getRoleOnProject(RBACSubject subject, String projectId) throws IOException {
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
