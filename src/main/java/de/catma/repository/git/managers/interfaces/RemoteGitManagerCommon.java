package de.catma.repository.git.managers.interfaces;

import de.catma.rbac.RBACManager;

/**
 * RemoteGitManagerCommon is part of a set of interfaces that are related to the interactions with the remote Git server which acts as CATMA's project and user
 * management backend.
 * <p>
 * There are three RemoteGitManager... interfaces:
 * <ul>
 * <li>RemoteGitManagerCommon
 * <li>{@link RemoteGitManagerPrivileged}
 * <li>{@link RemoteGitManagerRestricted}
 * </ul>
 *
 * Together, these represent the interface between CATMA and the API of the remote Git server. In theory, they make it possible to switch to a different Git
 * server without massive effort, however in reality, CATMA is in many respects tightly coupled to GitLab at the time of writing this.
 * <p>
 * Functions are defined in one of the three interfaces according to the contexts in which they are used (e.g. system actions vs. those of specific users) and
 * the level of access required to interact with the corresponding API methods (admin vs. regular user).
 * RemoteGitManagerPrivileged contains functions that require privileged (admin) access, while RemoteGitManagerRestricted contains functions that are used in
 * the context of a regular logged-in user. Conceptually, RemoteGitManagerCommon should contain functions that are used in both the system and user contexts,
 * however there are very few of these in practice. The functions of the {@link RBACManager} interface, which this interface extends, are almost exclusively
 * used only in the "restricted" context, but are all defined in this interface's implementation so that they can be kept together.
 */
public interface RemoteGitManagerCommon extends RBACManager {

}
