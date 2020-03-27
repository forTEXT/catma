package de.catma.ui.login;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;

import de.catma.backgroundservice.BackgroundService;
import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.ui.UIBackgroundService;
import de.catma.ui.module.main.CatmaHeader;
import de.catma.ui.module.main.MainView;
import de.catma.ui.module.main.NotLoggedInMainView;

public class Vaadin8InitializationService implements InitializationService {

	
	private BackgroundService backgroundService;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public BackgroundService accuireBackgroundService() {
		if(backgroundService == null) {
			backgroundService = new UIBackgroundService(true);
		}
		return backgroundService;
	}
	
	@Override
	public void shutdown() {
		if(backgroundService != null) {
			backgroundService.shutdown();
		}
	}
	

	@Override
	public String accquirePersonalTempFolder() throws IOException {
		String tmpdir = (String)VaadinSession.getCurrent().getAttribute("TempDir");
		if(tmpdir == null){
			String result;
			String tempDirProp = CATMAPropertyKey.TempDir.getValue();
			File tempDir = new File(tempDirProp);
	
			if (!tempDir.isAbsolute()) {
				result = VaadinServlet.getCurrent().getServletContext().getRealPath(// TODO:
																								// check
						"WEB-INF" + System.getProperty("file.separator") //$NON-NLS-1$
								+ tempDirProp);
			} else {
				result = tempDirProp;
			}
	
			tempDir = new File(result);
			if ((!tempDir.exists() && !tempDir.mkdirs())) {
				throw new IOException("could not create temporary directory: " + result); //$NON-NLS-1$
			}
			VaadinSession.getCurrent().setAttribute("TempDir",result);
			return result;
		} else {
			return tmpdir;
		}
	}

	@Override
	public Component newEntryPage(
		EventBus eventBus,
		LoginService loginService, HazelCastService hazelcastService) throws IOException {
		
		IRemoteGitManagerRestricted api = loginService.getAPI();

		if(api != null ){
			GitProjectManager projectManager = new GitProjectManager(
					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
					api,
					(projectId) -> {}, //noop deletion handler
					accuireBackgroundService(),
					eventBus);
			
			hazelcastService.start();
			GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
			boolean termsOfUseConsentGiven = 
				gitlabManagerPrivileged.updateLastLoginAndGetTermsOfUseConsent(api.getUser());
			return new MainView(
					projectManager, 
					new CatmaHeader(eventBus, loginService, gitlabManagerPrivileged), 
					eventBus,
					api,
					loginService,
					termsOfUseConsentGiven,
					consent -> gitlabManagerPrivileged.setTermsOfUseConsentGiven(api.getUser(), consent));
		} else {
			return new NotLoggedInMainView(this, loginService, hazelcastService, eventBus);

		}
	}
}
