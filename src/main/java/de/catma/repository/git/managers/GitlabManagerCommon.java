package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;

import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitMember;

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
	public default boolean isAuthorizedOnResource(RBACSubject subject, RBACPermission permission, Integer resourceId) {
		try {
	//TODO: not working YET 
//			Project project = restrictedGitLabApi.getProjectApi().getProject(namespace, project)(projectId);

			return isMemberAuthorized(permission,getGitLabApi().getProjectApi().getMember(resourceId, subject.getUserId()));
		} catch (GitLabApiException e) {
			getLogger().log(Level.WARNING, "Can't retrieve permissions from resource: "+ resourceId,e);
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
						member.getAccessLevel().value.intValue() != role.value.intValue()){
					return updateGroupMember(subject, role, group.getId());
				} else {
					// Role is either OWNER which means we would oust the owner, or it is already the same.
					// In both cases we refuse to update the role, and simply do nothing.
					return subject;
				}
			} catch (GitLabApiException e) {
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
	public default RBACSubject assignOnResource(RBACSubject subject, RBACRole role, Integer resourceId) throws IOException {
		throw new RuntimeException("not implemented yet");
	};
	
	public default de.catma.user.Member createGroupMember(RBACSubject subject, RBACRole role, Integer groupId) throws IOException {
		try {
			return new GitMember(getGitLabApi().getGroupApi().addMember(groupId, subject.getUserId(), AccessLevel.forValue(role.value)));
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
}
