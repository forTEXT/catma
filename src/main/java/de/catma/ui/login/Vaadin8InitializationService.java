package de.catma.ui.login;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.UIBackgroundService;
import de.catma.ui.modules.main.MainView;
import de.catma.ui.modules.main.NotLoggedInMainView;

public class Vaadin8InitializationService implements InitializationService {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public BackgroundService accuireBackgroundService() {
		BackgroundService bs = VaadinSession.getCurrent().getAttribute(BackgroundService.class);
		if(bs == null) {
			bs = new UIBackgroundService(true);
			VaadinSession.getCurrent().setAttribute(BackgroundService.class,bs);
		}
		return bs;
	}
	
	@Override
	public void shutdown() {
		BackgroundService bs = VaadinSession.getCurrent().getAttribute(BackgroundService.class);
		if(bs != null) {
			bs.shutdown();
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
	public Component newEntryPage(LoginService loginService) throws IOException {
		Optional<IRemoteGitManagerRestricted> api = loginService.getAPI();

		if(api.isPresent()){
			GitProjectManager projectManager = new GitProjectManager(
					RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
					api.get(),
					accuireBackgroundService());
			return new MainView(projectManager);
		} else {
			return new NotLoggedInMainView(this,loginService);

		}
	}
}
