package de.catma.ui.di;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import de.catma.hazelcast.HazelCastService;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.ui.login.LoginService;
import de.catma.ui.modules.account.EditAccountDialog;
import de.catma.ui.modules.dashboard.JoinProjectDialog;
import de.catma.ui.modules.main.CatmaHeader;

public class UIModule extends AbstractModule {

	@Override
	protected void configure() {
		super.configure();
		  install(new FactoryModuleBuilder()
				.build(UIFactory.class));
	}
	@Provides
	@Inject
	public EditAccountDialog provideEditAccountDialog(IRemoteGitManagerPrivileged gitManagerPrivileged, 
			LoginService loginService, EventBus eventBus){
		return new EditAccountDialog(gitManagerPrivileged, loginService, eventBus);
	}
	
	@Provides
	@Inject
	public CatmaHeader provideCatmaHeader(EditAccountDialog editAccountDialog, EventBus eventBus, LoginService loginService){
		return new CatmaHeader(() -> editAccountDialog, eventBus,loginService);
	}
	
	@Provides
	@Inject
	JoinProjectDialog provideJoinProjectDialog(LoginService loginService, EventBus eventBus, HazelCastService hazelcastService){
		return new JoinProjectDialog(loginService.getAPI().getUser(), eventBus, hazelcastService);
	}

	
}
