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

    NONE(0), GUEST(10), REPORTER(20), DEVELOPER(30), MASTER(40), OWNER(50);

    public final Integer value;

    RBACRole(int value) {
        this.value = value;
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
        return (value.toString());
    }
}
