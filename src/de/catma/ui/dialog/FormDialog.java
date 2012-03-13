package de.catma.ui.dialog;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class FormDialog extends VerticalLayout {
	
	public interface SaveCancelListener {
		public void savePressed(PropertysetItem propertysetItem);
		public void cancelPressed();
	}
	
	private Window dialogWindow;
	private Form form;
	
	public FormDialog(
			String caption,
			PropertysetItem propertysetItem, 
			SaveCancelListener saveCancelListener) {
		this(caption, propertysetItem, null, saveCancelListener);
	}
	
	public FormDialog(
			String caption,
			PropertysetItem propertysetItem, 
			FormFieldFactory formFieldFactory,
			SaveCancelListener saveCancelListener) {
		initComponents(caption, propertysetItem, formFieldFactory, saveCancelListener);
	}

	private void initComponents(
			String caption,
			final PropertysetItem propertysetItem, 
			FormFieldFactory formFieldFactory, 
			final SaveCancelListener saveCancelListener) {
		setSizeFull();
		setSpacing(true);
		
		dialogWindow = new Window(caption);
		dialogWindow.setModal(true);
		
		form = new Form();
		if (formFieldFactory != null) {
			form.setFormFieldFactory(formFieldFactory);
		}
		form.setSizeFull();
		form.setWriteThrough(false);
		form.setInvalidCommitted(false);
		form.setItemDataSource(propertysetItem);

		addComponent(form);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		Button btSave = new Button("Save");
		btSave.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					form.commit();
					dialogWindow.getParent().removeWindow(dialogWindow);
					saveCancelListener.savePressed(propertysetItem);
				}
				catch(InvalidValueException ignore) {}
			}
		});
		btSave.setClickShortcut(KeyCode.ENTER);

		Button btCancel = new Button("Cancel");
		btCancel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				dialogWindow.getParent().removeWindow(dialogWindow);
				saveCancelListener.cancelPressed();
			}
		});
		buttonPanel.addComponent(btSave);
		buttonPanel.addComponent(btCancel);
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		dialogWindow.addComponent(this);
		
		form.focus();
	}
	
	
	
	public Field getField(Object propertyId) {
		return form.getField(propertyId);
	}

	public void show(Window parent, String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		parent.addWindow(dialogWindow);
	}
	
	public void show(Window parent) {
		show(parent, "25%");
	}
}
