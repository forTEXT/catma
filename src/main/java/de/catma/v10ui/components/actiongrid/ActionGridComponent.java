package de.catma.v10ui.components.actiongrid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import de.catma.v10ui.util.Styles;

/**
 * Component that renders an action bar with a grid
 * @param <G> Gridtype e.g. TreeGrid or normal Grid
 */
public class ActionGridComponent<G extends Grid<?>> extends Composite<Div> implements HasComponents, FlexComponent<Div>, HasStyle {

    private final Component titleCompennt;
    private final G dataGrid;
    private final ActionGridBar actionGridBar;

    public ActionGridComponent(Component titleComponent, G dataGrid){
        this.titleCompennt = titleComponent;
        this.dataGrid = dataGrid;
        this.actionGridBar = new ActionGridBar(titleCompennt); // TODO: 15.10.18 refactor to guice
    }

    @Override
    protected Div initContent() {
        Div content = new Div();
        content.addClassName(Styles.actiongrid);
        content.add(actionGridBar, dataGrid);

        return content;
    }

    public ActionGridBar getActionGridBar() {
        return actionGridBar;
    }

    public G getDataGrid() {
        return dataGrid;
    }

}
