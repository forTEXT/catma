package de.catma.user.signup;

import de.catma.rbac.RBACRole;

public record ProjectSignupToken (String requestDate, String email, String namespace, String projectId, String projectName, RBACRole role, String token) {
}