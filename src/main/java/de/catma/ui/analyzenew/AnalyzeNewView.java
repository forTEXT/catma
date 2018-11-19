package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.bcel.verifier.VerificationResult;

import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.Corpus;
import de.catma.document.source.IndexInfoSet;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.ui.analyzer.GroupedQueryResultSelectionListener;
import de.catma.ui.analyzer.RelevantUserMarkupCollectionProvider;
import de.catma.ui.analyzer.TagKwicResultsProvider;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabComponent;

public class AnalyzeNewView extends VerticalLayout 
implements ClosableTab, TabComponent, GroupedQueryResultSelectionListener, RelevantUserMarkupCollectionProvider, TagKwicResultsProvider {
	
	public static interface CloseListenerNew{
		public void closeRequest(AnalyzeNewView analyzeNewView);
	}
	
	private IndexedRepository repository;
	private Corpus corpus;
	private CloseListenerNew closeListener;
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private IndexInfoSet indexInfoSet;
	
	 public AnalyzeNewView(Corpus corpus, IndexedRepository repository, CloseListenerNew closeListener2) {
			this.corpus = corpus;
			this.repository= repository;
			this.closeListener = closeListener2;
			this.relevantSourceDocumentIDs = new ArrayList<String>();
			this.relevantUserMarkupCollIDs = new ArrayList<String>();
			this.relevantStaticMarkupCollIDs = new ArrayList<String>();
			this.indexInfoSet = 
					new IndexInfoSet(
						Collections.<String>emptyList(), 
						Collections.<Character>emptyList(), 
						Locale.ENGLISH);
			
			initComponents();
			initListeners();
			initActions();
	
	}
	 private void initListeners() {
		 
	 }
	 private void initComponents() {
			Component searchPanel = createSearchPanel();
		    Component visIconsPanel=	createVisIconsPanel();		
			HorizontalLayout searchAndVisIconsPanel = new HorizontalLayout();
			searchAndVisIconsPanel.addComponents(searchPanel, visIconsPanel);
			searchAndVisIconsPanel.setWidth("100%");
			searchAndVisIconsPanel.setExpandRatio(searchPanel,  0.5f);
			searchAndVisIconsPanel.setExpandRatio(visIconsPanel,  0.5f);
			addComponent(searchAndVisIconsPanel);
		 
	 }
	 private void initActions() {
		 
	 }
	 
	 private void  createResultView() {
		 
	 }
	 
	@Override
	public void tagResults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> getRelevantUserMarkupCollectionIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Corpus getCorpus() {
	return corpus;
	}
	
	private Component createSearchPanel() {
		VerticalLayout searchPanel = new VerticalLayout();
		Label searchPanelLabel = new Label ("Queries");
		HorizontalLayout searchRow = new HorizontalLayout();
		Button btBuildQuery = new Button ("+ BUILD QUERY");
		ComboBox <String >predefQueries = new ComboBox<>();
		predefQueries.setItems("freq>0","tag ='Tag1'","wild = 'someWord'");
		Button btSearch = new Button ("SEARCH");
		btSearch.setWidth("100%");
		searchRow.addComponents(btBuildQuery,predefQueries);
		searchRow.setExpandRatio(predefQueries, 0.7f);
		//searchPanelLabel.setWidth("500px");
		
		searchPanel.addComponents(searchPanelLabel,searchRow,btSearch);	
		return searchPanel;
	}
	
	private Component createVisIconsPanel() {
		VerticalLayout visIconsPanel = new VerticalLayout();
		Label visIconsLabel = new Label ("Visualisations");
		//visIconsLabel.setWidth("500px");
		visIconsPanel.addComponent(visIconsLabel);	
		return visIconsPanel;
		
	}

	@Override
	public void resultsSelected(GroupedQueryResultSet groupedQueryResultSet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addClickshortCuts() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeClickshortCuts() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
