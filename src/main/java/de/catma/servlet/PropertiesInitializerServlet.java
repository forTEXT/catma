package de.catma.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import de.catma.document.repository.RepositoryProperties;
import de.catma.util.NonModifiableProperties;

@WebServlet(name = "CATMAProperties", urlPatterns = "/CATMAProperties", loadOnStartup = 1)
public class PropertiesInitializerServlet extends HttpServlet {
	
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        try {
	        log("CATMA Properties initializing...");
	        
			String propertiesFile = 
					System.getProperties().containsKey("prop") ? System.getProperties().getProperty(
					"prop") : "catma.properties";

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream input = classLoader.getResourceAsStream(propertiesFile);
            Properties properties = new Properties();
			properties.load(input);

			RepositoryProperties.INSTANCE.setProperties( 
				new NonModifiableProperties(properties));
			
			log("CATMA Properties initialized.");
        }
        catch (Exception e) {
        	throw new ServletException(e);
        }
	}
}
