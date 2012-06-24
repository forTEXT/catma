package de.catma.ui.analyzer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

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
import com.vaadin.ui.Window;

import de.catma.CatmaApplication;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.ui.analyzer.PhraseResultPanel.VisualizeGroupedQueryResultSelectionListener;
import de.catma.ui.analyzer.querybuilder.QueryBuilderWizardFactory;
import de.catma.ui.repository.MarkupCollectionItem;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabComponent;

public class AnalyzerView extends VerticalLayout implements ClosableTab, TabComponent {
	static interface CloseListener {
		public void closeRequest(AnalyzerView analyzerView);
	}
	
	private String userMarkupItemDisplayString = "User Markup Collections";
	private String staticMarkupItemDisplayString = "Static Markup Collections";
	private TextField searchInput;
	private Button btExecSearch;
	private Button btQueryBuilder;
	private Button btWordList;
	private HierarchicalContainer documentsContainer;
	private Tree documentsTree;
	private TabSheet resultTabSheet;
	private PhraseResultPanel phraseResultPanel;
	private IndexedRepository repository;
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private MarkupResultPanel markupResultPanel;
	private Corpus corpus;
	private Integer visualizationId;
	private PropertyChangeListener sourceDocumentChangedListener;
	private PropertyChangeListener userMarkupDocumentChangedListener;
	private CloseListener closeListener;
	private PropertyChangeListener corpusChangedListener;
	
