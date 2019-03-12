package de.catma.ui.di;

import static com.google.inject.servlet.ServletScopes.SESSION;

import javax.servlet.annotation.WebListener;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;
import com.google.inject.servlet.SessionScoped;
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
						bind(UI.class).to(CatmaApplication.class).in(VaadinUIScope.VAADINUI);
						bind(UIProvider.class).to(CatmaUIProvider.class);
					    bindScope(VaadinUIScoped.class, VaadinUIScope.VAADINUI);
					    
						serve("/*").with(CatmaApplicationServlet.class, ImmutableMap.<String,String>builder()
								.put("loadOnStartup", "0")
								.put("async-supported", "true")
								.put("org.atmosphere.container.JSR356AsyncSupport.mappingPath","/PUSH")
								.build());
					};
				}
				, new GitlabModule(), new BootstrapModule(), new UIModule()
				);
	}

}
