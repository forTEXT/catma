package de.catma.servlet;

import java.io.FileInputStream;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import de.catma.document.repository.RepositoryPropertiesName;
import de.catma.util.NonModifiableProperties;

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
	
			new InitialContext().bind(
				RepositoryPropertiesName.CATMAPROPERTIES.name(), 
				new NonModifiableProperties(properties));
			
			log("CATMA Properties initialized.");
        }
        catch (Exception e) {
        	throw new ServletException(e);
        }
	}
}