	public AnalyzerView(
			Corpus corpus, IndexedRepository repository, 
			CloseListener closeListener) {
		
		this.corpus = corpus;
		this.closeListener = closeListener;
		this.relevantSourceDocumentIDs = new ArrayList<String>();
		this.relevantUserMarkupCollIDs = new ArrayList<String>();
		this.relevantStaticMarkupCollIDs = new ArrayList<String>();
		
		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				this.relevantSourceDocumentIDs.add(sd.getID());
			}
			for (UserMarkupCollectionReference ref : 
				corpus.getUserMarkupCollectionRefs()) {
				this.relevantUserMarkupCollIDs.add(ref.getId());
			}
			for (StaticMarkupCollectionReference ref : 
				corpus.getStaticMarkupCollectionRefs()) {
				this.relevantStaticMarkupCollIDs.add(ref.getId());
			}
		}
		
		this.repository = repository;
		initComponents();
		initActions();
		initListeners();
	}

	private void initListeners() {
		sourceDocumentChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					// no action needed
				}
				else if (evt.getNewValue() == null) { //remove
					if (!relevantSourceDocumentIDs.isEmpty()) {
						removeSourceDocumentFromTree(
								(SourceDocument)evt.getOldValue());
						if (relevantSourceDocumentIDs.isEmpty()) {
							closeListener.closeRequest(AnalyzerView.this);
						}
					}
				}
				else { //update
					documentsTree.requestRepaint();
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentChangedListener);
		
		userMarkupDocumentChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { // insert
					// no action needed
				}
				else if (evt.getNewValue() == null) { // remove
					UserMarkupCollectionReference userMarkupCollectionReference =
							(UserMarkupCollectionReference) evt.getOldValue();
					removeUserMarkupCollectionFromTree(userMarkupCollectionReference);
				}
				else { // update
					documentsTree.requestRepaint();
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
				userMarkupDocumentChangedListener);	
		
		this.corpusChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { //remove
					// no action needed
				}
				else if (evt.getOldValue() == null) { //add
					// no action needed
				}
				else { 
					//update sourcedoc added
					if (evt.getOldValue() instanceof SourceDocument) {
						addSourceDocument((SourceDocument)evt.getOldValue());
					}
					// update usermarkupcoll added
					else if (evt.getOldValue() 
							instanceof UserMarkupCollectionReference) {
						addUserMarkupCollection(
							(UserMarkupCollectionReference)evt.getOldValue());
					}
 				}
			}
		};
		
		repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.corpusChanged,
				corpusChangedListener);
		
	}

	private void addUserMarkupCollection(
			UserMarkupCollectionReference umcRef) {
		SourceDocument sd = repository.getSourceDocument(umcRef);
		
		Collection<?> children = documentsTree.getChildren(sd);
		MarkupCollectionItem umcItem = null;
		if (children != null) {
			for (Object child : children) {
				if ((child instanceof MarkupCollectionItem) &&
						((MarkupCollectionItem)child).isUserMarkupCollectionItem()) {
					umcItem = (MarkupCollectionItem)child;
				}
			}
		}
		if (umcItem == null) {
			umcItem = new MarkupCollectionItem(userMarkupItemDisplayString, true);
			documentsTree.addItem(umcItem);
			documentsTree.setParent(umcItem, sd);
			addUserMarkupCollection(umcRef, umcItem);
		}
	}


	private void removeUserMarkupCollectionFromTree(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		relevantUserMarkupCollIDs.remove(
				userMarkupCollectionReference.getId());
		documentsTree.removeItem(userMarkupCollectionReference);
	}


	private void removeSourceDocumentFromTree(SourceDocument sourceDocument) {
		relevantSourceDocumentIDs.remove(sourceDocument.getID());
		
		for (UserMarkupCollectionReference umcRef :
			sourceDocument.getUserMarkupCollectionRefs()) {
			documentsTree.removeItem(umcRef);
			relevantUserMarkupCollIDs.remove(umcRef.getId());
		}
		
		Collection<?> children = documentsTree.getChildren(sourceDocument);
		if ((children != null) && (children.size() > 0)) {
			documentsTree.removeItem(children.iterator().next());
		}
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
		btQueryBuilder.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showQueryBuilder();
			}
		});
	}

	private void showQueryBuilder() {
		//TODO: handle query options
		List<String> unseparableCharacterSequences = Collections.emptyList();
		List<Character> userDefinedSeparatingCharacters = Collections.emptyList();
		QueryOptions queryOptions = new QueryOptions(
				relevantSourceDocumentIDs,
				relevantUserMarkupCollIDs,
				relevantStaticMarkupCollIDs,
				unseparableCharacterSequences,
				userDefinedSeparatingCharacters,
				Locale.ENGLISH,
				repository);

		final QueryTree queryTree = new QueryTree();
		QueryBuilderWizardFactory factory =
				new QueryBuilderWizardFactory(
					new WizardProgressListener() {
						
						public void wizardCompleted(WizardCompletedEvent event) {
							event.getWizard().removeListener(this);
							searchInput.setValue(queryTree.toString());
							executeSearch();
						}
						
						public void wizardCancelled(WizardCancelledEvent event) {
							event.getWizard().removeListener(this);
							
						}
						
						public void stepSetChanged(WizardStepSetChangedEvent event) {
							// TODO Auto-generated method stub
							
						}
						
						public void activeStepChanged(WizardStepActivationEvent event) {
							// TODO Auto-generated method stub
							
						}
					},
					queryTree,
					queryOptions);
		
		Window wizardWindow = 
				factory.createWizardWindow("Query Builder", "90%", "85%");
		
		getApplication().getMainWindow().addWindow(wizardWindow);
		wizardWindow.center();
	}


	private void executeSearch() {
		//TODO: handle query options
		List<String> unseparableCharacterSequences = Collections.emptyList();
		List<Character> userDefinedSeparatingCharacters = Collections.emptyList();
		QueryOptions queryOptions = new QueryOptions(
				relevantSourceDocumentIDs,
				relevantUserMarkupCollIDs,
				relevantStaticMarkupCollIDs,
				unseparableCharacterSequences,
				userDefinedSeparatingCharacters,
				Locale.ENGLISH,
				repository);
		
		QueryJob job = new QueryJob(
				searchInput.getValue().toString(),
				queryOptions);
		
		((BackgroundServiceProvider)getApplication()).submit(
				"Searching...",
				job, 
				new ExecutionListener<QueryResult>() {
			public void done(QueryResult result) {
				phraseResultPanel.setQueryResult(result);
				//TODO: lazy?!
				try {
					markupResultPanel.setQueryResult(result);
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
						"Error accessing the repository!", e);
				} 
			};
			public void error(Throwable t) {
				((CatmaApplication)getApplication()).showAndLogError(
					"Error during search!", t);
			}
		});
	}
	
	private void initComponents() {
		setSizeFull();
		
		Component searchPanel = createSearchPanel();
		
		Component convenienceButtonPanel = createConvenienceButtonPanel();
		
		VerticalLayout searchAndConveniencePanel = new VerticalLayout();
		searchAndConveniencePanel.setSpacing(true);
		searchAndConveniencePanel.setMargin(true);
		searchAndConveniencePanel.addComponent(searchPanel);
		searchAndConveniencePanel.addComponent(convenienceButtonPanel);
		
		Component documentsPanel = createDocumentsPanel();

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
		markupResultPanel = new MarkupResultPanel(repository);
		return markupResultPanel;
	}

	private Component createResultByPhraseView() {
		phraseResultPanel = new PhraseResultPanel(repository,
				new VisualizeGroupedQueryResultSelectionListener() {
					
					public void setSelected(GroupedQueryResultSet groupedQueryResultSet,
							boolean selected) {
						try {
							handleDistributionChartRequest(
									groupedQueryResultSet, selected);
						} catch (IOException e) {
							((CatmaApplication)getApplication()).showAndLogError(
								"Error showing the distribution chart",
								e);
						}
					}
				});
		return phraseResultPanel;
	}

	private Component createDocumentsPanel() {
		Panel documentsPanel = new Panel();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption(
				"Documents and collections constraining this search");
		
		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				addSourceDocument(sd);
			}
		}
		else {
			documentsTree.addItem("All documents");
		}
		
		documentsPanel.addComponent(documentsTree);
		return documentsPanel;
	}

	private void addSourceDocument(SourceDocument sd) {
		documentsTree.addItem(sd);
		MarkupCollectionItem umc = 
			new MarkupCollectionItem(
					userMarkupItemDisplayString, true);
		documentsTree.addItem(umc);
		documentsTree.setParent(umc, sd);
		for (UserMarkupCollectionReference umcRef :
			sd.getUserMarkupCollectionRefs()) {
			if (corpus.getUserMarkupCollectionRefs().contains(umcRef)) {
				addUserMarkupCollection(umcRef, umc);
			}
		}
	}
	
	private void addUserMarkupCollection(UserMarkupCollectionReference umcRef,
			MarkupCollectionItem umc) {
		documentsTree.addItem(umcRef);
		documentsTree.setParent(umcRef, umc);
		documentsTree.setChildrenAllowed(umcRef, false);
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
		
		searchPanel.addComponent(btExecSearch);
		searchPanel.setComponentAlignment(btExecSearch, Alignment.BOTTOM_CENTER);
		
		return searchPanel;
	}

	private void handleDistributionChartRequest(
			GroupedQueryResultSet groupedQueryResultSet, boolean selected) throws IOException {

		DistributionComputation dc = new DistributionComputation(
				groupedQueryResultSet, repository, relevantSourceDocumentIDs);
		dc.compute();
		
		this.visualizationId = 
			((CatmaApplication)getApplication()).addVisulization(
				visualizationId, (corpus==null)?"All documents":corpus.toString(), dc);
	}

	public void close() {
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentChangedListener);
		
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
				userMarkupDocumentChangedListener);	
		
		closeListener = null;
	}

	public Corpus getCorpus() {
		return corpus;
	}
	
	public void addClickshortCuts() {
		btExecSearch.setClickShortcut(KeyCode.ENTER);
	}
	
	public void removeClickshortCuts() {
		btExecSearch.removeClickShortcut();
	}
}
