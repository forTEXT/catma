package de.catma.ui.component;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ExpandEvent.ExpandListener;
import com.vaadin.ui.TreeGrid;


public class TreeGridFactory {
	
	public static class TreeGridChildRowClientCacheBugfix<T> implements ExpandListener<T> {
		@Override
		public void itemExpand(ExpandEvent<T> event) {
			if (event.getSource() instanceof TreeGrid) {
				DataProvider<?, ?> dataProvider = ((TreeGrid<?>)event.getSource()).getDataProvider();
				if (dataProvider != null) {
					dataProvider.refreshAll();
				}
			}
			
		}
	}
	
	public static <T> TreeGrid<T> createDefaultTreeGrid() {
		TreeGrid<T> treeGrid = new TreeGrid<>();
//		treeGrid.addStyleName("no-focused-before-border");
		treeGrid.addExpandListener(new TreeGridChildRowClientCacheBugfix<T>());
		return treeGrid;
	}
	
	public static <T> TreeGrid<T> createDefaultGrid(HierarchicalDataProvider<T, ?> dataProvider) {
		TreeGrid<T> treeGrid = createDefaultTreeGrid();
		treeGrid.setDataProvider(dataProvider);
		
		return treeGrid;
	}
}
