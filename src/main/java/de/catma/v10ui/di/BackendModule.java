package de.catma.v10ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.vaadin.guice.annotation.UIScope;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.git.GitProjectManager;

import java.io.IOException;

import static de.catma.v10ui.authentication.DummyAuth.DUMMYIDENT;

/**
 * Provides all backend Objects
 *
 */
public class BackendModule extends AbstractModule {


    //TODO: remove DUMMYIdent and do proper authentication using DI with guice
    @Provides
    @UIScope
    GitProjectManager provideGitRepoManager() throws IOException {
        return new GitProjectManager( RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(), DUMMYIDENT);
    }

}
