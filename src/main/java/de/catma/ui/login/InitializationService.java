package de.catma.ui.login;

import java.io.IOException;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;

import de.catma.backgroundservice.BackgroundService;
import de.catma.hazelcast.HazelCastService;
import de.catma.sqlite.SqliteService;

public interface InitializationService {	
	BackgroundService acquireBackgroundService();
	String acquirePersonalTempFolder() throws IOException;
	Component newEntryPage(EventBus eventBus, LoginService loginService, HazelCastService hazelcastService, SqliteService sqliteService);
	
	void shutdown();
}
