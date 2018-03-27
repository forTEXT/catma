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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.ui.AbstractField;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.TreeTable;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
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
	
	// this is a hack to get hold of the filter string and make it available to
	// this edit dialog. Background: the filterstring can be added as a new value
	// in the ComboBox but the addition/ValueChangeEvent is triggered by enter key
	// only, we need to react on new values also on BlurEvents when the filterstring is
	// still only a filterstring and not a full new value yet
	private static class FilterExposingComboBox extends ComboBox {

		private String currentFilterString;

		public FilterExposingComboBox(String caption) {
			super(caption);
		}

		@Override
		public void changeVariables(Object source, Map<String, Object> variables) {
			String newFilter;
	        if ((newFilter = (String) variables.get("filter")) != null) { //$NON-NLS-1$
	        	currentFilterString = newFilter;
	        }
	        super.changeVariables(source, variables);
		}
		
		public String getCurrentFilterString() {
			return currentFilterString;
		}
	}
	
	private TreeTable propertyTree;
	private AdhocPropertyValuesBuffer propertyValuesBuffer;
	private ComboBox newValueInput;
	private Button btAdd;
	private Button btSave;
	private Button btCancel;
	private TagInstance tagInstance;
	private Set<Property> changedProperties;
	private Label hintText;
	private HashMap<Property,List<String>> changeBuffer = new HashMap<>();

	public PropertyEditDialog(TagInstance tagInstance,
			SaveCancelListener<Set<Property>> saveCancelListener,
			AdhocPropertyValuesBuffer propertyValuesBuffer) {
		super(MessageFormat.format(Messages.getString("PropertyEditDialog.editPropertiesFor"), tagInstance.getTagDefinition().getName())); //$NON-NLS-1$
		this.tagInstance = tagInstance;
		this.propertyValuesBuffer = propertyValuesBuffer;
		changedProperties = new HashSet<Property>();
		initComponents();
		initActions(saveCancelListener);
		initData();
	}
	
	@SuppressWarnings("unchecked")
	private void initData() {
		for (Property p : tagInstance.getUserDefinedProperties()) {
			PropertyDefinition propertyDefinition = p.getPropertyDefinition();
			ClassResource pIcon = new ClassResource("tagmanager/resources/ylwdiamd.gif"); //$NON-NLS-1$
			
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
			
			values.addAll(propertyDefinition.getPossibleValueList());
			values.addAll(p.getPropertyValueList());
			
			for (String pValue : values) {
				String pValueItemId = propertyDefinition.getName() + "_" + pValue; //$NON-NLS-1$
				CheckBox cb = createCheckBox(p, pValue);
				
				propertyTree.addItem(
					new Object[] {
							null,
							pValue,
							cb
					},
					pValueItemId);
				
				propertyTree.setParent(pValueItemId, p);
				propertyTree.setChildrenAllowed(pValueItemId, false);
			}
			propertyTree.setCollapsed(p, false);
		}
		
		if (tagInstance.getUserDefinedProperties().size() == 1){
			propertyTree.setValue(
					tagInstance.getUserDefinedProperties().iterator().next());
		}
		
		for (String value : propertyValuesBuffer.getValues()){
			newValueInput.addItem(value);
		}
		
	}

	private CheckBox createCheckBox(final Property p, final String pValue) {
		final CheckBox cb = new CheckBox(
			null, p.getPropertyValueList().contains(pValue));
		
		cb.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				propertyValueChanged(p, pValue, cb.getValue());
			}
		});
		return cb;
	}
	private List<String> getValueListBuffer(Property p) {
		List<String> valueList = changeBuffer.get(p);
		if (valueList == null) {
			valueList = new ArrayList<String>();
			changeBuffer.put(p, valueList);
			valueList.addAll(p.getPropertyValueList());
		}
		return valueList;
	}
	private void propertyValueChanged(Property p, String pValue, boolean add){
		
		List<String> valueList = getValueListBuffer(p);
		
		if (add) {
			valueList.add(pValue);
		}
		else {
			valueList.remove(pValue);
		}
		
		changedProperties.add(
				tagInstance.getProperty(p.getPropertyDefinition().getName()));
		
	}

	private void initActions(
			final SaveCancelListener<Set<Property>> saveCancelListener) {

		newValueInput.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				btSave.setEnabled(newValueInput.getValue() == null);
				if (newValueInput.getValue() == null) {
					btSave.setDescription(""); //$NON-NLS-1$
				}
				else {
					btSave.setDescription(
						Messages.getString("PropertyEditDialog.addValueInfo")); //$NON-NLS-1$
				}
				
			}
		});
		
		
		// we assume that having an input text on blur was intended
		// as a new value that one can set with the btAdd Button
		// therefore we need to set the input string as a value of the ComboBox
		newValueInput.addBlurListener(new BlurListener() {
			
			@Override
			public void blur(BlurEvent event) {
				
				String currentInputText = ((FilterExposingComboBox)newValueInput).getCurrentFilterString();
				
				if ((currentInputText != null) && !currentInputText.trim().isEmpty()) {
					if (!newValueInput.getItemIds().contains(currentInputText)) {
						newValueInput.addItem(currentInputText);
					}
					newValueInput.setValue(currentInputText);
				}
				else {
					newValueInput.clear();
				}
			}
		});
		
		
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(PropertyEditDialog.this);
				saveCancelListener.cancelPressed();
			}
		});
		
		btSave.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				for (Property p : changedProperties) {
					p.setPropertyValueList(changeBuffer.get(p));
				}
				UI.getCurrent().removeWindow(PropertyEditDialog.this);
				saveCancelListener.savePressed(changedProperties);
			}
		});
		
		
		btAdd.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Object selection = propertyTree.getValue();
				final Property property = getProperty(selection);
				final String pValue = (String)newValueInput.getValue();
				
				if ((pValue == null)||(pValue.isEmpty())) {
					Notification.show(
						Messages.getString("PropertyEditDialog.infoTitle"), Messages.getString("PropertyEditDialog.valueMustNotBeEmpty"),  //$NON-NLS-1$ //$NON-NLS-2$
						Type.TRAY_NOTIFICATION);
					return;
				}
				if (property == null) {
					Notification.show(
						Messages.getString("PropertyEditDialog.infoTitle"),  //$NON-NLS-1$
						Messages.getString("PropertyEditDialog.selectOnePropertyFirst"), //$NON-NLS-1$
						Type.TRAY_NOTIFICATION);
					return;
				}
				
				List<String> valueBuffer = getValueListBuffer(property);
				if (valueBuffer.contains(pValue)
					|| property.getPropertyDefinition()
						.getPossibleValueList().contains(pValue)) {
						Notification.show(
							Messages.getString("PropertyEditDialog.infoTitle"), //$NON-NLS-1$
							Messages.getString("PropertyEditDialog.valueAlreadyExists"),  //$NON-NLS-1$
							Type.TRAY_NOTIFICATION);
					return;
				}
				
				propertyValuesBuffer.addValue(pValue);

				String pValueItemId = 
						property.getPropertyDefinition().getName() 
						+ "_" + pValue; //$NON-NLS-1$
				CheckBox cb = createCheckBox(property, pValue);
				propertyTree.addItem(
						new Object[] {
								null,
								pValue,
								cb
						},
						pValueItemId);	
				cb.setValue(true);
				
				propertyTree.setParent(pValueItemId, property);
				propertyTree.setChildrenAllowed(pValueItemId, false);
				newValueInput.setValue(null);
				btSave.setEnabled(true);
			}
		});
	}

	private void initComponents(){
		VerticalLayout mainLayout= new VerticalLayout();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		mainLayout.setSizeFull();
		
		hintText = new Label(Messages.getString("PropertyEditDialog.useCheckboxes")); //$NON-NLS-1$
		mainLayout.addComponent(hintText);
		
		propertyTree = new TreeTable();
		propertyTree.setSelectable(true);

		propertyTree.setSizeFull();
		propertyTree.setPageLength(10);
		propertyTree.setImmediate(true);
		
		propertyTree.focus();
		propertyTree.addShortcutListener(new AbstractField.FocusShortcut(
				propertyTree, KeyCode.ARROW_UP, ModifierKey.CTRL));
		
		propertyTree.addContainerProperty(TreePropertyName.property, String.class, ""); //$NON-NLS-1$
		propertyTree.setColumnHeader(TreePropertyName.property, Messages.getString("PropertyEditDialog.Property")); //$NON-NLS-1$
		
		propertyTree.addContainerProperty(TreePropertyName.icon, Resource.class, ""); //$NON-NLS-1$
		
		propertyTree.addContainerProperty(TreePropertyName.value, String.class, ""); //$NON-NLS-1$
		propertyTree.setColumnHeader(TreePropertyName.value, Messages.getString("PropertyEditDialog.Value")); //$NON-NLS-1$

		propertyTree.addContainerProperty(TreePropertyName.assigned, CheckBox.class, ""); //$NON-NLS-1$
		propertyTree.setColumnHeader(TreePropertyName.assigned, Messages.getString("PropertyEditDialog.Assigned")); //$NON-NLS-1$
		
		propertyTree.setItemCaptionPropertyId(TreePropertyName.property);
		propertyTree.setItemIconPropertyId(TreePropertyName.icon);
		
		propertyTree.setVisibleColumns(
				new Object[] {
						TreePropertyName.property,
						TreePropertyName.value,
						TreePropertyName.assigned
				});

		mainLayout.addComponent(propertyTree);
		mainLayout.setExpandRatio(propertyTree, 1.0f);
		HorizontalLayout comboBox = new HorizontalLayout();
		comboBox.setSpacing(true);
		
		newValueInput = new FilterExposingComboBox(Messages.getString("PropertyEditDialog.addAdHocValue")); //$NON-NLS-1$
		newValueInput.setTextInputAllowed(true);
		newValueInput.setNewItemsAllowed(true);
		
		newValueInput.setImmediate(true);
		newValueInput.addShortcutListener(new AbstractField.FocusShortcut(
				newValueInput, KeyCode.ARROW_DOWN, ModifierKey.CTRL));
		
		comboBox.addComponent(newValueInput);
		
		btAdd = new Button(Messages.getString("PropertyEditDialog.plus")); //$NON-NLS-1$
		btAdd.setClickShortcut(KeyCode.INSERT);
		comboBox.addComponent(btAdd);
		comboBox.setComponentAlignment(btAdd, Alignment.BOTTOM_RIGHT);
		
		mainLayout.addComponent(comboBox);
		
		hintText = new Label(Messages.getString("PropertyEditDialog.adhocValueInfo")); //$NON-NLS-1$
		mainLayout.addComponent(hintText);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btSave = new Button(Messages.getString("PropertyEditDialog.Save")); //$NON-NLS-1$
		btSave.setClickShortcut(KeyCode.ENTER, ModifierKey.ALT);
		buttonPanel.addComponent(btSave);
		buttonPanel.setComponentAlignment(btSave, Alignment.MIDDLE_RIGHT);
		
		btCancel = new Button(Messages.getString("PropertyEditDialog.Cancel")); //$NON-NLS-1$
		btCancel.setClickShortcut(KeyCode.ESCAPE);
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		mainLayout.addComponent(buttonPanel);
		mainLayout.setComponentAlignment(buttonPanel, Alignment.MIDDLE_RIGHT);
			
		setContent(mainLayout);
		setWidth("40%"); //$NON-NLS-1$
		setHeight("80%"); //$NON-NLS-1$
		setModal(true);
		center();
	}
	
	private Property getProperty(Object selection) {
		while ((selection != null) && !(selection instanceof Property)) {
			selection = propertyTree.getParent(selection);
		}
		return (Property)selection;
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
	
}
