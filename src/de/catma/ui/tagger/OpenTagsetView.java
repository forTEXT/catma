package de.catma.ui.tagger;

import java.io.IOException;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;

import de.catma.document.repository.Repository;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.tagmanager.TagsetTree;

public class OpenTagsetView extends VerticalLayout {
	
	private final static String SORTCAP_PROP = "SORTCAP";
	
	private Repository repository;
	private TagLibrary tagLibrary;
	private HierarchicalContainer tagLibraryContainer;
	private Tree tagLibrariesTree;
	private TagsetTree tagsetTree;
	
	public OpenTagsetView(Repository repository) {
		super();
		
		this.repository = repository;
		
		initComponents();
	}

	private void initComponents() {
		//TODO: use a splitpanel or margins to separate the two trees vertically
		
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
		
		tagsetTree = new TagsetTree(repository.getTagManager(), null, false, false, false, true, null);
		addComponent(tagsetTree);
		
		setMargin(true);
	}

	protected void handleTagLibrariesTreeItemClick(ItemClickEvent event) {
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
}
