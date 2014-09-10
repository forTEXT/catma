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
package de.catma.servlet;

import javax.servlet.ServletException;

import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;

public class CatmaApplicationServlet extends VaadinServlet {
	
	private enum JsLib {
//		JQUERY("jquery/jquery-1.7.2.min.js"),
		HIGHCHARTS_SL("highcharts/standalone-framework-4.0.3.js"),
		HIGHCHARTS("highcharts/highcharts-4.0.3.js"),
//		EXPORTING("highcharts/exporting.js"),
//		D3("doubletreejs_debug/d3.v3.js"),
//		CLASSLISTSUBSTITUTE("doubletreejs_debug/classListSubstitute.js"),
//		DOUBLETREE("doubletreejs_debug/DoubleTree.js"),
//		DT_TRIE("doubletreejs_debug/Trie.js"),
		D3("doubletreejs/d3.min.js"),
		CLASSLISTSUBSTITUTE("doubletreejs/classListSubstitute.min.js"),
		DOUBLETREE("doubletreejs/DoubleTree.min.js"),
		DT_TRIE("doubletreejs/Trie.min.js"),
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
			scriptBuilder.append("//]]>\n</script>\n");
			
			response.getDocument().body().prepend(scriptBuilder.toString());
		}
	}
	
	@Override
	protected void servletInitialized() throws ServletException {
		super.servletInitialized();
		getService().addSessionInitListener(new SessionInitListener() {
			
			@Override
			public void sessionInit(SessionInitEvent event) throws ServiceException {
				event.getSession().addBootstrapListener(new CatmaBootstrapListener());
			}
		});
	}
}
