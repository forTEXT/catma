package de.catma.servlet;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import de.catma.properties.CATMAProperties;
import de.catma.util.NonModifiableProperties;

public class PropertiesInitializerServlet extends HttpServlet {
	
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        try {
	        log("CATMA properties initializing...");
	        
			String propertiesFile = 
					System.getProperties().containsKey("prop") ? System.getProperties().getProperty(
					"prop") : "catma.properties";

			Properties properties = new Properties();
	
			properties.load(
				new FileInputStream(
					cfg.getServletContext().getRealPath(propertiesFile)));
			
			CATMAProperties.INSTANCE.setProperties( 
				new NonModifiableProperties(properties));
			
			log("CATMA properties initialized");
        }
        catch (Exception e) {
        	throw new ServletException(e);
        }
	}
}
