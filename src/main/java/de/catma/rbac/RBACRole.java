package de.catma.rbac;

/**
 * Roles which are mapped to gitlabs AccessLevel
 * 
 * @author db
 *
 */
public enum RBACRole {

    NONE(0,"None"), 
    GUEST(10,"Student"), 
    REPORTER(20,"Observer"), 
    ASSISTANT(30,"Assistant"), // Developer, default role for CATMA project participants
    MAINTAINER(40,"Partner"), // Maintainer, allowed to manage the team members, suitable for tutors
    OWNER(50,"Owner");

    private final int accessLevel;
    private final String roleName;

    private RBACRole(int value, String roleName) {
        this.accessLevel = value;
        this.roleName = roleName;
    }

    public static RBACRole forValue(int accessLevel) {
    	for (RBACRole role : values()) {
    		if (role.getAccessLevel() == accessLevel) {
    			return role;
    		}
    	}
        throw new IllegalArgumentException("Unknown accesslevel " + accessLevel);
    }

    public int getAccessLevel() {
        return (accessLevel);
    }

    public String toString() {
        return roleName;
    }
    
    public String getRoleName() {
    	return roleName;
	}
}
