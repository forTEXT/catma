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
package de.catma.ui.analyzer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.ui.dialog.SaveCancelListener;

public class TagKwicDialog extends VerticalLayout {
	
	private enum UmcTableProperty {
		CAPTION,
		TARGET,
		;
	}
	
	private TreeTable umcTable;
	private Button btOk;
	private Button btCancel;
	private boolean valueChangeEventActive = false;
	private SaveCancelListener<Map<String, UserMarkupCollection>> saveCancelListener;
	private Repository repository;
	private Window window;

	public TagKwicDialog(
		SaveCancelListener<Map<String,UserMarkupCollection>> saveCancelListener, Repository repository) {
		this.saveCancelListener = saveCancelListener;
		this.repository = repository;
		initComponents();
		initActions();
	}

	private void initActions() {
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(window);
				saveCancelListener.cancelPressed();
			}
		});
		
		btOk.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					UI.getCurrent().removeWindow(window);
					Map<String,UserMarkupCollection> result = collectionUmc();
					saveCancelListener.savePressed(result);
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"error opening User Markup Collection", e);
					saveCancelListener.cancelPressed();
				}
				
			}

		});
		
	}
	private Map<String, UserMarkupCollection> collectionUmc() throws IOException {
		Map<String, UserMarkupCollection> result = new HashMap<String, UserMarkupCollection>();
		
		Collection<?> itemIds = umcTable.getItemIds();
		for (Object itemId : itemIds) {
			if (itemId instanceof SourceDocument) {
				
				@SuppressWarnings("unchecked")
				Collection<UserMarkupCollectionReference> children = 
						(Collection<UserMarkupCollectionReference>) umcTable.getChildren(itemId);
				if ((children != null) && (!children.isEmpty())) { 
					for (UserMarkupCollectionReference curChild : children) {
						boolean isTarget = ((CheckBox)(umcTable.getItem(
								curChild).getItemProperty(
									UmcTableProperty.TARGET)).getValue()).getValue();
						if (isTarget) {
							UserMarkupCollection umc = 
									repository.getUserMarkupCollection(curChild);
							result.put(((SourceDocument)itemId).getID(), umc);
						}
					}
				}
				
			}
		}
		
		
		return result;
	}

	private void initComponents() {
		window = new Window("Select affected User Markup Collections");
		window.setModal(true);
		
		setSpacing(true);
		setMargin(true);
		setSizeFull();
		
		Label tagResultsLabel = 
			new Label("The selected User Markup Collections will be modfied by this tagging operation. Are sure?");
		addComponent(tagResultsLabel);
		
		umcTable = new TreeTable("User Markup Collections");
		umcTable.addContainerProperty(UmcTableProperty.CAPTION, String.class, null);
		umcTable.setColumnHeader(UmcTableProperty.CAPTION, "Document/Collection");
		umcTable.addContainerProperty(UmcTableProperty.TARGET, Component.class, null);
		umcTable.setColumnHeader(UmcTableProperty.TARGET, "targeted User Markup Collection");
		umcTable.setSizeFull();
		
		addComponent(umcTable);
		
		setExpandRatio(umcTable, 1.0f);
		
		btOk = new Button("Ok");
		btOk.setEnabled(false);
		
		btCancel = new Button("Cancel");
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		
		buttonPanel.addComponent(btOk);
		buttonPanel.addComponent(btCancel);
		
		buttonPanel.setComponentAlignment(btOk, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		addComponent(buttonPanel);
		setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		window.setContent(this);
		window.setWidth("50%");
		window.setHeight("80%");
	}
	
	public void addUserMarkCollections(
			SourceDocument sd, List<UserMarkupCollectionReference> umcRefs, UserMarkupCollectionReference initialTarget) {
		umcTable.addItem(
				new Object[] {
					sd.toString(),
					null
				}, 
				sd);
		
		for (UserMarkupCollectionReference umcRef : umcRefs) {
			umcTable.addItem(
				new Object[] {
						umcRef.toString(),
						createUmcRefCheckBox(umcRef, sd, umcRef.equals(initialTarget))
				}, 
				umcRef);
			umcTable.setChildrenAllowed(umcRef, false);
			umcTable.setParent(umcRef, sd);
		}
		
		umcTable.setCollapsed(sd, false);
	}

	private CheckBox createUmcRefCheckBox(final UserMarkupCollectionReference umcRef,
			final SourceDocument sd, boolean isTarget) {
		if (isTarget) {
			btOk.setEnabled(true);
		}
		CheckBox cb = new CheckBox(null, isTarget);
		cb.setImmediate(true);
		cb.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (!valueChangeEventActive) {
					valueChangeEventActive = true;
					btOk.setEnabled(true);
					@SuppressWarnings("unchecked")
					Collection<UserMarkupCollectionReference> children = 
							(Collection<UserMarkupCollectionReference>) umcTable.getChildren(sd);
					for (UserMarkupCollectionReference curChild : children) {
						if (!umcRef.equals(curChild)) {
							((CheckBox)(umcTable.getItem(
								curChild).getItemProperty(
									UmcTableProperty.TARGET)).getValue()).setValue(false);
						}
						
					}
					valueChangeEventActive = false;
				}
				
			}
		});
		return cb;
	}
	
	public void show() {
		UI.getCurrent().addWindow(window);
	}
}
