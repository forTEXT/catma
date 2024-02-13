package de.catma.ui.component.actiongrid;

import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.VerticalLayout;

/**
 * Component that renders an action bar with a grid
 * @param <G> Gridtype e.g. TreeGrid or normal Grid
 */
public class ActionGridComponent<G extends Grid<?>> extends VerticalLayout  {
	
	private static class DefaultSearchFilter implements SerializablePredicate<Object> {
		
		private String filterValue;
		
		public DefaultSearchFilter(String filterValue) {
			super();
			this.filterValue = filterValue.toLowerCase();
		}

		@Override
		public boolean test(Object arg) {
			if (arg != null) {
				return arg.toString().toLowerCase().contains(filterValue);
			}
			return false;
		}
		
	}
	
	private static class HierarchicalSearchFilter implements SerializablePredicate<Object> {
		
		private String filterValue;
		private HierarchicalDataProvider<Object, Object> dataProvider;
		

		public HierarchicalSearchFilter(
				String filterValue, 
				HierarchicalDataProvider<Object, Object> dataProvider) {
			super();
			this.filterValue = filterValue.toLowerCase();
			this.dataProvider = dataProvider;
		}

		@Override
		public boolean test(Object arg) {
			if (arg != null) {
				if (arg.toString().toLowerCase().contains(filterValue)) {
					return true;
				}
				Set<Object> children = 
					dataProvider.fetchChildren(new HierarchicalQuery<Object, Object>(null, arg)).collect(Collectors.toSet());
				
				for (Object child : children) {
					if (test(child)) {
						return true;
					}
				}
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
	
	private class HierarchicalSearchFilterProvider implements SearchFilterProvider<Object> {
		
		@Override
		public SerializablePredicate<Object> createSearchFilter(String searchInput) {
			return new HierarchicalSearchFilter(
					searchInput, 
					(HierarchicalDataProvider<Object, Object>)dataGrid.getDataProvider());
		}
	}	
	
    private final Component titleCompennt;
    private final G dataGrid;
    private final ActionGridBar actionGridBar;
    private boolean multiselect = false;
	private boolean headerVisible;
	private SearchFilterProvider<?> searchFilterProvider;
	private Registration btnToggleListSelectReg;

    public ActionGridComponent(Component titleComponent, G dataGrid){
        this.titleCompennt = titleComponent;
        this.dataGrid = dataGrid;
        this.actionGridBar = new ActionGridBar(titleCompennt);
        this.btnToggleListSelectReg = this.actionGridBar.addBtnToggleListSelect(
                event -> handleToggleMultiselectRequest());
        this.headerVisible = dataGrid.isHeaderVisible();
        
        if (dataGrid.getDataProvider() instanceof HierarchicalDataProvider) {
        	searchFilterProvider = new HierarchicalSearchFilterProvider();
        }
        else {
        	searchFilterProvider = new DefaultSearchFilterProvider();
        }
        
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

	private void handleToggleMultiselectRequest() {
        if(! multiselect) {
           setMultiselect(true);
        } else {
        	setMultiselect(false);
        }
    }
	
	private void setMultiselect(boolean enabled) {
        if(enabled) {
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
	
	public void setSelectionMode(Grid.SelectionMode selectionMode) {
		boolean enabled = selectionMode.equals(Grid.SelectionMode.MULTI);
		setMultiselect(enabled);
	}
	
	public void setSelectionModeFixed(Grid.SelectionMode selectionMode) {
		this.multiselect = selectionMode.equals(Grid.SelectionMode.MULTI);
		actionGridBar.getBtnToggleMultiselect().setVisible(false);
		dataGrid.setSelectionMode(selectionMode);
	}
    
	private void initComponents() {
        setHeight("100%");
        addComponents(actionGridBar, dataGrid);
        setExpandRatio(dataGrid, 1f);
        setSpacing(false);
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
