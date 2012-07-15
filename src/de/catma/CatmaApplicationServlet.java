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
