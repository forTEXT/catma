package de.catma.v10ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.vaadin.guice.annotation.UIScope;
import de.catma.v10ui.modules.analyze.AnalyzeView;

public class AnalyzeModule extends AbstractModule {

    @Provides @UIScope
    public AnalyzeView provideAnalyzeView(){
        return new AnalyzeView();
    }
}
