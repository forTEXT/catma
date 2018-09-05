/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.tagmanager;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.v7.ui.TreeTable;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class ButtonStateManager implements ValueChangeListener {
	
	private Button btLoadIntoDocument;
	private Button btRemoveTagset;
	private Button btEditTagset;
	private Button btInsertTag;
	private Button btRemoveTag;
	private Button btEditTag;
	private Button btInsertProperty;
	private Button btRemoveProperty;
	private Button btEditProperty;
	private boolean withTagsetButtons;
	private boolean withTagButtons;
	private boolean withPropertyButtons;
	private boolean withDocumentButtons;
	
	public ButtonStateManager(
			boolean withTagsetButtons,
			boolean withTagButtons,
			boolean withPropertyButtons,
			boolean withDocumentButtons,
			Button btbtLoadIntoDocument,
			Button btRemoveTagset, Button btEditTagset,
			Button btInsertTag, Button btRemoveTag, Button btEditTag,
			Button btInsertProperty, Button btRemoveProperty,
			Button btEditProperty) {
		this.withTagsetButtons = withTagsetButtons;
		this.withTagButtons = withTagButtons;
		this.withPropertyButtons = withPropertyButtons;
		this.withDocumentButtons = withDocumentButtons;
		this.btLoadIntoDocument = btbtLoadIntoDocument;
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
		
		Object value = treeTable.getValue();// wenn  hier multiselect enabeld ist . return ein Set, wenn nix selected -> leerer set, nicht null !
		
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
		if (withDocumentButtons){
			btLoadIntoDocument.setEnabled(selected);
		}
		
		if (withTagsetButtons) {
			btEditTagset.setEnabled(selected);
			btRemoveTagset.setEnabled(selected);
			
			if (withTagButtons) {
				btInsertTag.setEnabled(selected);				
			}			
		}
		
		if (withTagButtons) {
			btRemoveTag.setEnabled(false);
			btEditTag.setEnabled(false);
		}
		
		if (withPropertyButtons) {
			btInsertProperty.setEnabled(false);
			btRemoveProperty.setEnabled(false);
			btEditProperty.setEnabled(false);
		}
	}
	
	private void tagDefSelected(boolean selected) {
		if (withDocumentButtons){
			btLoadIntoDocument.setEnabled(false);
		}
		
		if (withTagsetButtons) {
			btEditTagset.setEnabled(false);
			btRemoveTagset.setEnabled(false);
			
			if (withTagButtons) {
				btInsertTag.setEnabled(selected);
			}			
		}
		
		if (withTagButtons) {
			btRemoveTag.setEnabled(selected);
			btEditTag.setEnabled(selected);
		}
		
		if (withPropertyButtons) {
			btInsertProperty.setEnabled(selected);
			btRemoveProperty.setEnabled(false);
			btEditProperty.setEnabled(false);
		}
	}
	
	private void propDefSelected(boolean selected) {
		if (withDocumentButtons){
			btLoadIntoDocument.setEnabled(false);
		}
		
		if (withTagsetButtons) {
			btEditTagset.setEnabled(false);
			btRemoveTagset.setEnabled(false);
			
			if (withTagButtons) {
				btInsertTag.setEnabled(false);
			}			
		}
		
		if (withTagButtons) {
			btRemoveTag.setEnabled(false);
			btEditTag.setEnabled(false);
		}
		
		if (withPropertyButtons) {
			btInsertProperty.setEnabled(false);
			btRemoveProperty.setEnabled(selected);
			btEditProperty.setEnabled(selected);
		}
	}	

}
