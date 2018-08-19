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
public class TileComponent extends Component {

    VerticalLayout verticalLayout;
    HorizontalLayout projectTitleBar;

    public TileComponent(String value, String color, String projectTitle) {

        getElement().getStyle().set("background-color", "grey");
        getElement().getStyle().set("width","22%");

        createContent(value, color, projectTitle);


    }

    public void createContent(String value, String color, String projectTitle) {
        verticalLayout = new VerticalLayout();
        verticalLayout.getStyle().set("width","100%");
        Label label = new Label(value);
        label.getElement().getStyle().set("height","20px");
        label.getElement().getStyle().set("font-size","10px");
        label.getElement().getStyle().set("margin-left","0px");
        label.getElement().getStyle().set("height","20px");

        Label label1 = new Label(color);

        projectTitleBar = new HorizontalLayout();

        Label title = new Label(projectTitle);
        title.getElement().getStyle().set("font-size","10px");
        Icon trashcan = new Icon(VaadinIcon.TRASH);
        trashcan.setColor("black");
        trashcan.setSize("18px");

        Icon options = new Icon(VaadinIcon.OPTIONS);
        options.setColor("black");
        options.setSize("18px");
        projectTitleBar.add(title, trashcan, options);
        projectTitleBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        projectTitleBar.getElement().getStyle().set("height","20px");
        projectTitleBar.getElement().getStyle().set("width","100%");
        projectTitleBar.getElement().getStyle().set("background-color","#c3cee0");
        verticalLayout.add(label, label1, projectTitleBar);

        verticalLayout.getStyle().set("background-color", color);
        getElement().setChild(0, verticalLayout.getElement());

    }

}
