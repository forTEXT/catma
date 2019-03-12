package de.catma.ui.di;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import de.catma.backgroundservice.BackgroundService;
import de.catma.ui.UIBackgroundService;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.Vaadin8InitializationService;

public class BootstrapModule extends AbstractModule {

	@Vaadin8Type
	@Provides 
	@Inject
	@VaadinUIScoped
	InitializationService provideVaadin8InitializationService(Injector injector){
		return new Vaadin8InitializationService(injector); 
	}
	

	@Provides
	@VaadinUIScoped
	EventBus provideEventBus(){
		return new EventBus();
	}

	@Provides
	@VaadinUIScoped
	BackgroundService provideBackgroundService(){
		return new UIBackgroundService(true);
	}

}
