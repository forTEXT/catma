package de.catma.v10ui.projects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;



@Tag("tilecomponent")
public class TileComponent extends Component  {

    VerticalLayout verticalLayout;
    HorizontalLayout projectTitleBar ;

    public TileComponent(String value, String color, String projectTitle){


        getElement().getStyle().set("background-color","yellow");


       createContent(value,color,projectTitle);

    }
public void createContent(String value, String color, String projectTitle){
        verticalLayout = new VerticalLayout();
        Label label = new Label(value);
        Label label1 = new Label(color);

          projectTitleBar = new HorizontalLayout();

          Label title = new Label(projectTitle);
          Icon trashcan = new Icon(VaadinIcon.TRASH);

          Icon options = new Icon(VaadinIcon.OPTIONS);
          projectTitleBar.add(title,trashcan,options);
          projectTitleBar.setAlignItems(FlexComponent.Alignment.END);
          verticalLayout.add(label,label1,projectTitleBar);
          verticalLayout.getStyle().set("background-color",color);
          getElement().setChild(0,verticalLayout.getElement());

}

}
