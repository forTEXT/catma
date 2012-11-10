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
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

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
		btAdd.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				String val = (String)newValueInput.getValue();
				if ((val == null)||(val.isEmpty())) {
					getApplication().getMainWindow().showNotification(
						"Info", "The value can not be empty!", 
						Notification.TYPE_TRAY_NOTIFICATION);
				}
				else {
					valueInput.addItem(val);
					newValueInput.setValue("");
				}
				
			}
		});
		
		btRemove.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				if (valueInput.getValue() != null) {
					valueInput.removeItem(valueInput.getValue());
				}
				else {
					getApplication().getMainWindow().showNotification(
							"Info", "Please select a value first!", 
							Notification.TYPE_TRAY_NOTIFICATION);
				}
				
			}
		});
		
		btCancel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				window.getParent().removeWindow(window);
				saveCancelListener.cancelPressed();
			}
		});
		
		btSave.addListener(new ClickListener() {
			
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
				window.getParent().removeWindow(window);
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
		
		nameInput = new TextField("Name");
		nameInput.setRequired(true);
		if (propertyDefinition != null) {
			nameInput.setValue(propertyDefinition.getName());
		}
		propPanel.addComponent(nameInput, 0, 0, 2, 0);
		
		if (propertyDefinition != null) {
			valueInput = new ListSelect(
				"Possible values", 
				propertyDefinition.getPossibleValueList().getPropertyValueList().getValues());
		}
		else {
			valueInput = new ListSelect("Possible values");
		}
		valueInput.setWidth("100%");
		valueInput.setRequired(true);
		valueInput.setNullSelectionAllowed(false);
		
		propPanel.addComponent(valueInput, 0, 1, 2, 1);
		
		newValueInput = new TextField("Add possible value");
		propPanel.addComponent(newValueInput, 0, 2);
		
		btAdd = new Button("+");
		propPanel.addComponent(btAdd, 1, 2);
		propPanel.setComponentAlignment(btAdd, Alignment.BOTTOM_CENTER);
		
		btRemove = new Button("-");
		propPanel.addComponent(btRemove, 2, 2);
		propPanel.setComponentAlignment(btRemove, Alignment.BOTTOM_CENTER);
		
		addComponent(propPanel);
		
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		btSave = new Button("Save");
		buttonPanel.addComponent(btSave);
		buttonPanel.setComponentAlignment(btSave, Alignment.MIDDLE_RIGHT);
		
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		addComponent(buttonPanel);
		setComponentAlignment(buttonPanel, Alignment.MIDDLE_RIGHT);
		
		window = new Window(caption);
		window.setContent(this);
		window.setWidth("30%");
		window.setHeight("70%");
		window.center();
	}
	
	
	public void show(Window parent) {
		parent.addWindow(window);
	}
	
}
