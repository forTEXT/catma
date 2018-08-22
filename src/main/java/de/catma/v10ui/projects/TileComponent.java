package de.catma.v10ui.projects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


@Tag("tilecomponent")
@HtmlImport("styles/tile-style.html")
public class TileComponent extends Div {

    VerticalLayout verticalLayout;
    HorizontalLayout projectTitleBar;

    public TileComponent(String projectDetails, String projectContent, String projectTitle) {

/*        getElement().getStyle().set("background-color", "#c3cee0");
        getElement().getStyle().set("margin","0");*/
        setClassName("tile");


        createContent(projectDetails, projectContent, projectTitle);


    }

    public void createContent(String projectDetails, String projectContent, String projectTitle) {
        verticalLayout = new VerticalLayout();


        Label details = new Label(projectDetails);

        details.setClassName("projectDetails");

        Label content = new Label(projectContent);

        projectTitleBar = new HorizontalLayout();

        Label title = new Label(projectTitle);
      //  title.getElement().getStyle().set("overflow", "hidden");
       // title.getElement().getStyle().set("width", "30px");
        title.setClassName("titleBar_name");


        Icon trashcan = new Icon(VaadinIcon.TRASH);
       // trashcan.setColor("black");
       // trashcan.setSize("18px");
       // trashcan.getElement().getStyle().set("display","inline");
      //  trashcan.getElement().getStyle().set("float","right");
        trashcan.setClassName("titleBar_trashIcon");

        Icon options = new Icon(VaadinIcon.OPTIONS);
    // options.setColor("black");
      //  options.setSize("10px");
      //  options.getElement().getStyle().set("display","inline");
      //  options.getElement().getStyle().set("margin-right","0");
      //  options.getElement().getStyle().set("float","right");

       // Div iconsDiv = new Div(trashcan,options);
        options.addClassName("titleBar_optionsIcon");



        projectTitleBar.add(title, trashcan,options);

        /*projectTitleBar.getElement().getStyle().set("display","inline-block");
        projectTitleBar.getElement().getStyle().set("height","20px");
        projectTitleBar.getElement().getStyle().set("width","100%");
        projectTitleBar.getElement().getStyle().set("background-color","white");*/

        projectTitleBar.setClassName("titleBar");



        verticalLayout.add(details, content, projectTitleBar);
        verticalLayout.setClassName("verticalLayout");
    /*    verticalLayout.getStyle().set("padding","0");*/

       // verticalLayout.getStyle().set("background-color", color);
        getElement().getStyle().set("padding","0");
        getElement().appendChild(verticalLayout.getElement());
       // getElement().setChild(0, verticalLayout.getElement());

    }

}
