package de.catma.ui.component.actiongrid;

import com.vaadin.ui.Button.ClickEvent;
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
    private boolean multiselect = false;
	private boolean headerVisible;

    public ActionGridComponent(Component titleComponent, G dataGrid){
        this.titleCompennt = titleComponent;
        this.dataGrid = dataGrid;
        this.actionGridBar = new ActionGridBar(titleCompennt);
        this.actionGridBar.addBtnToggleListSelect(
                event -> toggleMultiselect(event));
        this.headerVisible = dataGrid.isHeaderVisible();
        initComponents();
    }

    private void toggleMultiselect(ClickEvent event) {
        if(! multiselect) {
            dataGrid.setSelectionMode(Grid.SelectionMode.MULTI);
            multiselect = true;
            if (!headerVisible) {
            	dataGrid.setHeaderVisible(true);
            }
        }else {
            dataGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
            multiselect = false;
            
            if (!headerVisible) {
            	dataGrid.setHeaderVisible(false);
            }
        }
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
