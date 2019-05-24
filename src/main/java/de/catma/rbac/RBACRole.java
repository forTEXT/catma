package de.catma.rbac;

import java.util.HashMap;
import java.util.Map;

/**
 * Roles which are mapped to gitlabs AccessLevel
 * 
 * @author db
 *
 */
public enum RBACRole {

    NONE(0,"None"), TRAINEE(10,"Trainee"), STUDENT(20,"Student"), ASSISTANT(30,"Assistant"), MAINTAINER(40,"Maintainer"), OWNER(50,"Owner");

    public final Integer value;
    public final String roleName;

    RBACRole(int value, String roleName) {
        this.value = value;
        this.roleName = roleName;
    }

    private static Map<Integer, RBACRole> valuesMap = new HashMap<Integer, RBACRole>(6);
    static {
        for (RBACRole role : RBACRole.values())
            valuesMap.put(role.value, role);
    }

    public static RBACRole forValue(Integer value) {
        return valuesMap.get(value);
    }

    public Integer toValue() {
        return (value);
    }

    public String toString() {
        return roleName;
    }
    
    public String getRolename() {
    	return roleName;
	}
}
