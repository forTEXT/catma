package de.catma.ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.catma.ui.login.InitializationService;
import de.catma.ui.login.Vaadin8InitializationService;

public class Vaadin8Module extends AbstractModule {

	@Vaadin8Type
	@Provides 
	InitializationService provideVaadin8InitializationService(){
		return new Vaadin8InitializationService(); 
	}
}
