package de.catma.repository.git;

import de.catma.rbac.RBACRole;
import de.catma.user.Member;
import de.catma.user.Permission;

/**
 * A user implementation for a gitlab member
 */
public class GitMember implements Member {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delegate.getId() == null) ? 0 : delegate.getId().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitMember other = (GitMember) obj;
		if (delegate.getId() == null) {
			if (other.delegate.getId() != null)
				return false;
		} else if (!delegate.getId().equals(other.delegate.getId()))
			return false;
		return true;
	}

	private final org.gitlab4j.api.models.Member delegate;
	private final RBACRole role;
	private final Integer id;

    public GitMember(org.gitlab4j.api.models.Member member){
        this.delegate = member;
        this.role = RBACRole.forValue(delegate.getAccessLevel().value);
        this.id = delegate.getId();
    }

    @Override
    public Integer getUserId() {
        return delegate.getId();
    }

    @Override
    public String getIdentifier() {
        return delegate.getUsername();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getEmail() {
        return "/nonexistent";
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isGuest() {
        return delegate.getAccessLevel().value == RBACRole.GUEST.value;
    }

    @Override
    public boolean isSpawnable() {
        return false;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return true;
    }
    
    public RBACRole getRole(){
    	return RBACRole.forValue(delegate.getAccessLevel().value);
    }
}
