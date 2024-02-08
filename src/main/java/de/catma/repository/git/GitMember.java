package de.catma.repository.git;

import java.util.Objects;

import de.catma.rbac.RBACRole;
import de.catma.user.Member;

/**
 * A user implementation for a gitlab member
 */
public class GitMember implements Member {

	private final org.gitlab4j.api.models.Member delegate;	

	@Override
	public int hashCode() {
		return Objects.hash(getUserId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof GitMember))
			return false;
		GitMember other = (GitMember) obj;
		return Objects.equals(getUserId(), other.getUserId());
	}

	public GitMember(org.gitlab4j.api.models.Member member){
        this.delegate = member;
    }

    @Override
    public Long getUserId() {
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
    
    public RBACRole getRole(){
    	return RBACRole.forValue(delegate.getAccessLevel().value);
    }
    
    @Override
	public String toString() {
		return getName();
	}
    
    @Override
    public int compareTo(Member o) {
    	
    	String n1 = this.getName();
    	if (n1 == null) {
    		n1 = this.getIdentifier();
    	}
    	String n2 = o.getName();
    	if (n2 == null) {
    		n2 = o.getIdentifier();
    	}
    	
    	if (n1 != null && n2 != null && !n1.equals(n2)) {
    		return n1.compareTo(n2);
    	}
    	
    	
    	
    	return this.getIdentifier().compareTo(o.getIdentifier());
    }
}
