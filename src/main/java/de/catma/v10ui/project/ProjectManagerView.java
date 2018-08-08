package de.catma.v10ui.project;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.H2;
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;

@Tag("projectMangerView")
public class ProjectManagerView extends Component {
    ProjectManager projectManager;

    public ProjectManagerView(ProjectManager projectManager) {
        this.projectManager = projectManager;
        H2 pmvtitle = new H2("ProjectManager View");
        pmvtitle.addClassName("main-layout__title");

        System.out.println("componenet ------!!!!!!!!!----------");
        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();
          System.out.println( "Erstes Project = "+projectPager.first().toString());
        }catch(Exception e){
            e.printStackTrace();
        }





    }

  private void  initProjectManagerViewComponents(ProjectManager projectManager){

    }
}
