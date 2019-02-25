package de.catma.servlet;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import de.catma.document.repository.RepositoryProperties;
import de.catma.util.NonModifiableProperties;

@WebServlet(loadOnStartup = 1, name="PropertiesInitializer", urlPatterns="/PropertiesInitializer")
public class PropertiesInitializerServlet extends HttpServlet {
	
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        try {
	        log("CATMA Properties initializing...");
	        
			String propertiesFile = 
					System.getProperties().containsKey("prop") ? System.getProperties().getProperty(
					"prop") : "catma.properties";

			Properties properties = new Properties();
	
			properties.load(
				new FileInputStream(
					cfg.getServletContext().getRealPath(propertiesFile)));
			
			RepositoryProperties.INSTANCE.setProperties( 
				new NonModifiableProperties(properties));
			
			log("CATMA Properties initialized.");
        }
        catch (Exception e) {
        	throw new ServletException(e);
        }
	}
}
