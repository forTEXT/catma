package de.catma.v10ui.modules.main;

import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.guice.server.GuiceVaadinServlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

/**
 * Servlet as main entrypoint is required for DI with guice
 * @author db
 */
@WebServlet(name = "Guice-Vaadin-Servlet", urlPatterns = "/*")
@PackagesToScan({"de.catma.v10ui"})
public class CatmaV10Servlet  extends GuiceVaadinServlet {

}