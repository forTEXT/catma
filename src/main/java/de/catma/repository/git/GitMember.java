package de.catma.repository.git;

import de.catma.rbac.RBACRole;
import de.catma.user.Member;
import de.catma.user.Permission;

/**
 * A user implementation for a gitlab member
 */
public class GitMember implements Member {

    private org.gitlab4j.api.models.Member delegate;

    public GitMember(org.gitlab4j.api.models.Member member){
        this.delegate = member;
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
