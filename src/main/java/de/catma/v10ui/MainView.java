package de.catma.v10ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;


@HtmlImport("styles/shared-styles.html")
@Route("")
public class MainView extends VerticalLayout  {

    public MainView(){
        ExampleTemplate template = new ExampleTemplate();

        Button button = new Button("Click me",
                event -> template.setValue("Clicked!"));

        add(button, template);
        setClassName("main-layout");

    }


}
