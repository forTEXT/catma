package de.catma.ui.login;

import java.io.IOException;

import com.vaadin.ui.Component;

import de.catma.backgroundservice.BackgroundService;
import de.catma.hazelcast.HazelCastService;

public interface InitializationService {	
	BackgroundService accuireBackgroundService();
	String accquirePersonalTempFolder() throws IOException;
	Component newEntryPage(LoginService loginService, HazelCastService hazelcastService) throws IOException;
	
	void shutdown();
}
