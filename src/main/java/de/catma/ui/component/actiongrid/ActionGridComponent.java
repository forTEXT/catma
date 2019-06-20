package de.catma.ui.component.actiongrid;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.Registration;
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
	
	private static class DefaultSearchFilter implements SerializablePredicate<Object> {
		
		private String filterValue;
		
		public DefaultSearchFilter(String filterValue) {
			super();
			this.filterValue = filterValue;
		}

		@Override
		public boolean test(Object arg) {
			if (arg != null) {
				return arg.toString().startsWith(filterValue);
			}
			return false;
		}
		
	}
	
	public static class DefaultSearchFilterProvider implements SearchFilterProvider<Object> {
		@Override
		public SerializablePredicate<Object> createSearchFilter(String searchInput) {
			return new DefaultSearchFilter(searchInput);
		}
	}
	
    private final Component titleCompennt;
    private final G dataGrid;
    private final ActionGridBar actionGridBar;
    private boolean multiselect = false;
	private boolean headerVisible;
	private SearchFilterProvider<?> searchFilterProvider = new DefaultSearchFilterProvider();
	private Registration btnToggleListSelectReg;

    public ActionGridComponent(Component titleComponent, G dataGrid){
        this.titleCompennt = titleComponent;
        this.dataGrid = dataGrid;
        this.actionGridBar = new ActionGridBar(titleCompennt);
        this.btnToggleListSelectReg = this.actionGridBar.addBtnToggleListSelect(
                event -> handleToggleMultiselectRequest(event));
        this.headerVisible = dataGrid.isHeaderVisible();
        initComponents();
        initActions();
    }

    private void initActions() {
    	actionGridBar.addSearchValueChangeListener(valueChange -> handleSearchValueChange(valueChange));
	}

	@SuppressWarnings("unchecked")
	private void handleSearchValueChange(ValueChangeEvent<String> valueChange) {
		
		DataProvider<?, ?> dataProvider = this.dataGrid.getDataProvider();
		
		if (dataProvider instanceof ConfigurableFilterDataProvider) {
			@SuppressWarnings("rawtypes")
			ConfigurableFilterDataProvider filterableDp = (ConfigurableFilterDataProvider)dataProvider;
			filterableDp.setFilter(
				searchFilterProvider.createSearchFilter(valueChange.getValue()));
		}
	}

	private void handleToggleMultiselectRequest(ClickEvent event) {
        if(! multiselect) {
            dataGrid.setSelectionMode(Grid.SelectionMode.MULTI);
            multiselect = true;
            if (!headerVisible) {
            	dataGrid.setHeaderVisible(true);
            }
        } else {
            dataGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
            multiselect = false;
            
            if (!headerVisible) {
            	dataGrid.setHeaderVisible(false);
            }
        }
    }
	
	public void setSelectionModeFixed(Grid.SelectionMode selectionMode) {
		this.multiselect = selectionMode.equals(Grid.SelectionMode.MULTI);
		actionGridBar.getBtnToggleMultiselect().setEnabled(false);
		dataGrid.setSelectionMode(selectionMode);
	}
    
	private void initComponents() {
        addStyleName(Styles.actiongrid);
        setHeight("100%");
        addComponents(actionGridBar, dataGrid);
    }

    public ActionGridBar getActionGridBar() {
        return actionGridBar;
    }

    public G getDataGrid() {
        return dataGrid;
    }

    public void setSearchFilterProvider(SearchFilterProvider<?> searchFilterProvider) {
		this.searchFilterProvider = searchFilterProvider;
		actionGridBar.setSearchInputVisible(searchFilterProvider != null);
	}
}
