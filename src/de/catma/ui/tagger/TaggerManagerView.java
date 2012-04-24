package de.catma.ui.tagger;

import java.util.Iterator;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagManager;

public class TaggerManagerView extends VerticalLayout implements CloseHandler {
	
	private TabSheet tabSheet;
	private Label noOpenTaggers;
	private int nextTaggerID = 0;
	
	public TaggerManagerView() {
		initComponents();
	}
	
	private void initComponents() {
		tabSheet = new TabSheet();
		noOpenTaggers = 
			new Label(
				"There are no open Taggers. " +
				"Please use the Repository Manager to open a Source Document in a Tagger.");
		
		tabSheet.setCloseHandler(this);
		
		noOpenTaggers.setSizeFull();
		setMargin(true);
		addComponent(noOpenTaggers);
		setComponentAlignment(noOpenTaggers, Alignment.MIDDLE_CENTER);
		
		addComponent(tabSheet);
		tabSheet.hideTabs(true);
		tabSheet.setHeight("0px");
	}

	public TaggerView openSourceDocument(
			TagManager tagManager, SourceDocument sourceDocument, Repository repository) {
		TaggerView taggerView = getTaggerView(sourceDocument);
		if (taggerView != null) {
			tabSheet.setSelectedTab(taggerView);
		}
		else {
			taggerView = new TaggerView(
					nextTaggerID++, tagManager, sourceDocument, repository);
			Tab tab = tabSheet.addTab(taggerView, sourceDocument.toString());
			tab.setClosable(true);
			tabSheet.setSelectedTab(tab.getComponent());
		}
		
		if (tabSheet.getComponentCount() != 0) {
			noOpenTaggers.setVisible(false);
			setMargin(false);
			tabSheet.hideTabs(false);
			tabSheet.setSizeFull();
		}
		
		return taggerView;
	}
	
	
	private TaggerView getTaggerView(SourceDocument sourceDocument) {
		Iterator<Component> iterator = tabSheet.getComponentIterator();
		while (iterator.hasNext()) {
			Component c = iterator.next();
			TaggerView taggerView = (TaggerView)c;
			if (taggerView.getSourceDocument().getID().equals(sourceDocument.getID())) {
				return taggerView;
			}
		}
		
		return null;
	}
	

	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		
		tabsheet.removeComponent(tabContent);
		((TaggerView)tabContent).close();

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
			
			noOpenTaggers.setVisible(true);
			setMargin(true);
		}

	}

	public void openUserMarkupCollection(TaggerView taggerView,
			UserMarkupCollection userMarkupCollection) {
		taggerView.openUserMarkupCollection(userMarkupCollection);
	}
}
