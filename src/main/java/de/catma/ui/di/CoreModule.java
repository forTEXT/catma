package de.catma.ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import de.catma.hazelcast.HazelCastService;
import de.catma.rbac.IRBACManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.ui.login.GitlabLoginService;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.login.Vaadin8InitializationService;

public class CoreModule extends AbstractModule {

	@Override
	protected void configure() {
		super.configure();
		  install(new FactoryModuleBuilder()
				.implement(IRemoteGitManagerRestricted.class, GitlabManagerRestricted.class)
				.build(IRemoteGitManagerFactory.class)
				  );
		  bind(LoginService.class).to(GitlabLoginService.class).in(VaadinUIScoped.class);
		  bind(InitializationService.class).to(Vaadin8InitializationService.class).in(VaadinUIScoped.class);
	}
	
	@Provides
	@VaadinUIScoped
	IRemoteGitManagerPrivileged providePrivilegedGitManager(){
		return new GitlabManagerPrivileged();
	}
	
	@Provides
	@VaadinUIScoped
	@Inject
	IRemoteGitManagerRestricted provideCurrentGitManager(LoginService loginService){
		return loginService.getAPI();
	}
	
	
	@Provides
	@VaadinUIScoped
	@Inject
	IRBACManager provideRBACManager(LoginService loginService){
		return loginService.getAPI();
	}
	
	
	@Provides
	@Singleton
	HazelCastService provideHazelcastService(){
		return new HazelCastService();
	}
}