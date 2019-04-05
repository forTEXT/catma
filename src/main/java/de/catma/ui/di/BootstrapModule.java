package de.catma.ui.di;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.catma.backgroundservice.BackgroundService;
import de.catma.ui.UIBackgroundService;

public class BootstrapModule extends AbstractModule {

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
