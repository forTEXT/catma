package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;

public class TagManagerView extends VerticalLayout implements CloseHandler {
	
	private TabSheet tabSheet;
	private Label noOpenTagLibraries;
	private PropertyChangeListener tagLibraryChangedListener;
	private TagManager tagManager;
	
	public TagManagerView(TagManager tagManager) {
		this.tagManager = tagManager;
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { // removal
					TagLibrary tagLibrary =
							(TagLibrary)evt.getOldValue();
					TagLibraryView tlv = getTagLibraryView(tagLibrary.getId());
					if (tlv != null) {
						onTabClose(tabSheet, tlv);
					}
				}
				
			}
		};
		initComponents();
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagLibraryChanged, tagLibraryChangedListener);
	}
	
	private void initComponents() {
		tabSheet = new TabSheet();
		noOpenTagLibraries = 
			new Label(
				"There are no open Tag Libraries. " +
				"Please use the Repository Manager to open a Tag Libray.");
		
		tabSheet.setCloseHandler(this);
		
		noOpenTagLibraries.setSizeFull();
		setMargin(true);
		addComponent(noOpenTagLibraries);
		setComponentAlignment(noOpenTagLibraries, Alignment.MIDDLE_CENTER);
		
		addComponent(tabSheet);
		tabSheet.hideTabs(true);
		tabSheet.setHeight("0px");
	}

	public void openTagLibrary(TagLibrary tagLibrary) {
		TagLibraryView tagLibraryView = getTagLibraryView(tagLibrary.getId());
		if (tagLibraryView != null) {
			tabSheet.setSelectedTab(tagLibraryView);
		}
		else {
			tagLibraryView = new TagLibraryView(tagManager, tagLibrary);
			Tab tab = tabSheet.addTab(tagLibraryView, tagLibrary.getName());
			tab.setClosable(true);
			tabSheet.setSelectedTab(tab.getComponent());
		}
		
		if (tabSheet.getComponentCount() != 0) {
			noOpenTagLibraries.setVisible(false);
			setMargin(false);
			tabSheet.hideTabs(false);
			tabSheet.setSizeFull();
		}
	}
	
	
	private TagLibraryView getTagLibraryView(String tagLibraryID) {
		Iterator<Component> iterator = tabSheet.getComponentIterator();
		while (iterator.hasNext()) {
			Component c = iterator.next();
			TagLibraryView tagLibraryView = (TagLibraryView)c;
			if (tagLibraryView.getTagLibrary().getId().equals(tagLibraryID)) {
				return tagLibraryView;
			}
		}
		
		return null;
	}
	

	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		tabsheet.removeComponent(tabContent);
		((TagLibraryView)tabContent).close();

		// workaround for http://dev.vaadin.com/ticket/7686
		try {
			Thread.sleep(5);
		} catch (InterruptedException ex) {
	            //do nothing 
	    }
		
		if (tabsheet.getComponentCount() == 0) {
			 //setVisible(false) doesn't work here because of out of sync errors
			tabSheet.hideTabs(true);
			tabSheet.setHeight("0px");
			
			noOpenTagLibraries.setVisible(true);
			setMargin(true);
		}

	}

}
