package de.catma.v10ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.vaadin.guice.annotation.UIScope;
import de.catma.repository.git.GitProjectManager;
import de.catma.v10ui.projects.ProjectTilesView;

public class ProjectModule extends AbstractModule {

    @Provides @UIScope
    ProjectTilesView provideProjects(GitProjectManager gitProjectManager){
        return new ProjectTilesView(gitProjectManager);

    }

}
