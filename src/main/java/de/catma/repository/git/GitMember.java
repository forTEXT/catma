package de.catma.repository.git;

import de.catma.user.Permission;
import de.catma.user.User;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;

/**
 * A user implementation for a gitlab member
 */
public class GitMember implements User {

    private Member delegate;

    public GitMember(Member member){
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
        return delegate.getEmail();
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isGuest() {
        return delegate.getAccessLevel() == AccessLevel.GUEST;
    }

    @Override
    public boolean isSpawnable() {
        return false;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return true;
    }
}
