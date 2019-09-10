package de.catma.ui.legacy.analyzer.querybuilder;

import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.TreeTable;

public class EndorsedTreeTable extends TreeTable {

	public EndorsedTreeTable() {
	}

	public EndorsedTreeTable(String caption, Container dataSource) {
		super(caption, dataSource);
	}

	public EndorsedTreeTable(String caption) {
		super(caption);
	}
	
	@Override
	protected Object getPropertyValue(
			Object rowId, Object colId, Property property) {
		
		try {
			return super.getPropertyValue(rowId, colId, property);
		}
		catch (Exception e) {
			e.printStackTrace(); // Vaadin table swallows the relevant information...
			throw new RuntimeException(e);
		}
	}	

}
