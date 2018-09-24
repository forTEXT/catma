package de.catma.v10ui.modules.dashboard;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.HasItems;
import de.catma.project.ProjectReference;

import java.util.Collection;
import java.util.Objects;

public class ProjectCard extends Composite<VerticalLayout> implements HasItems<ProjectReference> {

    private ProjectReference item;

    public ProjectCard(ProjectReference t){
        setItems(t);
    }

    @Override
    public void setItems(Collection<ProjectReference> items) {
        Objects.requireNonNull(items);
        item = Objects.requireNonNull(items.iterator().next());
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("projectlist__card");
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidth("352px");
        VerticalLayout preview = new VerticalLayout();
        preview.addClassName("projectlist__card__preview");
        preview.add(new Span(item.getDescription()));
        preview.add(new Span(item.getProjectId()));
        layout.add(preview);

        HorizontalLayout descriptionBar = new HorizontalLayout();
        descriptionBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        descriptionBar.setWidth("100%");

        Span name = new Span(item.getName());
        name.setWidth("100%");
        descriptionBar.add(name);
        descriptionBar.expand(name);

        Button buttonRemove = new Button(null, VaadinIcon.TRASH.create());
        buttonRemove.getElement().setAttribute("theme","icon");
        descriptionBar.add(buttonRemove);

        Button buttonAction = new Button(null, VaadinIcon.ELLIPSIS_DOTS_V.create());
        buttonAction.getElement().setAttribute("theme","icon");
        descriptionBar.add(buttonAction);

        layout.add(descriptionBar);
        return layout;
    }
}
