package de.catma.ui.tagmanager;

import java.util.Iterator;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;

public class TagManagerView extends VerticalLayout implements CloseHandler {
	
	private TabSheet tabSheet;
	private Label noOpenTagLibraries;
	
	public TagManagerView() {
		initComponents();
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
		
		tabSheet.setVisible(false);
		addComponent(tabSheet);
	}

	public void openTagLibrary(TagManager tagManager, TagLibrary tagLibrary) {
		TagLibraryView tagLibraryView = getTagLibraryView(tagLibrary);
		if (tagLibraryView != null) {
			tabSheet.setSelectedTab(tagLibraryView);
		}
		else {
			tagLibraryView = new TagLibraryView(tagManager, tagLibrary);
			Tab tab = tabSheet.addTab(tagLibraryView, tagLibrary.getName());
			tab.setClosable(true);
			tabSheet.setSelectedTab(tab.getComponent());
		}
		
		if (!tabSheet.isVisible()) {
			noOpenTagLibraries.setVisible(false);
			setMargin(false);
			tabSheet.setVisible(true);
			tabSheet.setSizeFull();
			
			// workaround to force a repaint, that provokes to display the tabsheet in full size
			getWindow().setWidth(getWindow().getWidth()+0.1f, getWidthUnits());
		}
	}
	
	
	private TagLibraryView getTagLibraryView(TagLibrary tagLibrary) {
		Iterator<Component> iterator = tabSheet.getComponentIterator();
		while (iterator.hasNext()) {
			Component c = iterator.next();
			TagLibraryView tagLibraryView = (TagLibraryView)c;
			if (tagLibraryView.getTagLibrary().getId().equals(tagLibrary.getId())) {
				return tagLibraryView;
			}
		}
		
		return null;
	}
	

	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		// workaround for http://dev.vaadin.com/ticket/7686
		tabsheet.removeComponent(tabContent);
		((TagLibraryView)tabContent).close();

		try {
			Thread.sleep(5);
		} catch (InterruptedException ex) {
	            //do nothing 
	    }
		if (tabsheet.getComponentCount() == 0) {
			tabsheet.setVisible(false);
			noOpenTagLibraries.setVisible(true);
			setMargin(true);
		}

	}

}
