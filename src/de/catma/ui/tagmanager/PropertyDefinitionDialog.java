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

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.IDGenerator;

public class PropertyDefinitionDialog extends VerticalLayout {

	private TextField nameInput;
	private ListSelect valueInput;
	private Button btRemove;
	private Window window;
	private TextField newValueInput;
	private Button btAdd;
	private Button btSave;
	private Button btCancel;
	private PropertyDefinition propertyDefinition;

	public PropertyDefinitionDialog(
			String caption, PropertyDefinition propertyDefinition,
			SaveCancelListener<PropertyDefinition> saveCancelListener) {
		this.propertyDefinition = propertyDefinition; 
		initComponents(caption);
		initActions(saveCancelListener);
	}

	public PropertyDefinitionDialog(
			String caption, 
			SaveCancelListener<PropertyDefinition> saveCancelListener) {
		this(caption, null, saveCancelListener);
	}

	private void initActions(final SaveCancelListener<PropertyDefinition> saveCancelListener) {
		btAdd.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				String val = (String)newValueInput.getValue();
				if ((val == null)||(val.isEmpty())) {
					Notification.show(
						Messages.getString("PropertyDefinitionDialog.infoTitle"), Messages.getString("PropertyDefinitionDialog.valueMustNotBeEmpty"),  //$NON-NLS-1$ //$NON-NLS-2$
						Type.TRAY_NOTIFICATION);
				}
				else {
					valueInput.addItem(val);
					newValueInput.setValue(""); //$NON-NLS-1$
				}
				
			}
		});
		
		btRemove.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				if (valueInput.getValue() != null) {
					valueInput.removeItem(valueInput.getValue());
				}
				else {
					Notification.show(
							Messages.getString("PropertyDefinitionDialog.infoTitle"), Messages.getString("PropertyDefinitionDialog.selectAValueFirst"),  //$NON-NLS-1$ //$NON-NLS-2$
							Type.TRAY_NOTIFICATION);
				}
				
			}
		});
		
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(window);
				saveCancelListener.cancelPressed();
			}
		});
		
		btSave.addClickListener(new ClickListener() {
			
			@SuppressWarnings("unchecked")
			public void buttonClick(ClickEvent event) {
				ArrayList<String> values = new ArrayList<String>();
				values.addAll((Collection<? extends String>) valueInput.getItemIds());
				if (propertyDefinition == null)  {
					propertyDefinition = 
						new PropertyDefinition(
							null, new IDGenerator().generate(), 
							(String)nameInput.getValue(),
							new PropertyPossibleValueList(values, true));
				}
				else {
					propertyDefinition.setName((String)nameInput.getValue());
					propertyDefinition.setPossibleValueList(
							new PropertyPossibleValueList(values, true));
				}
				UI.getCurrent().removeWindow(window);
				saveCancelListener.savePressed(propertyDefinition);
			}
		});
	}

	private void initComponents(
		String caption) {
		setMargin(true);
		setSpacing(true);
		
		GridLayout propPanel = new GridLayout(3, 3);
		propPanel.setSpacing(true);
		
		nameInput = new TextField(Messages.getString("PropertyDefinitionDialog.name")); //$NON-NLS-1$
		nameInput.setRequired(true);
		if (propertyDefinition != null) {
			nameInput.setValue(propertyDefinition.getName());
		}
		propPanel.addComponent(nameInput, 0, 0, 2, 0);
		
		if (propertyDefinition != null) {
			valueInput = new ListSelect(
				Messages.getString("PropertyDefinitionDialog.possibleValues"),  //$NON-NLS-1$
				propertyDefinition.getPossibleValueList().getPropertyValueList().getValues());
		}
		else {
			valueInput = new ListSelect(Messages.getString("PropertyDefinitionDialog.possibleValues")); //$NON-NLS-1$
		}
		valueInput.setWidth("100%"); //$NON-NLS-1$
		valueInput.setRequired(true);
		valueInput.setNullSelectionAllowed(false);
		
		propPanel.addComponent(valueInput, 0, 1, 2, 1);
		
		newValueInput = new TextField(Messages.getString("PropertyDefinitionDialog.addPossibleValue")); //$NON-NLS-1$
		propPanel.addComponent(newValueInput, 0, 2);
		
		btAdd = new Button(Messages.getString("PropertyDefinitionDialog.Plus")); //$NON-NLS-1$
		propPanel.addComponent(btAdd, 1, 2);
		propPanel.setComponentAlignment(btAdd, Alignment.BOTTOM_CENTER);
		
		btRemove = new Button(Messages.getString("PropertyDefinitionDialog.Minus")); //$NON-NLS-1$
		propPanel.addComponent(btRemove, 2, 2);
		propPanel.setComponentAlignment(btRemove, Alignment.BOTTOM_CENTER);
		
		addComponent(propPanel);
		
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btSave = new Button(Messages.getString("PropertyDefinitionDialog.Save")); //$NON-NLS-1$
		buttonPanel.addComponent(btSave);
		buttonPanel.setComponentAlignment(btSave, Alignment.MIDDLE_RIGHT);
		
		btCancel = new Button(Messages.getString("PropertyDefinitionDialog.Cancel")); //$NON-NLS-1$
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		addComponent(buttonPanel);
		setComponentAlignment(buttonPanel, Alignment.MIDDLE_RIGHT);
		
		window = new Window(caption);
		window.setContent(this);
		window.setWidth("30%"); //$NON-NLS-1$
		window.setHeight("70%"); //$NON-NLS-1$
		window.center();
	}
	
	
	public void show() {
		UI.getCurrent().addWindow(window);
	}
	
}
