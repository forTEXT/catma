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
package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagInstance;
import de.catma.ui.dialog.SaveCancelListener;

public class PropertyEditDialog extends Window {

	private static enum TreePropertyName {
		property,
		value,
		assigned, 
		;
	}
	
	private TreeTable propertyTree;
	private TextField newValueInput;
	private Button btAdd;
	private Button btSave;
	private Button btCancel;
	private TagInstance tagInstance;

	public PropertyEditDialog(TagInstance tagInstance,
			SaveCancelListener<List<Property>> saveCancelListener) {
		this.tagInstance = tagInstance; 
		initComponents();
		initActions(saveCancelListener);
		initData();
	}

	private void initData() {
		for (Property p : tagInstance.getUserDefinedProperties()) {
			PropertyDefinition propertyDefinition = p.getPropertyDefinition();
			
			propertyTree.addItem(
					new Object[] {
							propertyDefinition.getName(),
							null,
							null},
					propertyDefinition);
			propertyTree.setChildrenAllowed(propertyDefinition, true);
			
			for (String pValue : 
				propertyDefinition.getPossibleValueList().getPropertyValueList().getValues()) {
				
				String pValueItemId = propertyDefinition.getUuid() + "_" + pValue;
				propertyTree.addItem(
					new Object[] {
							null,
							pValue,
							createCheckBox(p, pValue)
					},
					pValueItemId);
				
				propertyTree.setParent(pValueItemId, propertyDefinition);
				propertyTree.setChildrenAllowed(pValueItemId, false);
			}
		
			propertyTree.setCollapsed(propertyDefinition, false);
		}
	}

	private CheckBox createCheckBox(final Property p, final String pValue) {
		final CheckBox cb = new CheckBox(
			null, p.getPropertyValueList().getValues().contains(pValue));
		
		cb.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				propertyValueChanged(p, pValue, cb.booleanValue());
			}
		});
		
		return cb;
	}

	private void propertyValueChanged(Property p, String pValue, boolean add) {
		
		List<String> valueList = new ArrayList<String>();
		valueList.addAll(p.getPropertyValueList().getValues());
		if (add) {
			valueList.add(pValue);
		}
		else {
			valueList.remove(pValue);
		}
		
		p.setPropertyValueList(new PropertyValueList(valueList));
		
		// TODO: bookkeeping about which p has changed for saveCancelListener
	}

	private void initActions(final SaveCancelListener<List<Property>> saveCancelListener){
		// TODO: call save/cancel
		btCancel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				getParent().removeWindow(PropertyEditDialog.this);
				saveCancelListener.cancelPressed();
			}
		});
		// TODO: add new value functionality
	}

	private void initComponents(){
		VerticalLayout mainLayout= new VerticalLayout();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		
		propertyTree = new TreeTable();
		propertyTree.setSelectable(true);
		propertyTree.setMultiSelect(true);
		propertyTree.setSizeFull();
		
		propertyTree.addContainerProperty(TreePropertyName.property, String.class, "");
		propertyTree.setColumnHeader(TreePropertyName.property, "Property");
		
		propertyTree.addContainerProperty(TreePropertyName.value, String.class, "");
		propertyTree.setColumnHeader(TreePropertyName.value, "Value");

		propertyTree.addContainerProperty(TreePropertyName.assigned, CheckBox.class, "");
		propertyTree.setColumnHeader(TreePropertyName.assigned, "Assigned");

		mainLayout.addComponent(propertyTree);
		
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
		setModal(true);
		center();
	}

	public void show(Window parent) {
		parent.addWindow(this);
	}
	
}
