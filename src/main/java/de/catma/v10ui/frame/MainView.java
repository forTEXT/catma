package de.catma.v10ui.frame;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.*;
import de.catma.document.repository.RepositoryProperties;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.project.ProjectManager;
import de.catma.repository.LoginToken;
import de.catma.repository.db.maintenance.UserManager;
import de.catma.repository.git.GitProjectManager;
import de.catma.user.User;
import de.catma.util.NonModifiableProperties;
import de.catma.v10ui.authentication.AuthenticationHandler;
import de.catma.v10ui.project.ProjectManagerView;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


@HtmlImport("styles/shared-styles.html")
@Route("")
@PageTitle("CATMA 6-flow")
public class MainView extends Div implements RouterLayout, PageConfigurator,LoginToken {

    private Object user;
    private ProjectManagerView projectManagerView;
    private Button btloginLogout;
    private UserManager userManager = new UserManager();


    public MainView(){
        ExampleTemplate template = new ExampleTemplate();
        Button button = new Button("Login",
                event -> template.setValue("Clicked!"));
        button.addClassName("main-layout__nav");
        btloginLogout = new Button(("LoginLogout"),event ->  handleLoginLogoutEvent());


        H2 title = new H2("CATMA 6");
        title.addClassName("main-layout__title");



        Div header = new Div(title, button,btloginLogout, template);
        header.addClassName("main-layout__header");

       // getProjects();

        HorizontalLayout mainContent = new HorizontalLayout();
        VerticalLayout leftPanel =new VerticalLayout();
        Div body = new Div();

        add(header);




        setClassName("main-layout");

    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addMetaTag("apple-mobile-web-app-capable", "yes");
        settings.addMetaTag("apple-mobile-web-app-status-bar-style", "black");
    }

/*    public void getProjects(){
        try {
            initTempDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ProjectManager projectManager =
                new GitProjectManager(
                        RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
                        userIdentification);
        projectManagerView = new ProjectManagerView(projectManager);

    }*/

    private void initTempDirectory() throws IOException {
        String tempDirProp = RepositoryPropertyKey.TempDir.getValue();
        File tempDir = new File(tempDirProp);
        if ((!tempDir.exists() && !tempDir.mkdirs())) {
            throw new IOException("could not create temporary directory: " ); //$NON-NLS-1$
        }
    }


    private void handleLoginLogoutEvent() {

        String scheme = VaadinServletService.getCurrentServletRequest().getScheme();
        String serverName = VaadinServletService.getCurrentServletRequest().getServerName();
        Integer port = VaadinServletService.getCurrentServletRequest().getServerPort();
        String contextPath = VaadinService.getCurrentRequest().getContextPath();
        ServletConfig cfg = VaadinServlet.getCurrent().getServletConfig();
        try {
            initProperties();
        } catch (ServletException e) {
            e.printStackTrace();
        }

        final String afterLogoutRedirectURL =
                String.format("%s://%s%s%s", scheme, serverName, port == 80 ? "" : ":" + port, contextPath); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$


        if (this.user == null) {

            AuthenticationHandler authenticationHandler =
                    new AuthenticationHandler();

            authenticationHandler.authenticate(userIdentification -> {
                try {

                    this.user = userIdentification;
                    userManager.login(this);

                    // backgroundService = new UIBackgroundService(true);

                   initTempDirectory();

                    //  btloginLogout.setHtmlContentAllowed(true);

                    ProjectManager projectManager = new GitProjectManager(
                            RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
                            userIdentification
                    );

                   // User user = projectManager.getUser();

                    //TODO:
//					if (user.isGuest()) {
//						identifier = Messages.getString("CatmaApplication.Guest"); //$NON-NLS-1$
//					}

                    /*btloginLogout.setCaption(
                            MessageFormat.format(
                                    Messages.getString("CatmaApplication.signOut"), user.getName()));*/ //$NON-NLS-1$

                    projectManagerView = new ProjectManagerView(projectManager);

                    // contentPanel.setContent(projectManagerView);
                    add(projectManagerView);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            // btloginLogout.setCaption(Messages.getString("CatmaApplication.signIn")); //$NON-NLS-1$

            //  logger.info("closing session and redirecting to " + afterLogoutRedirectURL);
            // Page.getCurrent().setLocation(afterLogoutRedirectURL);
            VaadinSession.getCurrent().close();
        }



    }


    public void initProperties() throws ServletException {


        try {
            System.out.println("CATMA Properties initializing...");

            String propertiesFile ="catma_cb.properties";

            Properties properties = new Properties();

         InputStream inputStream= MainView.class.getClassLoader().getResourceAsStream(propertiesFile);
         properties.load(inputStream);

            RepositoryProperties.INSTANCE.setProperties(
                    new NonModifiableProperties(properties));

            System.out.println("CATMA Properties initialized.");
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void close() {
        VaadinSession.getCurrent().setAttribute("USER", null);
/*        if (projectManagerView != null) {
            projectManagerView.close();
        }
        logger.info("application for user" + getUser() + " has been closed"); //$NON-NLS-1$ //$NON-NLS-2$
        if (repositoryOpened) {
            userManager.logout(this);
            repositoryOpened = false;
        }
        backgroundService.shutdown();
        super.close();*/
    }

    @Override
    public Object getUser() {
        return user;
    }
}
