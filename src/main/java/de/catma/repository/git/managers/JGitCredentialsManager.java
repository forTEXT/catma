package de.catma.repository.git.managers;

import de.catma.repository.git.managers.interfaces.GitUserInformationProvider;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;

public class JGitCredentialsManager {
    private final GitUserInformationProvider gitUserInformationProvider;

    public JGitCredentialsManager(GitUserInformationProvider gitUserInformationProvider) {
        this.gitUserInformationProvider = gitUserInformationProvider;
    }

    public CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider("oauth2", gitUserInformationProvider.getPassword());
    }

    public CredentialsProvider refreshTransientCredentialsAndGetNewProvider() throws IOException {
        gitUserInformationProvider.refreshUserCredentials();
        return new UsernamePasswordCredentialsProvider("oauth2", gitUserInformationProvider.getPassword());
    }
}
