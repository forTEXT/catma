package de.catma.ui.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.IndexerProvider;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.tabbedview.ClosableTab;

public class AnalyzerView extends VerticalLayout implements ClosableTab {
	
	private TextField searchInput;
	private Button btExecSearch;
	private Button btQueryBuilder;
	private Button btWordList;
	private HierarchicalContainer documentsContainer;
	private Tree documentsTree;
	private TabSheet resultTabSheet;
	private PhraseResultPanel phraseResultPanel;
	private Repository repository;
	private List<String> selectedDocumentIDs;
	
	public AnalyzerView(
			Corpus corpus, Repository repository) {
		this.selectedDocumentIDs = new ArrayList<String>();
		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				this.selectedDocumentIDs.add(sd.getID());
			}
		}
		this.repository = repository;
		initComponents(corpus);
		initActions();
	}

	private void initActions() {
		btExecSearch.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				executeSearch();
			}

		});
		btWordList.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				searchInput.setValue("freq>0");
				executeSearch();
			}

		});
	}

	private void executeSearch() {
		List<String> unseparableCharacterSequences = Collections.emptyList();
		List<Character> userDefinedSeparatingCharacters = Collections.emptyList();
		QueryOptions queryOptions = new QueryOptions(
				selectedDocumentIDs,
				unseparableCharacterSequences,
				userDefinedSeparatingCharacters,
				Locale.ENGLISH);
		
		QueryJob job = new QueryJob(
				searchInput.getValue().toString(),
				((IndexerProvider)getApplication()).getIndexer(), 
				queryOptions);
		
		((BackgroundServiceProvider)getApplication()).submit(job, 
				new ExecutionListener<QueryResult>() {
			public void done(QueryResult result) {
				phraseResultPanel.setQueryResult(result);
			};
			public void error(Throwable t) {
						// TODO Auto-generated method stub
						
			}
		});
	}
	
	private void initComponents(Corpus corpus) {
		setSizeFull();
		
		Component searchPanel = createSearchPanel();
		
		Component convenienceButtonPanel = createConvenienceButtonPanel();
		
		VerticalLayout searchAndConveniencePanel = new VerticalLayout();
		searchAndConveniencePanel.setSpacing(true);
		searchAndConveniencePanel.setMargin(true);
		searchAndConveniencePanel.addComponent(searchPanel);
		searchAndConveniencePanel.addComponent(convenienceButtonPanel);
		
		Component documentsPanel = createDocumentsPanel(corpus);

		HorizontalSplitPanel topPanel = new HorizontalSplitPanel();
		topPanel.setSplitPosition(70);
		topPanel.addComponent(searchAndConveniencePanel);
		topPanel.addComponent(documentsPanel);
		addComponent(topPanel);
		
		setExpandRatio(topPanel, 0.25f);
		
		Component resultPanel = createResultPanel();
		resultPanel.setSizeFull();
		
		addComponent(resultPanel);
		setExpandRatio(resultPanel, 0.75f);
	}

	private Component createResultPanel() {
		
		resultTabSheet = new TabSheet();
		resultTabSheet.setSizeFull();
		
		Component resultByPhraseView = createResultByPhraseView();
		resultTabSheet.addTab(resultByPhraseView, "Result by phrase");
		
		Component resultByMarkupView = createResultByMarkupView();
		resultTabSheet.addTab(resultByMarkupView, "Result by markup");
		
		return resultTabSheet;
	}

	private Component createResultByMarkupView() {
		// TODO Auto-generated method stub
		return new HorizontalLayout();
	}

	private Component createResultByPhraseView() {
		phraseResultPanel = new PhraseResultPanel(repository);
		return phraseResultPanel;
	}

	private Component createDocumentsPanel(Corpus corpus) {
		Panel documentsPanel = new Panel();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption(
				"Documents and collections considered for this search");
		
		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				documentsTree.addItem(sd);
			}
		}
		else {
			documentsTree.addItem("All documents");
		}
		
		documentsPanel.addComponent(documentsTree);
		return documentsPanel;
	}

	private Component createConvenienceButtonPanel() {
		HorizontalLayout convenienceButtonPanel = new HorizontalLayout();
		convenienceButtonPanel.setSpacing(true);
		
		btQueryBuilder = new Button("Query Builder");
		convenienceButtonPanel.addComponent(btQueryBuilder);
		
		btWordList = new Button("Wordlist");
		convenienceButtonPanel.addComponent(btWordList);
	
		return convenienceButtonPanel;
	}

	private Component createSearchPanel() {
		HorizontalLayout searchPanel = new HorizontalLayout();
		searchPanel.setSpacing(true);
		searchPanel.setWidth("100%");
		
		searchInput = new TextField();
		searchInput.setCaption("Query");
		searchInput.setWidth("100%");

		
		searchPanel.addComponent(searchInput);
		searchPanel.setExpandRatio(searchInput, 1.0f);
		
		btExecSearch = new Button("Execute Query");
		btExecSearch.setClickShortcut(KeyCode.ENTER);
		
		searchPanel.addComponent(btExecSearch);
		searchPanel.setComponentAlignment(btExecSearch, Alignment.BOTTOM_CENTER);
		
		return searchPanel;
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

}
