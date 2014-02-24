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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

import de.catma.document.standoffmarkup.usermarkup.TagInstanceInfo;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagsetDefinition;

import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.StringListProperty;
import de.catma.ui.tagger.MarkupPanel;
import de.catma.ui.tagger.TagInstanceTree;
import de.catma.util.IDGenerator;

public class InstancePropertyDefinitionDialog extends Window {

	private static enum TreePropertyName {
		caption,
		propertyname,
		value,
		select, 
		markupcollection, 
		;
	}
	
	private TreeTable instancePropertyTree;
	private ListSelect valueInput;
	private Property property;
	private PropertyDefinition propertyDefinition;
	private TextField newValueInput;
	private Button btAdd;
	private Button btSave;
	private Button btCancel;
	private TagInstance tagInstance;
	private Object tagInstanceTree;
	private int TagInstanceInfo;

	public InstancePropertyDefinitionDialog(TagInstance tagInstance,
			SaveCancelListener<List<Property>> saveCancelListener) {
		this.tagInstance = tagInstance; 
		initComponents();
		initActions(saveCancelListener);
	}

	private void initActions(SaveCancelListener<List<Property>> saveCancelListener){
		
	}

	private void initComponents(){
		VerticalLayout mainLayout= new VerticalLayout();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		
		instancePropertyTree = new TreeTable();
		instancePropertyTree.setSelectable(true);
		instancePropertyTree.setMultiSelect(true);
		instancePropertyTree.setSizeFull();
		
		instancePropertyTree.addContainerProperty(TreePropertyName.propertyname, String.class, "");
		instancePropertyTree.setColumnHeader(TreePropertyName.propertyname, "Property");
		
		instancePropertyTree.addContainerProperty(TreePropertyName.value, String.class, "");
		instancePropertyTree.setColumnHeader(TreePropertyName.value, "Value");
		
		instancePropertyTree.addItem(TagInstance.class);
		instancePropertyTree.setPageLength(instancePropertyTree.size());
		
		
				
		mainLayout.addComponent(instancePropertyTree);
		
		
		HorizontalLayout textField = new HorizontalLayout();
		textField.setSpacing(true);
		
		newValueInput = new TextField("Add possible value");
		textField.addComponent(newValueInput);

		
		btAdd = new Button("+");
		textField.addComponent(btAdd);
		textField.setComponentAlignment(btAdd, Alignment.BOTTOM_RIGHT);
		
		mainLayout.addComponent(textField);
		
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btSave = new Button("Save");
		buttonPanel.addComponent(btSave);
		buttonPanel.setComponentAlignment(btSave, Alignment.MIDDLE_RIGHT);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		mainLayout.addComponent(buttonPanel);
		mainLayout.setComponentAlignment(buttonPanel, Alignment.MIDDLE_RIGHT);

		
			
		setContent(mainLayout);
		setWidth("30%");
		setHeight("70%");
		center();
	}
	
	public void addPropertyTree(PropertyDefinition propertyDefinition) {
		instancePropertyTree.removeAllItems();
				
		instancePropertyTree.addItem(property);
		

		establishHierarchy(propertyDefinition, property);
		
		}	
	
	private void establishHierarchy(
			PropertyDefinition propertyDefinition, Property property) {
		String baseID = propertyDefinition.getUuid();
		if (baseID.isEmpty()) {
			instancePropertyTree.setChildrenAllowed(property, true);
			instancePropertyTree.setParent(propertyDefinition, property);
		}
		else {
			PropertyDefinition parent = property.getPropertyDefinition();
			instancePropertyTree.setChildrenAllowed(parent, true);
			instancePropertyTree.setParent(property, parent);
		}		
	}
	

	public void show(Window parent) {
		parent.addWindow(this);
	}
	
}
