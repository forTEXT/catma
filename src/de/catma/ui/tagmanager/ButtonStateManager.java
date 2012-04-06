package de.catma.ui.tagmanager;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.TreeTable;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagsetDefinition;

public class ButtonStateManager implements ValueChangeListener {
	
	private Button btRemoveTagset;
	private Button btEditTagset;
	private Button btInsertTag;
	private Button btRemoveTag;
	private Button btEditTag;
	private Button btInsertProperty;
	private Button btRemoveProperty;
	private Button btEditProperty;
	private boolean withTagsetButtons;
	
	public ButtonStateManager(boolean withTagsetButtons, 
			Button btRemoveTagset, Button btEditTagset,
			Button btInsertTag, Button btRemoveTag, Button btEditTag,
			Button btInsertProperty, Button btRemoveProperty,
			Button btEditProperty) {
		this.withTagsetButtons = withTagsetButtons;
		this.btRemoveTagset = btRemoveTagset;
		this.btEditTagset = btEditTagset;
		this.btInsertTag = btInsertTag;
		this.btRemoveTag = btRemoveTag;
		this.btEditTag = btEditTag;
		this.btInsertProperty = btInsertProperty;
		this.btRemoveProperty = btRemoveProperty;
		this.btEditProperty = btEditProperty;
		tagsetDefSelected(false);
	}



	public void valueChange(ValueChangeEvent event) {
		TreeTable treeTable = (TreeTable)event.getProperty();
		
		Object value = treeTable.getValue();
		
		if (value == null) {
			tagsetDefSelected(false); // all disabled
		}
		else if (value instanceof TagsetDefinition) {
			tagsetDefSelected(true);
		}
		else if (value instanceof TagDefinition) {
			tagDefSelected(true);
		}
		else if (value instanceof PropertyDefinition) {
			propDefSelected(true);
		}
		else {
			tagsetDefSelected(false); // all disabled
		}
		
	}
	
	private void tagsetDefSelected(boolean selected) {
		if (withTagsetButtons) {
			btEditTagset.setEnabled(selected);
			btRemoveTagset.setEnabled(selected);
			btInsertTag.setEnabled(selected);
		}
		btRemoveTag.setEnabled(false);
		btEditTag.setEnabled(false);
		btInsertProperty.setEnabled(false);
		btRemoveProperty.setEnabled(false);
		btEditProperty.setEnabled(false);
	}
	
	private void tagDefSelected(boolean selected) {
		if (withTagsetButtons) {
			btEditTagset.setEnabled(false);
			btRemoveTagset.setEnabled(false);
			btInsertTag.setEnabled(selected);
		}
		btRemoveTag.setEnabled(selected);
		btEditTag.setEnabled(selected);
		btInsertProperty.setEnabled(selected);
		btRemoveProperty.setEnabled(false);
		btEditProperty.setEnabled(false);
	}
	
	private void propDefSelected(boolean selected) {
		if (withTagsetButtons) {
			btEditTagset.setEnabled(false);
			btRemoveTagset.setEnabled(false);
			btInsertTag.setEnabled(false);
		}
		btRemoveTag.setEnabled(false);
		btEditTag.setEnabled(false);
		btInsertProperty.setEnabled(false);
		btRemoveProperty.setEnabled(selected);
		btEditProperty.setEnabled(selected);
	}	

}
