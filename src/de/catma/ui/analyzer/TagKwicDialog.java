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
		btCancel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				window.getParent().removeWindow(window);
				saveCancelListener.cancelPressed();
			}
		});
		
		btOk.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					window.getParent().removeWindow(window);
					Map<String,UserMarkupCollection> result = collectionUmc();
					saveCancelListener.savePressed(result);
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
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
				for (UserMarkupCollectionReference curChild : children) {
					boolean isTarget = ((CheckBox)(umcTable.getItem(
							curChild).getItemProperty(
								UmcTableProperty.TARGET)).getValue()).booleanValue();
					if (isTarget) {
						UserMarkupCollection umc = 
								repository.getUserMarkupCollection(curChild);
						result.put(((SourceDocument)itemId).getID(), umc);
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
		umcTable.setColumnHeader(UmcTableProperty.TARGET, "Markup goes here");
		umcTable.setSizeFull();
		
		addComponent(umcTable);
		
		setExpandRatio(umcTable, 1.0f);
		
		btOk = new Button("Ok");
		btCancel = new Button("Cancel");
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		
		buttonPanel.addComponent(btOk);
		buttonPanel.addComponent(btCancel);
		
		buttonPanel.setComponentAlignment(btOk, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		addComponent(buttonPanel);
		
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
		CheckBox cb = new CheckBox(null, isTarget);
		cb.setImmediate(true);
		cb.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (!valueChangeEventActive) {
					valueChangeEventActive = true;
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
	
	public void show(Window parent) {
		parent.addWindow(window);
	}
}
