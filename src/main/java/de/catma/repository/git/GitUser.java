package de.catma.repository.git;

import de.catma.user.User;

public class GitUser implements User {
	
	private org.gitlab4j.api.models.User gitlabUser;
	
	public GitUser(org.gitlab4j.api.models.User gitlabUser) {
		super();
		this.gitlabUser = gitlabUser;
	}

	@Override
	public Long getUserId() {
		return gitlabUser.getId();
	}

	@Override
	public String getIdentifier() {
		return gitlabUser.getUsername();
	}

	@Override
	public String getName() {
		return gitlabUser.getName();
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getEmail() {
		return gitlabUser.getEmail();
	}
}
