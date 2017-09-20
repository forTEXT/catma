# Getting Started

## Eclipse

### Prerequisites:
- Maria DB 10.1.26 or later (setup script: `db/createCATMA6DB.sql`)
- Eclipse Neon 3, Release 4.6.3
- Latest Vaadin Plugin - [Download](http://vaadin.com/eclipse)
- Latest IvyDE Plugin - [Download](http://ant.apache.org/ivy/ivyde/updatesite)
- Jetty 9.3.20v20170531 - [Download](http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/9.4.6.v20170531/jetty-distribution-9.4.6.v20170531.zip)
- Latest Jetty Plugin - [Download](http://eclipse-jetty.github.io/update/) (the custom Jetty version needs to be selected in the plugin's settings)

### Instructions:

1. Copy `catma.properties` to custom `catma_custom.properties` and adjust `RepositoryFolderPath1`, `RepositoryUrl1`, `TempDir`, `IndexerUrl1` and `GraphDbPath1` and add `BaseURL=http://localhost:8080/catma/` to the bottom of the file.
2. Create new Run Configuration for "Jetty Webapp" with `WebApp=WebContent, ContextPath=/catma` and arguments: `-server -Dprop=catma_custom.properties`
3. Set Options: Enable JSP support, Enable JNDI support, Enable Websocket support, Disable server cache, Disable client cache

## IntelliJ IDEA

See [Migrating From Eclipse to IntelliJ IDEA](https://www.jetbrains.com/help/idea/eclipse.html)

### Prerequisites:
- Maria DB 10.1.26 or later - [Download](https://downloads.mariadb.org/) (setup script: `db/createCATMA6DB.sql`)
- IntelliJ IDEA Ultimate - [Download](https://www.jetbrains.com/idea/download) (the Ultimate Edition is needed for GWT/Vaadin and Jetty support)
- Jetty 9.3.20v20170531 - [Download](http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/9.4.6.v20170531/jetty-distribution-9.4.6.v20170531.zip) - [Docs](http://www.eclipse.org/jetty/documentation/current/)
- GWT SDK 2.7.0 - [Download](http://goo.gl/t7FQSn) (make sure that the version matches that of your Vaadin version, you can check it here: `https://github.com/vaadin/framework/blob/<vaadin-version>/pom.xml` (look for a line like `<vaadin.gwt.version>2.7.0.vaadin5</vaadin.gwt.version>` under the `<properties>` node)

### Instructions:

1. Clone the CATMA Git repositories:
   1. Clone the "catma" repo, branch: "catma6" (https://github.com/mpetris/catma.git)
   2. Clone the "catma-core" repo, branch: "catma6" (https://github.com/mpetris/catma-core.git)
2. Import the "catma" project:
   1. Open IntelliJ IDEA and choose "Import Project" from the splash screen (alternatively go to File > New > Project from Existing Sources...)
   2. Choose the path that was used to clone the "catma" repo in step 1.1
   3. In the "Import Project" dialog window that opens, choose "Import project from external model" and then choose "Eclipse"
   4. On the next page, choose "Create module files near .classpath files", then ".idea (directory based)" for the project format and lastly check the box labelled "Link created IntelliJ IDEA modules to Eclipse project files"
      (this option automatically keeps the Eclipse projects and IntelliJ IDEA modules synchronized)
   5. On the next page, select the "catma" project and click Next
   6. On the next page, add and/or select the JDK (it should be 1.8) and give it the name "JavaSE-1.8", then click Finish
3. Once the project has been imported and opened, install the necessary plugins:
   1. Go to File > Settings > Plugins, click "Browse repositories..." and search for "ivy". Select the "IvyIDEA" plugin and install it. [Plugin](https://plugins.jetbrains.com/plugin/3612-ivyidea) - [Docs](http://confluence.jetbrains.com/display/CONTEST/IvyIDEA)
   2. Follow the instructions to install the Vaadin plugins here: https://vaadin.com/vaadin-documentation-portlet/designer/designer-installing-idea.html
   3. Restart IntelliJ IDEA
4. Configure the compiler:
   1. Go to File > Settings > Build, Execution, Deployment > Compiler > Java Compiler, select the "Javac" compiler and set the project bytecode version to "1.8".
      Under the "Per-module bytecode version" heading, click the green plus and choose the "catma" module, then click Apply.
5. Configure plugins:
   1. Go to File > Settings > IvyIDEA and check the box labelled "Resolve dependencies in background", then at the bottom, set the "Ivy Log Level" to "Info" and click Apply
6. Configure project - go to File > Project Structure, then:
   1. Under "Project", ensure that "Project SDK" is set to "JavaSE-1.8" and that "Project language level" is set to "8 - Lambdas, type annotations etc."
   2. Under "Modules", above the left-hand tree view where the "catma" module is listed, click the green plus and choose "Import Module"
   3. Choose the path that was used to clone the "catma-core" repo in step 1.2 and proceed as you did for the "catma" repo, steps 2.3 to 2.5
   4. For both the "catma" and "catma-core" modules, in the right-hand pane, remove the dependencies that are highlighted in red and choose "IntelliJ IDEA (.iml)" for "Dependencies storage format" at the bottom.
	  _The dependencies we are removing are only needed if you are using Eclipse.
	  For the "catma" module there should be 5 of these, 2 that start with "org.eclipse.jst.j2ee" and 3 that start with "org.apache.ivyde.eclipse".
	  For the "catma-core" module there should be 2, 1 that starts with "org.eclipse.jst.j2ee" and 1 that starts with "org.apache.ivyde.eclipse"._
	  Click Apply and OK to close the dialog.
   5. At this point IntelliJ IDEA should re-index the projects and you may see a notification about frameworks having been detected and asking whether you'd like to configure them.
	  If you don't see a notification you can check for historical messages under the "Event Log" tab in the bottom-right corner of the IDE (the tabs may only become visible after clicking the icon that looks like a screen in the bottom-left corner).
   6. Choose to configure the frameworks. You may get a version control popup. If you don't see a "Setup Frameworks" dialog after closing the version control popup you may need to click the "Configure" button/link again.
	  Within the "Setup Frameworks" dialog, you should see top-level items called "Web", "GWT" and "IvyIDEA". Make sure everything is selected and click OK. It will appear as though nothing happened, but that's ok.
7. Exclude old code:
   1. In the left-hand project tree view, navigate to `src/de.catma/queryengine`, right-click the `computation_old` subdirectory and choose "Mark Directory as" > "Excluded" near the bottom of the context menu. The directory should now be coloured red (and you can collapse the tree again).
8. Create project directories and extract Jetty and GWT:
   1. Create a directory named `dev` at the root of the project
   2. Extract Jetty and the GWT SDK here (see prerequisites, they should each have their own subdirectory)
   3. Also create subdirectories called `repo`, `graphdb` and `temp` (we'll use these later)
   4. If you now get a notification about configuring frameworks that contains references to Google App Engine and a number of GWT `web.xml` files you can ignore it, or open it and click Cancel
9. Configure Facets - go to File > Project Structure, then:
   1. Under "Facets", you should now see "GWT", "IvyIDEA" and "Web" facets
   2. Select the "GWT (catma)" sub-node, then:
      1. Enter the fully qualified path to GWT (`[...]/dev/gwt-2.7.0`), select "Web" as the "Target Web Facet" and tick the checkbox labelled "Show compiler output"
      2. You should see a yellow triangle at the bottom warning you about a library not being a module dependency. Click the Fix button.
      3. The warning should change and read "GWT compiler output is not included in an artifact". Ignore it for now, we'll fix this later.
   3. Select the "IvyIDEA (catma)" sub-node, then:
      1. Browse to and select the `ivy.xml` file at the root of the project
	  2. Tick the checkbox labelled "Use module specific ivy settings" at the bottom, select "Use your own" and browse to and select the `ivysettings.xml` file at the root of the project
      3. Click Apply and OK to close the dialog
	  4. Go back to File > Project Structure and select the "IvyIDEA (catma)" sub-node again. You should now see a number of configurations listed and all warnings should be gone.
   4. Repeat step 9.3 above for the "IvyIDEA (catma-core)" sub-node. Be sure to choose the xml files from the "catma-core" project directory.
   5. Select the "Web (catma)" sub-node, then:
      1. IntelliJ IDEA should have pre-selected the `/WebContent/WEB-INF/web.xml` file under "Deployment Descriptors", and the `/WebContent` directory under "Web Resource Directories"
      2. There should also be a ticked checkbox under "Source Roots" pointing to the `/src` directory
      3. You should once again see a yellow triangle with a warning reading "'Web' Facet resources are not included in an artifact"
	  4. Click the Create Artifact button
      6. You will be taken to the Artifacts section where an artifact will already have been created. This is fine, we just need to modify it slightly.
	  7. Tick the checkbox labelled "Show content of elements" and expand all nodes in both tree views
      8. Drag "'catma-core' compile output" from the right-hand tree view and drop it under `WEB-INF/classes` in the left-hand tree view
	  9. There should be a number of libraries as well as "'catma' GWT compiler output" and "'catma' Vaadin WidgetSet compile output" remaining in the right-hand tree view now
      10. Select all libraries (but _not_ the GWT or Vaadin elements) from both projects in the right-hand tree view, right-click and choose "Put into /WEB-INF/lib" from the context menu
	  11. Click Apply. All warnings should disappear.
   6. Select the "GWT (catma)" sub-node again, then:
      1. Tick the checkbox for the `de.catma.ui.CleaWidgetset` GWT Module
	  2. Click Apply
10. Resolve Ivy dependencies:
    1. In the left-hand project tree view, right-click on the root node and select "IvyIDEA" > "Resolve for all modules" near the bottom of the context menu
    2. This will take some time - please wait for the dependency resolution process to complete
    3. Confirm that Ivy was able to resolve all dependencies by clicking the IvyIDEA tab at the bottom-left of the IDE window
11. Configure Artifacts (i) - go to File > Project Structure, then:
    1. Select "Artifacts" from the menu on the left
    2. In the right-hand tree view, you should now see an "IvyIDEA" node under both of the projects
    3. Drag both "IvyIDEA" nodes into `/WEB-INF/lib` in the left-hand tree view
    4. This should cause a large number of libraries to appear in the `lib` directory in the left-hand tree view
    5. Click Apply
12. Configure exported libraries - still in the Project Structure dialog:
    1. Select "Modules" from the menu on the left
	2. Select the "catma-core" module from the left-hand tree view
	3. Tick the "Export" checkbox for both libraries listed in the right-hand pane (lingpipe & IvyIDEA)
	4. Click Apply
13. Configure unmanaged libraries - still in the Project Structure dialog, "Modules" section:
    1. Select the "catma" module from the left-hand tree view
	2. Click the green plus on the right and select "JARs or directories..."
	3. Browse to `/WebContent/WEB-INF/lib` and select both jar files (aerogear-otp.jar & ColorPicker-GWT-2.1.jar)
	4. Click OK
    5. Click Apply and OK to close the Project Structure dialog _(we do this so that the new libraries we just added appear as available elements under "Artifacts" when the dialog is re-opened)_
14. Configure Artifacts (ii) - go to File > Project Structure, then:
	1. Select "Artifacts" from the menu on the left
	2. In the right-hand tree view, you should now see the aerogear and ColorPicker libraries as available elements under "catma"
	3. Select them both, right-click, and select "Put into WEB-INF/lib"
15. Configure Artifacts (iii) - still in the Project Structure dialog, "Artifacts" section:
    1. In the right-hand tree view, right-click "'catma' Vaadin WidgetSet compile output" and select "Put into Output Root" from the context menu
    2. A "VAADIN" directory should appear in the left-hand tree view and there should be no available sub-elements left in the right-hand tree view
	3. Click Apply and OK
16. Build the project:
    1. Go to Build > Build Project
	2. Confirm that the project built successfully (check the Messages tab)
17. If you haven't already, set up Maria DB and run the setup script (see prerequisites)
18. Change settings:
    1. Create a copy of `/WebContent/catma.properties` and call it `catma_custom.properties`
    2. Adjust `RepositoryFolderPath1`, `RepositoryUrl1`, `TempDir`, `IndexerUrl1` and `GraphDbPath1` as appropriate and add `BaseURL=http://localhost:8080/catma/` to the bottom of the file.
    (`RepositoryFolderPath1`, `TempDir` and `GraphDbPath1` should be set to the `repo`, `temp` and `graphdb` directories respectively, which we created in 8.3)
19. Create a Jetty run configuration:
    1. Click the dropdown arrow in the toolbar at the top-right of the IDE and select "Edit Configurations..."
	2. Click the green plus in the top left and select Jetty Server > Local (you might have to show the "irrelevant" items at the bottom of the list)
    3. Name the configuration "Local"
	4. You should see an error at the bottom saying "Application Server not specified".
	5. Click the Configure... button next to "Application Server" at the top
	6. Browse to the Jetty Home directory (`[...]/dev/jetty-distribution-9.3.20.v20170531`)
    7. Click OK twice to close the dialog window
	8. You should now see a warning at the bottom saying "JMX module is not included". Click the Fix button.
    9. You should now see a warning at the bottom saying "No artifacts marked for deployment". Click the Fix button.
	10. The "catma:Web" artifact will be added under the "Deployment" tab
    11. Still on the "Deployment" tab, tick the checkbox labelled "Use custom context root" and type `/catma/` into the text box underneath
	12. Click Apply
    13. Back on the "Server" tab, click the Configure... button again and make sure all of the following modules are selected: `jsp`, `jndi`, `websocket` and `server`
	14. Click OK
    15. In the "VM options" text box on the "Server" tab, enter `-Dprop=catma_custom.properties`
	16. Click Apply and OK
20. Run it:
    1. Click the green play button in the toolbar at the top-right of the IDE to start the Jetty server (it should start after some time)
    2. Browse to `http://localhost:8080/catma/` in your favourite browser
    
    
