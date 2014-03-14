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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
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
		icon,
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
	private Set<Property> changedProperties;
	private Label hintText;
	private boolean init = false;

	public PropertyEditDialog(TagInstance tagInstance,
			SaveCancelListener<Set<Property>> saveCancelListener) {
		super("Edit Properties for Tag "
				+tagInstance.getTagDefinition().getName());
		this.tagInstance = tagInstance;
		changedProperties = new HashSet<Property>();
		initComponents();
		initActions(saveCancelListener);
	}
	
	@Override
	public void attach() {
		super.attach();
		
		if (!init ) {
			initData();
			init = true;
		}
	}
	

	private void initData() {
		for (Property p : tagInstance.getUserDefinedProperties()) {
			PropertyDefinition propertyDefinition = p.getPropertyDefinition();
			ClassResource pIcon = new ClassResource(
					"ui/tagmanager/resources/ylwdiamd.gif", getApplication());
			
			propertyTree.addItem(
					new Object[] {
							propertyDefinition.getName(),
							null,
							null},
					p);
			propertyTree.getContainerProperty(
					p, TreePropertyName.icon).setValue(pIcon);
			propertyTree.setChildrenAllowed(p, true);
			
			
			Set<String> values = new HashSet<String>();
			values.addAll(propertyDefinition.getPossibleValueList().getPropertyValueList().getValues());
			values.addAll(p.getPropertyValueList().getValues());
			
			for (String pValue : values) {
				String pValueItemId = propertyDefinition.getUuid() + "_" + pValue;
				propertyTree.addItem(
					new Object[] {
							null,
							pValue,
							createCheckBox(p, pValue)
					},
					pValueItemId);
				
				propertyTree.setParent(pValueItemId, p);
				propertyTree.setChildrenAllowed(pValueItemId, false);
			}
			
			propertyTree.setCollapsed(p, false);
		}
		
		if (tagInstance.getUserDefinedProperties().size() == 1){
			propertyTree.setValue(tagInstance.getUserDefinedProperties().iterator().next());
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
		
		changedProperties.add(
				tagInstance.getProperty(p.getPropertyDefinition().getUuid()));
		
	}

	private void initActions(final SaveCancelListener<Set<Property>> saveCancelListener){

		btCancel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				getParent().removeWindow(PropertyEditDialog.this);
				saveCancelListener.cancelPressed();
			}
		});
		
		btSave.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				
				
				getParent().removeWindow(PropertyEditDialog.this);
				saveCancelListener.savePressed(changedProperties);
			}
		});
		
		btAdd.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object selection = propertyTree.getValue();
				final Property property = getProperty(selection);
				final String pValue = (String)newValueInput.getValue();
				if ((pValue == null)||(pValue.isEmpty())) {
					getApplication().getMainWindow().showNotification(
						"Info", "The value can not be empty!", 
						Notification.TYPE_TRAY_NOTIFICATION);
				}
				else {
							
					if (property == null) {
						getWindow().showNotification(
							"Information", 
							"Please select exactly one Property from the list first!",
							Notification.TYPE_TRAY_NOTIFICATION);
					}
					else {
						if (property.getPropertyValueList().getValues().contains(
								pValue) ||
							property.getPropertyDefinition()
								.getPossibleValueList().getPropertyValueList().getValues().contains(pValue)){
								getApplication().getMainWindow().showNotification(
										"Info", "This value already exists. Please choose another name!", 
										Notification.TYPE_TRAY_NOTIFICATION);
						}
						
						propertyValueChanged(property, pValue, true);
						String pValueItemId = property.getPropertyDefinition().getUuid() + "_" + pValue;
						propertyTree.addItem(
								new Object[] {
										null,
										pValue,
										createCheckBox(property, pValue)
								},
								pValueItemId);		
						propertyTree.setParent(pValueItemId, property.getPropertyDefinition());
						propertyTree.setChildrenAllowed(pValueItemId, false);
						newValueInput.setValue("");
						
					}
					
				}
			}
		});
	}

	private void initComponents(){
		VerticalLayout mainLayout= new VerticalLayout();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		
		hintText = new Label("Please use the check boxes to select values.");
		mainLayout.addComponent(hintText);
		
		propertyTree = new TreeTable();
		propertyTree.setSelectable(true);

		propertyTree.setSizeFull();
		propertyTree.setPageLength(10);
		propertyTree.setImmediate(true);
		
		propertyTree.addContainerProperty(TreePropertyName.property, String.class, "");
		propertyTree.setColumnHeader(TreePropertyName.property, "Property");
		
		propertyTree.addContainerProperty(TreePropertyName.icon, Resource.class, "");
		
		propertyTree.addContainerProperty(TreePropertyName.value, String.class, "");
		propertyTree.setColumnHeader(TreePropertyName.value, "Value");

		propertyTree.addContainerProperty(TreePropertyName.assigned, CheckBox.class, "");
		propertyTree.setColumnHeader(TreePropertyName.assigned, "Assigned");
		
		propertyTree.setItemCaptionPropertyId(TreePropertyName.property);
		propertyTree.setItemIconPropertyId(TreePropertyName.icon);
		
		propertyTree.setVisibleColumns(
				new Object[] {
						TreePropertyName.property,
						TreePropertyName.value,
						TreePropertyName.assigned
				});

		mainLayout.addComponent(propertyTree);
		
		HorizontalLayout textField = new HorizontalLayout();
		textField.setSpacing(true);
		
		newValueInput = new TextField("Add possible value");
		
		textField.addComponent(newValueInput);

		
		btAdd = new Button("+");
		textField.addComponent(btAdd);
		textField.setComponentAlignment(btAdd, Alignment.BOTTOM_RIGHT);
		
		mainLayout.addComponent(textField);
		
		hintText = new Label("New property values created here exist only for this tag instance! "
				+ "For the creation of new systematic values use the Tag Manager.");
		mainLayout.addComponent(hintText);
		
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
	
	private Property getProperty(Object selection) {
		while ((selection != null) && !(selection instanceof Property)) {
			selection = propertyTree.getParent(selection);
		}
		return (Property)selection;
	}
	

	public void show(Window parent) {
		parent.addWindow(this);
	}
	
}
