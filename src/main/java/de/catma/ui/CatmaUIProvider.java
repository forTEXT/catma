package de.catma.ui;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

/**
 * provides a {@link CatmaApplication} via guice
 * @author db
 *
 */
public class CatmaUIProvider extends UIProvider {

    private Injector injector;
    
    @Inject
    public CatmaUIProvider(Injector injector) {
    	this.injector = injector;
    }
    
	@Override
	public CatmaApplication createInstance(UICreateEvent event) {
		return injector.getInstance(CatmaApplication.class);
	}
	
	@Override
	public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
		return CatmaApplication.class;
	}

}
