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
package de.catma;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.ui.Window;

public class CatmaApplicationServlet extends ApplicationServlet {
	
	private enum JsLib {
		JQUERY("jquery/jquery-1.7.2.min.js"),
		HIGHCHARTS("highcharts/highcharts.js"),
//		EXPORTING("highcharts/exporting.js"),
//		D3("doubletreejs/d3.v3.js"),
//		CLASSLISTSUBSTITUTE("doubletreejs/classListSubstitute.js"),
//		DOUBLETREE("doubletreejs/DoubleTree.js"),
//		DT_TRIE("doubletreejs/Trie.js"),
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
	
	@Override
	protected void writeAjaxPageHtmlVaadinScripts(Window window,
			String themeName, Application application, BufferedWriter page,
			String appUrl, String themeUri, String appId,
			HttpServletRequest request) throws ServletException, IOException {
		page.write("<script type=\"text/javascript\">\n");
		page.write("//<![CDATA[\n");
		for (JsLib lib : JsLib.values()) {
			page.write(
					"document.write(\"<script language='javascript' src='" 
		    		+ request.getContextPath() 
		    		+ "/VAADIN/" + lib + "'><\\/script>\");\n");
		}
		
		page.write("//]]>\n</script>\n");
	      
		super.writeAjaxPageHtmlVaadinScripts(window, themeName, application, page,
				appUrl, themeUri, appId, request);
	}
	

	@Override
	protected void writeAjaxPageHtmlHeader(BufferedWriter page, String title,
			String themeUri, HttpServletRequest request) throws IOException {
		
		for (CssLib lib : CssLib.values()) {
			page.write(
				"<link rel=\"stylesheet\" href=\"" 
				+ request.getContextPath() +"/VAADIN/" + lib + "\" />");
		}
		
		super.writeAjaxPageHtmlHeader(page, title, themeUri, request);
	}
	
//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
//			throws ServletException, IOException {
//		Map<?,?> requestParams = req.getParameterMap();
//		System.out.println("got doGET:");
//		if (requestParams != null) {
//			for (Map.Entry<?,?> entry : requestParams.entrySet()) {
//				System.out.println("entry: " + entry.getKey() + " : " + entry.getValue());
//			}
//		}
//		else {
//			System.out.println("requestParams was null");
//		}
//		super.doGet(req, resp);
//	}
//	
//	@Override
//	protected void service(HttpServletRequest request,
//			HttpServletResponse response) throws ServletException, IOException {
//		Map<?,?> requestParams = request.getParameterMap();
//		System.out.println("got doGET:");
//		if (requestParams != null) {
//			for (Map.Entry<?,?> entry : requestParams.entrySet()) {
//				System.out.println("entry: " + entry.getKey() + " : " + entry.getValue());
//			}
//		}
//		else {
//			System.out.println("requestParams was null");
//		}
//		super.service(request, response);
//	}
}
