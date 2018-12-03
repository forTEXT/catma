package de.catma.ui.component.actiongrid;

import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;

import de.catma.ui.layout.FlexLayout;
import de.catma.ui.util.Styles;

/**
 * Component that renders an action bar with a grid
 * @param <G> Gridtype e.g. TreeGrid or normal Grid
 */
public class ActionGridComponent<G extends Grid<?>> extends FlexLayout  {

    private final Component titleCompennt;
    private final G dataGrid;
    private final ActionGridBar actionGridBar;
    private  boolean multiselect = false;

    public ActionGridComponent(Component titleComponent, G dataGrid){
        this.titleCompennt = titleComponent;
        this.dataGrid = dataGrid;
        this.actionGridBar = new ActionGridBar(titleCompennt);
        this.actionGridBar.addBtnToggleListSelect(
                (event) -> {
                    if(! multiselect) {
                        dataGrid.setSelectionMode(Grid.SelectionMode.MULTI);
                        multiselect = true;
                    }else {
                        dataGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
                        multiselect = false;
                    }
                }
        );
        initComponents();
    }

    private void initComponents() {
        addStyleName(Styles.actiongrid);
        addComponents(actionGridBar, dataGrid);
    }

    public ActionGridBar getActionGridBar() {
        return actionGridBar;
    }

    public G getDataGrid() {
        return dataGrid;
    }

}
