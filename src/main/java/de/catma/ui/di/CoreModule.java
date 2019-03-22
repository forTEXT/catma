package de.catma.ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import de.catma.rbac.IRBACManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.ui.login.GitlabLoginService;
import de.catma.ui.login.LocalUserLoginService;
import de.catma.ui.login.LoginService;

public class CoreModule extends AbstractModule {

	@Override
	protected void configure() {
		super.configure();
		  install(new FactoryModuleBuilder()
				.implement(IRemoteGitManagerRestricted.class, GitlabManagerRestricted.class)
				.build(IRemoteGitManagerFactory.class));
	}
	
	@Provides
	@VaadinUIScoped
	IRemoteGitManagerPrivileged providePrivilegedGitManager(){
		return new GitlabManagerPrivileged();
		
	}
	
	@Provides
	@VaadinUIScoped
	@Inject
	@GitLabType LoginService provideGitlabLoginService(IRemoteGitManagerFactory iRemoteGitManagerFactory){
		return new GitlabLoginService(iRemoteGitManagerFactory);
	}

	@Provides
	@VaadinUIScoped
	@Inject
	@LocalUserLoginType LoginService provideFakeLoginService(IRemoteGitManagerFactory iRemoteGitManagerFactory){
		return new LocalUserLoginService(iRemoteGitManagerFactory);
	}
	
	@Provides
	@VaadinUIScoped
	@Inject
	IRBACManager provideRBACManager(LoginService loginService){
		return loginService.getAPI();
	}
	
	
}