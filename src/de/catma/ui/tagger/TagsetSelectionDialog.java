package de.catma.ui.tagger;

import java.io.IOException;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.tagmanager.TagsetTree;

public class TagsetSelectionDialog extends VerticalLayout {
	
	private final static String SORTCAP_PROP = "SORTCAP";
	
	private Window dialogWindow;
	
	private Repository repository;
	private TagLibrary tagLibrary;
	private HierarchicalContainer tagLibraryContainer;
	private Tree tagLibrariesTree;
	private TagsetTree tagsetTree;
	
	public TagsetSelectionDialog(Repository repository) {
		super();
		
		this.repository = repository;
		
		initComponents();
		initListeners();
	}

	private void initComponents() {
		tagLibraryContainer = new HierarchicalContainer();
		tagLibraryContainer.addContainerProperty(SORTCAP_PROP, String.class, null);
		
		//TODO: factor out into component, copied from TagLibraryPanel
		tagLibrariesTree = new Tree();
		tagLibrariesTree.setContainerDataSource(tagLibraryContainer);
		
		tagLibrariesTree.setCaption("Tag Libraries");
		tagLibrariesTree.addStyleName("bold-label-caption");
		tagLibrariesTree.setImmediate(true);
		tagLibrariesTree.setItemCaptionMode(ItemCaptionMode.ID);
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			tagLibrariesTree.addItem(tlr);
			tagLibrariesTree.getItem(tlr).getItemProperty(SORTCAP_PROP).setValue(
					(tlr.toString()==null)?"":tlr.toString());
			tagLibrariesTree.setChildrenAllowed(tlr, false);
		}
		tagLibraryContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });
		
		tagLibrariesTree.addItemClickListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {
				handleTagLibrariesTreeItemClick(event);				
			}
		});
		
		addComponent(tagLibrariesTree);
		
		tagsetTree = new TagsetTree(repository.getTagManager(), null, false, false, false, false, true, null);
		addComponent(tagsetTree);
		
		setMargin(true);
		
		dialogWindow = new Window("Open Tagset");
		dialogWindow.setContent(this);
	}
	
	private void initListeners() {
		tagsetTree.addBtLoadIntoDocumentListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(dialogWindow);			
			}
		});
	}

	private void handleTagLibrariesTreeItemClick(ItemClickEvent event) {
		TagLibraryReference tagLibraryReference = ((TagLibraryReference)event.getItemId());
		
		if (tagLibrary == null || tagLibrary.getId() != tagLibraryReference.getId()) {
			try {
				tagsetTree.getTagTree().getContainerDataSource().removeAllItems();
				tagLibrary = repository.getTagLibrary(tagLibraryReference);
				tagsetTree.addTagsetDefinition(tagLibrary.collection());
				
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error opening the Tag Library!", e);
			}
		}		
	}
	
	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		dialogWindow.setStyleName("open-tag-set");
		dialogWindow.setModal(true);
		UI.getCurrent().addWindow(dialogWindow);
		dialogWindow.center();
	}
	
	public void show() {
		show("40%");
	}
}
