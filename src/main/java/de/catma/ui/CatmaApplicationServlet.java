/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;

import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;

import de.catma.properties.CATMAPropertyKey;

public class CatmaApplicationServlet extends VaadinServlet {
	
	public static final class DelegateQueryResultRequestHandler implements RequestHandler {
		private final CopyOnWriteArrayList<RequestHandler> delegates;
		
		public DelegateQueryResultRequestHandler() {
			this.delegates = new CopyOnWriteArrayList<RequestHandler>();
		}
		
		@Override
		public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
				throws IOException {
			String requestPath = request.getPathInfo().toLowerCase();
			if (!delegates.isEmpty() 
					&& (requestPath.endsWith("/queryresult/selection.json") || requestPath.startsWith("/api"))) {
				// TODO: turn delegates into a map so that we can do exact path matches?
				for (RequestHandler handler : delegates) {
					if (handler.handleRequest(session, request, response)) {
						return true;
					}
				}
			}
			return false;
		}
		
		public void add(RequestHandler handler) {
			if (!delegates.contains(handler)) {
				delegates.add(handler);
			}
		}
		
		public void remove(RequestHandler handler) {
			delegates.remove(handler);
		}
	}
	
	private enum JsLib {
		D3("doubletreejs/d3.min.js"),
		CLASSLISTSUBSTITUTE("doubletreejs/classListSubstitute.min.js"),
		DOUBLETREE("doubletreejs/DoubleTree.min.js"),
		DT_TRIE("doubletreejs/Trie.min.js"),
		VEGA("vega/vega.js"),
		VEGA_LITE("vega/vega-lite.js"),
		VEGA_EMBED("vega/vega-embed.js"),
		;
		String relFilePath;

		private JsLib(String relFilePath) {
			this.relFilePath = relFilePath;
		}
		
		@Override
		public String toString() {
			return relFilePath;
		}
	}
	
	private enum CssLib {
		DOUBLETREE("doubletreejs/doubletree.css"),
		;
		String relFilePath;

		private CssLib(String relFilePath) {
			this.relFilePath = relFilePath;
		}
		
		@Override
		public String toString() {
			return relFilePath;
		}
	}
	
	private static class CatmaBootstrapListener implements BootstrapListener {
		@Override
		public void modifyBootstrapFragment(BootstrapFragmentResponse response) {
			// noop
		}
		@Override
		public void modifyBootstrapPage(BootstrapPageResponse response) {
			for (CssLib lib : CssLib.values()) {
				response.getDocument().head().append(
					"<link rel=\"stylesheet\" href=\"" 
					+ response.getRequest().getContextPath() +"/VAADIN/" + lib + "\" />");
			}

			StringBuilder scriptBuilder = new StringBuilder();
			
			scriptBuilder.append(
					"<script type=\"text/javascript\">\n");
			scriptBuilder.append("//<![CDATA[\n");
			for (JsLib lib : JsLib.values()) {
				scriptBuilder.append(
						"document.write(\"<script language='javascript' src='" 
								+ response.getRequest().getContextPath() 
								+ "/VAADIN/" + lib + "'><\\/script>\");\n");
			}
//			scriptBuilder.append(
//				"document.write(\"<script language='javascript' src='https://cdnjs.cloudflare.com/ajax/libs/vega/3.0.0-rc3/vega.js'><\\/script>\");\n");
//			scriptBuilder.append(
//				"document.write(\"<script language='javascript' src='https://cdnjs.cloudflare.com/ajax/libs/vega-lite/2.0.0-beta.10/vega-lite.js'><\\/script>\");\n");
//			scriptBuilder.append(
//				"document.write(\"<script language='javascript' src='https://cdnjs.cloudflare.com/ajax/libs/vega-embed/3.0.0-beta.19/vega-embed.js'><\\/script>\");\n");
			
			
			scriptBuilder.append("//]]>\n</script>\n");
			
			response.getDocument().body().prepend(scriptBuilder.toString());
		}
	}
	
	@Override
	protected void servletInitialized() throws ServletException {
		getService().addSessionInitListener(new SessionInitListener() {
			
			@Override
			public void sessionInit(SessionInitEvent event) throws ServiceException {
				event.getSession().addBootstrapListener(new CatmaBootstrapListener());
			}
		});
		
		getService().setSystemMessagesProvider(new SystemMessagesProvider() {
			@Override
			public SystemMessages getSystemMessages(
					SystemMessagesInfo systemMessagesInfo) {
				CustomizedSystemMessages messages = new CustomizedSystemMessages();
				try {
					String problemRedirectURL = 
							CATMAPropertyKey.BaseURL.getValue( 
									CATMAPropertyKey.BaseURL.getDefaultValue());

					messages.setAuthenticationErrorURL(problemRedirectURL);
					messages.setInternalErrorURL(problemRedirectURL);
					messages.setSessionExpiredURL(problemRedirectURL);
					messages.setCommunicationErrorURL(problemRedirectURL);
					messages.setCookiesDisabledURL(problemRedirectURL);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return messages;
			}
		});

	}
	
    protected DeploymentConfiguration createDeploymentConfiguration(Properties initParameters) {
        initParameters.setProperty("widgetset","de.catma.ui.CleaWidgetset" );
        initParameters.setProperty("productionMode", "true");
        initParameters.setProperty("closeIdleSessions", "true");
        initParameters.setProperty("pushMode", "manual");
        initParameters.setProperty("transport", "websocket-xhr");
        return super.createDeploymentConfiguration(initParameters);
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
    		throws ServiceException {
        VaadinServletService service = new VaadinServletService(this,
                deploymentConfiguration) {
        	protected java.util.List<com.vaadin.server.RequestHandler> createRequestHandlers() throws ServiceException {
        		java.util.List<com.vaadin.server.RequestHandler> handlers = super.createRequestHandlers();
        		handlers.add(new DelegateQueryResultRequestHandler());
        		return handlers;
        	};
        };
        service.init();
        return service;
    }

}
