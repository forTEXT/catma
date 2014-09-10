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
package de.catma.ui.dialog;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class FormDialog<T> extends VerticalLayout {
	
	private Window dialogWindow;
	private Form form;
	private Label hint;
	private Button btSave;
	private Button btCancel;
	
	public FormDialog(
			String caption,
			PropertysetItem propertysetItem, 
			SaveCancelListener<PropertysetItem> saveCancelListener) {
		this(caption, propertysetItem, null, saveCancelListener);
	}
	
	public FormDialog(
			String caption,
			PropertysetItem propertysetItem, 
			FormFieldFactory formFieldFactory,
			SaveCancelListener<PropertysetItem> saveCancelListener) {
		this(caption, null, propertysetItem, formFieldFactory, saveCancelListener);
	}
	public FormDialog(
			String caption,
			String hintText,
			PropertysetItem propertysetItem, 
			FormFieldFactory formFieldFactory,
			SaveCancelListener<PropertysetItem> saveCancelListener) {
		initComponents(caption, hintText, propertysetItem, formFieldFactory);
		initAction(saveCancelListener, propertysetItem);
	}
	
	public FormDialog(
			String caption,
			BeanItem<T> beanItem, 
			FormFieldFactory formFieldFactory,
			SaveCancelListener<T> saveCancelListener) {
		initComponents(caption, null, beanItem, formFieldFactory);
		initAction(saveCancelListener, beanItem);
	}
	

	private void initAction(
			final SaveCancelListener<T> saveCancelListener, 
			final BeanItem<T> beanItem) {
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(dialogWindow);
				saveCancelListener.cancelPressed();
			}
		});

		btSave.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					form.commit();
					UI.getCurrent().removeWindow(dialogWindow);
					saveCancelListener.savePressed(beanItem.getBean());
				}
				catch(InvalidValueException ignore) {}
			}
		});	

	}
	
	private void initAction(
		final SaveCancelListener<PropertysetItem> saveCancelListener, 
		final PropertysetItem propertysetItem) {
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(dialogWindow);
				saveCancelListener.cancelPressed();
			}
		});

		btSave.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					form.commit();
					UI.getCurrent().removeWindow(dialogWindow);
					saveCancelListener.savePressed(propertysetItem);
				}
				catch(InvalidValueException ignore) {}
			}
		});	
	}

	private void initComponents(
			String caption,
			String hintText, final PropertysetItem propertysetItem, 
			FormFieldFactory formFieldFactory) {
		
		setSizeFull();
		setSpacing(true);
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		
		dialogWindow = new Window(caption, content);
		dialogWindow.setModal(true);
		
		form = new Form();
		if (formFieldFactory != null) {
			form.setFormFieldFactory(formFieldFactory);
		}
		form.setSizeFull();
		form.setBuffered(true);
		form.setInvalidCommitted(false);
		form.setItemDataSource(propertysetItem);
		
		addComponent(form);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		hint = new Label(hintText);
		hint.setContentMode(ContentMode.TEXT);
		
		addComponent(hint);
		
		btSave = new Button("Save");

		btSave.setClickShortcut(KeyCode.ENTER);

		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btSave);
		buttonPanel.addComponent(btCancel);
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		content.addComponent(this);
		
		form.focus();
	}
	
	
	
	public Field getField(Object propertyId) {
		return form.getField(propertyId);
	}

	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		UI.getCurrent().addWindow(dialogWindow);
	}
	
	public void show() {
		show("25%");
	}

	public void setVisibleItemProperties(Object[] visibleProperties) {
		form.setVisibleItemProperties(visibleProperties);
	}
	

	
}
