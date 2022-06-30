package de.catma.repository.git;

import de.catma.rbac.RBACRole;
import de.catma.user.Member;

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
