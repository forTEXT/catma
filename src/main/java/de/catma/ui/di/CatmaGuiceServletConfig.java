package de.catma.ui.di;

import javax.servlet.annotation.WebListener;

import com.google.gwt.dev.util.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

import de.catma.ui.CatmaApplication;
import de.catma.ui.CatmaApplicationServlet;
import de.catma.ui.CatmaUIProvider;

@WebListener
public class CatmaGuiceServletConfig extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(
				new ServletModule(){
					protected void configureServlets() {
						serve("/*").with(CatmaApplicationServlet.class,Maps.create("loadOnStartup", "0"));
						bind(UI.class).to(CatmaApplication.class).in(ServletScopes.SESSION);
						bind(UIProvider.class).to(CatmaUIProvider.class);
					};
				}
				, new GitlabModule(), new BootstrapModule(), new UIModule()
				);
	}

}
