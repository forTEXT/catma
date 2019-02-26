package de.catma.ui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.catma.ui.login.GitlabLoginService;
import de.catma.ui.login.LocalUserLoginService;
import de.catma.ui.login.LoginService;

public class GitlabModule extends AbstractModule {

	@Provides
	@GitLabType LoginService provideGitlabLoginService(){
		return new GitlabLoginService();
	}

	@Provides
	@LocalUserLoginType LoginService ProvideFakeLoginService(){
		return new LocalUserLoginService();
	}
	
}