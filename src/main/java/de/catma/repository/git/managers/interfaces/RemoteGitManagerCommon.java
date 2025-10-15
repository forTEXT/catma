package de.catma.repository.git.managers.interfaces;

import de.catma.rbac.RBACManager;
import org.gitlab4j.api.GitLabApi;

public interface RemoteGitManagerCommon extends RBACManager {
    GitLabApi getGitLabApi();
}
