package de.catma.ui;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.ui.TreeTable;

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
