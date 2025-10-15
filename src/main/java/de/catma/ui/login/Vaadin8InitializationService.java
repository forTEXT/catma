package de.catma.ui.login;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import de.catma.backgroundservice.BackgroundService;
import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.sqlite.SqliteService;
import de.catma.ui.UIBackgroundService;
import de.catma.ui.module.main.CatmaHeader;
import de.catma.ui.module.main.MainView;
import de.catma.ui.module.main.NotLoggedInMainView;
import de.catma.user.User;

import java.io.File;
import java.io.IOException;

public class Vaadin8InitializationService implements InitializationService {
	private BackgroundService backgroundService;

	@Override
	public BackgroundService acquireBackgroundService() {
		if (backgroundService == null) {
			backgroundService = new UIBackgroundService(true);
		}
		return backgroundService;
	}

	@Override
	public void shutdown() {
		if (backgroundService != null) {
			backgroundService.shutdown();
		}
	}

	@Override
	public String acquirePersonalTempFolder() throws IOException {
		String tempDirSessionAttribute = (String)VaadinSession.getCurrent().getAttribute("TempDir");

		if (tempDirSessionAttribute != null) {
			return tempDirSessionAttribute;
		}

		File tempDir = new File(CATMAPropertyKey.TEMP_DIR.getValue());

		if (!tempDir.isAbsolute()) {
			// TODO: check if this works
			tempDir = new File(
					VaadinServlet.getCurrent().getServletContext().getRealPath(
							"WEB-INF" + System.getProperty("file.separator") + CATMAPropertyKey.TEMP_DIR.getValue()
					)
			);
		}

		if (!tempDir.exists() && !tempDir.mkdirs()) {
			throw new IOException(String.format("Failed to create temp directory at path %s", tempDir.getAbsolutePath()));
		}

		VaadinSession.getCurrent().setAttribute("TempDir", tempDir.getAbsolutePath());

		return tempDir.getAbsolutePath();
	}

	@Override
	public Component newEntryPage(EventBus eventBus, LoginService loginService, HazelCastService hazelcastService, SqliteService sqliteService) {
		RemoteGitManagerRestricted remoteGitManagerRestricted = loginService.getRemoteGitManagerRestricted();

		if (remoteGitManagerRestricted == null ) {
			return new NotLoggedInMainView(this, loginService, hazelcastService, sqliteService, eventBus);
		}

		GitProjectsManager gitProjectsManager = new GitProjectsManager(
				CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(),
				remoteGitManagerRestricted,
				(projectReference) -> {}, // noop deletion handler, currently there is no persistent project on the graph level
				acquireBackgroundService(),
				eventBus
		);

		hazelcastService.start();

		User user = remoteGitManagerRestricted.getUser();
		GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
		boolean termsOfUseConsentGiven = gitlabManagerPrivileged.updateLastLoginAndGetTermsOfUseConsent(user);

		return new MainView(
				gitProjectsManager,
				new CatmaHeader(eventBus, loginService, gitlabManagerPrivileged, remoteGitManagerRestricted),
				eventBus,
				loginService,
				termsOfUseConsentGiven,
				consent -> gitlabManagerPrivileged.setTermsOfUseConsentGiven(user, consent)
		);
	}
}
