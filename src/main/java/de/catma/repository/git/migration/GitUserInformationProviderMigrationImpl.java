package de.catma.repository.git.migration;

import de.catma.repository.git.managers.interfaces.GitUserInformationProvider;
import org.gitlab4j.api.GitLabApi;

import java.io.IOException;

public class GitUserInformationProviderMigrationImpl implements GitUserInformationProvider {
    private final GitLabApi gitLabApi;

    public GitUserInformationProviderMigrationImpl(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }

    @Override
    public String getUsername() {
        return null; // not needed for migrations
    }

    @Override
    public String getPassword() {
        return gitLabApi.getAuthToken();
    }

    @Override
    public String getEmail() {
        return null; // not needed for migrations
    }

    @Override
    public void refreshUserCredentials() throws IOException {
        throw new IOException("Not implemented"); // not needed for migrations
    }
}
