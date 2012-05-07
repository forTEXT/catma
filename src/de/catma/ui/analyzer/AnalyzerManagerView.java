package de.catma.ui.analyzer;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;

public class AnalyzerManagerView extends VerticalLayout implements CloseHandler {
	
	private TabSheet tabSheet;
	private Label noOpenAnalyzers;
	
	public AnalyzerManagerView() {
		initComponents();
	}
	// TODO: factor out initComponents and onTabclose stuff
	private void initComponents() {
		tabSheet = new TabSheet();
		noOpenAnalyzers = 
			new Label(
				"Heavy Metal is the Law!");
		
		tabSheet.setCloseHandler(this);
		
		noOpenAnalyzers.setSizeFull();
		setMargin(true);
		addComponent(noOpenAnalyzers);
		setComponentAlignment(noOpenAnalyzers, Alignment.MIDDLE_CENTER);
		
		addComponent(tabSheet);
		tabSheet.hideTabs(true);
		tabSheet.setHeight("0px");	
	}

	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		
		tabsheet.removeComponent(tabContent);
		((AnalyzerView)tabContent).close();

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
			
			noOpenAnalyzers.setVisible(true);
			setMargin(true);
		}
	}

	public void analyzeDocuments(Corpus corpus, Repository repository) {
		//TODO: make equal titles distinguishable
		AnalyzerView analyzerView = 
				new AnalyzerView(corpus, repository);
		Tab tab = tabSheet.addTab(
				analyzerView, (corpus == null)? "All documents" : corpus.toString());
		tab.setClosable(true);
		tabSheet.setSelectedTab(tab.getComponent());

		
		if (tabSheet.getComponentCount() != 0) {
			noOpenAnalyzers.setVisible(false);
			setMargin(false);
			tabSheet.hideTabs(false);
			tabSheet.setSizeFull();
		}
	}
}
