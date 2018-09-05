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
        setClassName("tile");
        initComponents(projectDetails, projectContent, projectTitle);
    }

    public void initComponents(String projectDetails, String projectContent, String projectTitle) {

        verticalLayout = new VerticalLayout();
        verticalLayout.setClassName("tile_verticalLayout");

        Label details = new Label(projectDetails);
        details.setClassName("projectDetails");

        Label content = new Label(projectContent);

        projectTitleBar = new HorizontalLayout();
        projectTitleBar.setClassName("titleBar");

        Label projectName = new Label(projectTitle);
        projectName.setClassName("titleBar_name");

        Icon trashcan = new Icon(VaadinIcon.TRASH);
        trashcan.setClassName("titleBar_trashIcon");

        Icon options = new Icon(VaadinIcon.OPTIONS);
        options.addClassName("titleBar_optionsIcon");

        projectTitleBar.add(projectName, trashcan,options);
        verticalLayout.add(details, content, projectTitleBar);

        getElement().appendChild(verticalLayout.getElement());

    }

}
