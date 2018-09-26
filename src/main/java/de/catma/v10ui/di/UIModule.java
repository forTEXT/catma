package de.catma.v10ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.vaadin.guice.annotation.UIScope;
import de.catma.repository.git.GitProjectManager;
import de.catma.v10ui.modules.analyze.AnalyzeView;
import de.catma.v10ui.modules.dashboard.DashboardView;
import de.catma.v10ui.modules.main.ErrorLogger;
import de.catma.v10ui.projects.ProjectTilesView;

/**
 * Provides all views
 */
public class UIModule extends AbstractModule {

    @Provides
    @UIScope
    ProjectTilesView provideProjects(GitProjectManager gitProjectManager){
        return new ProjectTilesView(gitProjectManager);
    }

    @Provides
    @UIScope
    public AnalyzeView provideAnalyzeView(){
        return new AnalyzeView();
    }

    @Provides
    public DashboardView provideDashboardView(GitProjectManager gitProjectManager, ErrorLogger errorLogger){
        return new DashboardView(gitProjectManager, errorLogger);
    }
}
