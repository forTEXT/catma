package de.catma.ui.login;

import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;

import de.catma.backgroundservice.BackgroundService;
import de.catma.hazelcast.HazelCastService;

public interface InitializationService {	
	BackgroundService accuireBackgroundService();
	String accquirePersonalTempFolder() throws IOException;
	Component newEntryPage(EventBus eventBus,
			LoginService loginService, HazelCastService hazelcastService) throws IOException;
	
	void shutdown();
}
