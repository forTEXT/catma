package de.catma.repository.git;

import de.catma.user.Permission;
import de.catma.user.User;

public class GitUser implements User {
	
	private org.gitlab4j.api.models.User gitlabUser;
	
	public GitUser(org.gitlab4j.api.models.User gitlabUser) {
		super();
		this.gitlabUser = gitlabUser;
	}

	@Override
	public Integer getUserId() {
		return gitlabUser.getId();
	}

	@Override
	public String getIdentifier() {
		return gitlabUser.getUsername(); //TODO: rather combination of extern uid and provider...
	}

	@Override
	public String getName() {
		return gitlabUser.getName();
	}

	@Override
	public String getEmail() {
		return gitlabUser.getEmail();
	}
	
	@Override
	public boolean isLocked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGuest() {
		return false;
	}

	@Override
	public boolean isSpawnable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPermission(Permission permission) {
		// TODO Auto-generated method stub
		return true;
	}
	
}
