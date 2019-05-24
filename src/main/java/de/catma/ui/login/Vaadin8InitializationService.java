package de.catma.ui.login;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.hazelcast.HazelCastService;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.di.UIFactory;
import de.catma.ui.modules.main.NotLoggedInMainView;

public class Vaadin8InitializationService implements InitializationService {

	private final Injector injector;
	
	private BackgroundService backgroundService;

	@Inject
	public Vaadin8InitializationService(Injector injector) {
		this.injector = injector;
	}
	
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public BackgroundService accuireBackgroundService() {
		if(backgroundService == null) {
			backgroundService = injector.getInstance(BackgroundService.class);
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
			String tempDirProp = RepositoryPropertyKey.TempDir.getValue();
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
	@Inject
	public Component newEntryPage(LoginService loginService, HazelCastService hazelcastService) throws IOException {
		IRemoteGitManagerRestricted api = loginService.getAPI();

		if(api != null ){
			GitProjectManager projectManager = new GitProjectManager(
					RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
					api,
					(projectId) -> {}, //noop deletion handler
					accuireBackgroundService(),
					injector.getInstance(EventBus.class));

			hazelcastService.start();
			return injector.getInstance(UIFactory.class).getMainview(projectManager, api);
		} else {
			return new NotLoggedInMainView(this, loginService, hazelcastService, injector.getInstance(EventBus.class));

		}
	}
}
