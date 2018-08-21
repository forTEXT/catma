package de.catma.v10ui.projects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


@Tag("tilecomponent")
public class TileComponent extends Component {

    VerticalLayout verticalLayout;
    HorizontalLayout projectTitleBar;

    public TileComponent(String projectDetails, String projectContent, String projectTitle) {

        getElement().getStyle().set("background-color", "#c3cee0");
        getElement().getStyle().set("margin","0");


        createContent(projectDetails, projectContent, projectTitle);


    }

    public void createContent(String projectDetails, String projectContent, String projectTitle) {
        verticalLayout = new VerticalLayout();
        verticalLayout.getStyle().set("width","100%");

        Label details = new Label(projectDetails);
        details.getElement().getStyle().set("height","15px");
        details.getElement().getStyle().set("text-align","left");
        details.getElement().getStyle().set("font-size","14px");
        details.getElement().getStyle().set("margin-left","4px");
        details.getElement().getStyle().set("height","20px");

        Label content = new Label(projectContent);

        projectTitleBar = new HorizontalLayout();

        Label title = new Label(projectTitle);
        title.getElement().getStyle().set("overflow", "hidden");
        title.getElement().getStyle().set("width", "30px");


        Icon trashcan = new Icon(VaadinIcon.TRASH);
        trashcan.setColor("black");
        trashcan.setSize("18px");
       // trashcan.getElement().getStyle().set("display","inline");
      //  trashcan.getElement().getStyle().set("float","right");

        Icon options = new Icon(VaadinIcon.OPTIONS);
        options.setColor("black");
        options.setSize("18px");
      //  options.getElement().getStyle().set("display","inline");
        options.getElement().getStyle().set("margin-right","0");
      //  options.getElement().getStyle().set("float","right");

       // Div iconsDiv = new Div(trashcan,options);



        projectTitleBar.add(title, trashcan,options);
        projectTitleBar.getElement().getStyle().set("css","");
        projectTitleBar.getElement().getStyle().set("display","inline-block");
        projectTitleBar.getElement().getStyle().set("height","20px");
        projectTitleBar.getElement().getStyle().set("width","100%");
        projectTitleBar.getElement().getStyle().set("background-color","white");



        verticalLayout.add(details, content, projectTitleBar);
        verticalLayout.getStyle().set("padding","0");

       // verticalLayout.getStyle().set("background-color", color);
        getElement().getStyle().set("padding","0");
        getElement().appendChild(verticalLayout.getElement());
       // getElement().setChild(0, verticalLayout.getElement());

    }

}
