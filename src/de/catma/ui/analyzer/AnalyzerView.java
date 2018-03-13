/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.analyzer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.antlr.runtime.RecognitionException;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.ResizeEvent;
import com.vaadin.ui.Window.ResizeListener;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryJob.QueryException;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzer.querybuilder.QueryBuilderWizardFactory;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.repository.MarkupCollectionItem;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabComponent;
import de.catma.util.Equal;

public class AnalyzerView extends VerticalLayout 
implements ClosableTab, TabComponent, GroupedQueryResultSelectionListener, RelevantUserMarkupCollectionProvider, TagKwicResultsProvider {
	
	static interface CloseListener {
		public void closeRequest(AnalyzerView analyzerView);
	}
	
	private DistributionSelectionListener distributionSelectionListener = new DistributionSelectionListener() {
		@Override
		public void queryResultRowsSelected(String label,
				List<QueryResultRow> rows, int x, int y) {
			try {
				boolean markupBased = false;
				if (!rows.isEmpty()) {
					markupBased = (rows.get(0) instanceof TagQueryResultRow);
				}
				KwicPanel kwicPanel = 
						new KwicPanel(repository, AnalyzerView.this, markupBased);
				kwicPanel.addQueryResultRows(rows);
				new KwicWindow(
						MessageFormat.format(
							Messages.getString("AnalyzerView.KWICtitle"), label, x, y),  //$NON-NLS-1$
						kwicPanel).show(); 
			}
			catch (Exception e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						Messages.getString("AnalyzerView.errorAccessingRepo"), e); //$NON-NLS-1$
			}
		}
	};
	private String userMarkupItemDisplayString = Messages.getString("AnalyzerView.MarkupCollections"); //$NON-NLS-1$
	private TextField searchInput;
	private Button btExecSearch;
	private Button btQueryBuilder;
	private Button btWordList;
	private Button btNewTab;
	private Button btHelp;
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
	private Integer disChartVisualizationId;
	private PropertyChangeListener sourceDocumentChangedListener;
	private PropertyChangeListener userMarkupDocumentChangedListener;
	private CloseListener closeListener;
	private PropertyChangeListener corpusChangedListener;
	private IndexInfoSet indexInfoSet;
	private Component resultPanel;
	private ProgressBar searchProgress;
	
	private Object lastTagResultsDialogTagLibrarySelection;
	private Object lastTagResultsDialogTagsetSelection;
	private Float lastTagResultsDialogHeight = null;
	private Float lastTagResultsDialogWidth = null;

	private AnalyzerHelpWindow analyzerHelpWindow = new AnalyzerHelpWindow();
	
	protected Unit lastTagResultsDialogUnit;
	
	public AnalyzerView(
			Corpus corpus, IndexedRepository repository, 
			CloseListener closeListener) throws Exception {
		
		this.corpus = corpus;
		this.closeListener = closeListener;
		this.relevantSourceDocumentIDs = new ArrayList<String>();
		this.relevantUserMarkupCollIDs = new ArrayList<String>();
		this.relevantStaticMarkupCollIDs = new ArrayList<String>();
		this.repository = repository;
		this.indexInfoSet = 
				new IndexInfoSet(
					Collections.<String>emptyList(), 
					Collections.<Character>emptyList(), 
					Locale.ENGLISH);
		initComponents();
		initActions();
		initListeners();
	}
	
	@Override
	public void attach() {
		super.attach();		
	}
	
	
	private void initListeners() {
		//FIXME: the view doesn't get closed when "All documents" is analyzed and the repo gets closed
		//FIXME: update result panels to prevent stale results
		sourceDocumentChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					// no action needed
				}
				else if (evt.getNewValue() == null) { //remove
					if (!relevantSourceDocumentIDs.isEmpty()) {
						SourceDocument sourceDocument = 
								(SourceDocument)evt.getOldValue();
						if (relevantSourceDocumentIDs.contains(sourceDocument.getID())) {
							removeSourceDocumentFromTree(sourceDocument);
							if (relevantSourceDocumentIDs.isEmpty()) {
								closeListener.closeRequest(AnalyzerView.this);
							}
						}
					}
				}
				else { //update
					String oldId = (String) evt.getOldValue();
					if (relevantSourceDocumentIDs.contains(oldId)) {
						removeSourceDocumentFromTree((SourceDocument) evt.getNewValue());
						addSourceDocument((SourceDocument) evt.getNewValue());
					}
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
					if (relevantUserMarkupCollIDs.contains(
							userMarkupCollectionReference.getId())) {

						removeUserMarkupCollectionFromTree(
								userMarkupCollectionReference);
					}
				}
				else { // update
					documentsTree.markAsDirty();
				}
			}
		};
		this.repository.addPropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
				userMarkupDocumentChangedListener);	
		
		this.corpusChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { //remove
					Corpus corpus = (Corpus) evt.getOldValue();
					if ((AnalyzerView.this.corpus != null) 
							&& Equal.nonNull(AnalyzerView.this.corpus.getId(), corpus.getId())) {
						
						//detaching relevant documents from corpus
						//the documents itself are not removed
						AnalyzerView.this.corpus = null; 
					}
				}
				else if (evt.getOldValue() == null) { //add
					// no action needed
				}
				else { 
					Corpus corpus = (Corpus)evt.getNewValue();
					
					if ((AnalyzerView.this.corpus != null) 
							&& Equal.nonNull(AnalyzerView.this.corpus.getId(), corpus.getId())) {
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
			umcItem = new MarkupCollectionItem(
					sd, userMarkupItemDisplayString, true);
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
		documentsTree.removeItem(sourceDocument);
	}

	private void initActions() {
		btExecSearch.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				executeSearch();
			}

		});
		btWordList.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				searchInput.setValue("freq>0"); //$NON-NLS-1$
				executeSearch();
			}

		});
		btQueryBuilder.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showQueryBuilder();
			}
		});
		btNewTab.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				openNewTab();
			}
		});
		btHelp.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if(analyzerHelpWindow.getParent() == null){
					UI.getCurrent().addWindow(analyzerHelpWindow);
				} else {
					UI.getCurrent().removeWindow(analyzerHelpWindow);
				}
				
			}
		});
		
	}
	//opens new analyzer tab with same constraints
	private void openNewTab(){
		((AnalyzerProvider)UI.getCurrent()).analyze(corpus, repository);
	}

	private void showQueryBuilder() {
		QueryOptions queryOptions = new QueryOptions(
				relevantSourceDocumentIDs,
				relevantUserMarkupCollIDs,
				relevantStaticMarkupCollIDs,
				indexInfoSet.getUnseparableCharacterSequences(),
				indexInfoSet.getUserDefinedSeparatingCharacters(),
				indexInfoSet.getLocale(),
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
						
						public void stepSetChanged(WizardStepSetChangedEvent event) {/*noop*/}
						
						public void activeStepChanged(WizardStepActivationEvent event) {/*noop*/}
					},
					queryTree,
					queryOptions,
					corpus);
		
		Window wizardWindow = 
				factory.createWizardWindow(Messages.getString("AnalyzerView.QueryBuilder"), "90%", "85%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		UI.getCurrent().addWindow(wizardWindow);
		wizardWindow.center();
	}


	private void executeSearch() {

		QueryOptions queryOptions = new QueryOptions(
				relevantSourceDocumentIDs,
				relevantUserMarkupCollIDs,
				relevantStaticMarkupCollIDs,
				indexInfoSet.getUnseparableCharacterSequences(),
				indexInfoSet.getUserDefinedSeparatingCharacters(),
				indexInfoSet.getLocale(),
				repository);
		
		QueryJob job = new QueryJob(
				searchInput.getValue().toString(),
				queryOptions);
		
		((BackgroundServiceProvider)UI.getCurrent()).submit(
				Messages.getString("AnalyzerView.Searching"), //$NON-NLS-1$
				job, 
				new ExecutionListener<QueryResult>() {
			public void done(QueryResult result) {
				setSearchState(false);
				phraseResultPanel.setQueryResult(result);
				
				try {
					markupResultPanel.setQueryResult(result);
				} catch (Exception e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						Messages.getString("AnalyzerView.errorAccessingRepo"), e); //$NON-NLS-1$
				} 
				
				if (resultTabSheet.getSelectedTab().equals(markupResultPanel)) {
					if (markupResultPanel.isEmpty() && 
							!phraseResultPanel.isEmpty()) {
						HTMLNotification.show(
							Messages.getString("AnalyzerView.InfoTitle"),  //$NON-NLS-1$
							Messages.getString("AnalyzerView.phraseResultsHint"), //$NON-NLS-1$
							Type.TRAY_NOTIFICATION);
					}
				}
			};
			public void error(Throwable t) {
				setSearchState(false);
				if (t instanceof QueryException) {
					QueryJob.QueryException qe = (QueryJob.QueryException)t;
		            String input = qe.getInput();
		            int idx = ((RecognitionException)qe.getCause()).charPositionInLine;
		            if ((idx >=0) && (input.length() > idx)) {
		                char character = input.charAt(idx);
		            	String message = MessageFormat.format(
		            		Messages.getString("AnalyzerView.queryFormatError"), //$NON-NLS-1$
		            		input,
	                        idx+1,
	                        character);
						HTMLNotification.show(
		            			Messages.getString("AnalyzerView.InfoTitle"), message, Type.TRAY_NOTIFICATION); //$NON-NLS-1$
		            }
		            else {
		            	String message = MessageFormat.format(
		            			Messages.getString("AnalyzerView.generalQueryFormatError"), //$NON-NLS-1$
			            		input);
						HTMLNotification.show(
		            			Messages.getString("AnalyzerView.InfoTitle"), message, Type.TRAY_NOTIFICATION); //$NON-NLS-1$
		            }
				}
				else {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						Messages.getString("AnalyzerView.errorDuringSearch"), t); //$NON-NLS-1$
				}
			}
		});
		setSearchState(true);
	}
	
	private List<String> getSourceDocumentIDs(
			Collection<SourceDocument> sourceDocuments) {
		ArrayList<String> result = new ArrayList<String>();
		for (SourceDocument sd : sourceDocuments) {
			result.add(sd.getID());
		}
		return result;
	}

	public void resultsSelected(GroupedQueryResultSet groupedQueryResultSet) {
		try {
			handleDistributionChartRequest(groupedQueryResultSet);
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				Messages.getString("AnalyzerView.errorShowingDistChart"), //$NON-NLS-1$
				e);
		}
	}
	
	private void initComponents() throws Exception {
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
		topPanel.setSplitPosition(60);
		topPanel.addComponent(searchAndConveniencePanel);
		topPanel.addComponent(documentsPanel);
		addComponent(topPanel);
		
		setExpandRatio(topPanel, 0.24f);
		
		resultPanel = createResultPanel();
		resultPanel.setSizeFull();
		
		addComponent(resultPanel);
		setExpandRatio(resultPanel, 0.75f);
	}

	private void setSearchState(boolean enabled) {
		searchInput.setEnabled(!enabled);
		btExecSearch.setEnabled(!enabled);
		btQueryBuilder.setEnabled(!enabled);
		btWordList.setEnabled(!enabled);
		searchProgress.setIndeterminate(enabled);
		searchProgress.setVisible(enabled);

		resultPanel.setEnabled(!enabled);
	}
	
	private Component createResultPanel() {
		
		resultTabSheet = new TabSheet();
		resultTabSheet.setSizeFull();
		
		Component resultByPhraseView = createResultByPhraseView();
		resultTabSheet.addTab(resultByPhraseView, Messages.getString("AnalyzerView.resultByPhrase")); //$NON-NLS-1$
		
		Component resultByMarkupView = createResultByMarkupView();
		resultTabSheet.addTab(resultByMarkupView, Messages.getString("AnalyzerView.resultByMarkup")); //$NON-NLS-1$
		
		return resultTabSheet;
	}

	private Component createResultByMarkupView() {
		markupResultPanel = new MarkupResultPanel(repository, this, this, this, new QueryOptionsProvider() {
			
			@Override
			public QueryOptions getQueryOptions() {
				return new QueryOptions(
						relevantSourceDocumentIDs,
						relevantUserMarkupCollIDs,
						relevantStaticMarkupCollIDs,
						indexInfoSet.getUnseparableCharacterSequences(),
						indexInfoSet.getUserDefinedSeparatingCharacters(),
						indexInfoSet.getLocale(),
						repository);
			}
		});
		return markupResultPanel;
	}

	private Component createResultByPhraseView() {
		phraseResultPanel = new PhraseResultPanel(repository, this, this, this, new QueryOptionsProvider() {
			
			@Override
			public QueryOptions getQueryOptions() {
				return new QueryOptions(
						relevantSourceDocumentIDs,
						relevantUserMarkupCollIDs,
						relevantStaticMarkupCollIDs,
						indexInfoSet.getUnseparableCharacterSequences(),
						indexInfoSet.getUserDefinedSeparatingCharacters(),
						indexInfoSet.getLocale(),
						repository);
			}
		});
		return phraseResultPanel;
	}

	private Component createDocumentsPanel() throws Exception {

        HorizontalLayout documentsPanel = new HorizontalLayout();
        documentsPanel.setSpacing(true);
        documentsPanel.setMargin(new MarginInfo(true, true, false, true));
        documentsPanel.setWidth("100%"); //$NON-NLS-1$
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption(
				Messages.getString("AnalyzerView.docsConstrainingThisSearch")); //$NON-NLS-1$
		
		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				addSourceDocument(sd);
			}
		}
		else {
			for (SourceDocument sd : repository.getSourceDocuments()) {
				addSourceDocument(sd);
			}
			documentsTree.addItem(Messages.getString("AnalyzerView.AllDocuments")); //$NON-NLS-1$
		}
		
		documentsPanel.addComponent(documentsTree);
		documentsPanel.setExpandRatio(documentsTree, 1.0f);
		btNewTab = new Button(Messages.getString("AnalyzerView.NewQuery")); //$NON-NLS-1$
		btNewTab.setDescription(Messages.getString("AnalyzerView.NewTabSameConstrains")); //$NON-NLS-1$
		documentsPanel.addComponent(btNewTab);
		documentsPanel.setComponentAlignment(btNewTab, Alignment.TOP_RIGHT);
		return documentsPanel;
	}

	private void addSourceDocument(SourceDocument sd) {
		relevantSourceDocumentIDs.add(sd.getID());
		//TODO: provide a facility where the user can select between different IndexInfoSets
		indexInfoSet = 
				sd.getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet();
		
		documentsTree.addItem(sd);
		MarkupCollectionItem umc = 
			new MarkupCollectionItem(
					sd, 
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
		this.relevantUserMarkupCollIDs.add(umcRef.getId());
		documentsTree.addItem(umcRef);
		documentsTree.setParent(umcRef, umc);
		documentsTree.setChildrenAllowed(umcRef, false);
	}


	private Component createConvenienceButtonPanel() {
		HorizontalLayout convenienceButtonPanel = new HorizontalLayout();
		convenienceButtonPanel.setSpacing(true);
		
		btQueryBuilder = new Button(Messages.getString("AnalyzerView.QueryBuilder")); //$NON-NLS-1$
		btQueryBuilder.addStyleName("secondary-button"); //$NON-NLS-1$
		convenienceButtonPanel.addComponent(btQueryBuilder);
		
		btWordList = new Button(Messages.getString("AnalyzerView.Wordlist")); //$NON-NLS-1$
		btWordList.addStyleName("primary-button"); //$NON-NLS-1$
		convenienceButtonPanel.addComponent(btWordList);
		
		btHelp = new Button(FontAwesome.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$

		convenienceButtonPanel.addComponent(btHelp);
	
		return convenienceButtonPanel;
	}

	private Component createSearchPanel() {
		HorizontalLayout searchPanel = new HorizontalLayout();
		searchPanel.setSpacing(true);
		searchPanel.setWidth("100%"); //$NON-NLS-1$
		
		searchInput = new TextField();
		searchInput.setCaption(Messages.getString("AnalyzerView.Query")); //$NON-NLS-1$
		searchInput.setWidth("100%"); //$NON-NLS-1$
		searchInput.setImmediate(true);
		
		searchPanel.addComponent(searchInput);
		searchPanel.setExpandRatio(searchInput, 1.0f);
		
		btExecSearch = new Button(Messages.getString("AnalyzerView.ExecQuery")); //$NON-NLS-1$
		
		searchPanel.addComponent(btExecSearch);
		searchPanel.setComponentAlignment(btExecSearch, Alignment.BOTTOM_CENTER);
		
		searchProgress = new ProgressBar();
		searchProgress.setIndeterminate(false);
		searchProgress.setVisible(false);
		
		searchPanel.addComponent(searchProgress);
		searchPanel.setComponentAlignment(searchProgress, Alignment.BOTTOM_CENTER);
		
		return searchPanel;
	}

	private void handleDistributionChartRequest(
			GroupedQueryResultSet groupedQueryResultSet) throws IOException {

		DistributionComputation dc = new DistributionComputation(
				groupedQueryResultSet, repository, relevantSourceDocumentIDs);
		dc.compute();
		
		this.disChartVisualizationId = 
			((CatmaApplication)UI.getCurrent()).addVisualization(
				disChartVisualizationId, (corpus==null)?Messages.getString("AnalyzerView.AllDocuments"):corpus.toString(), dc, //$NON-NLS-1$
				distributionSelectionListener);
	}

	public void close() {
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.sourceDocumentChanged,
				sourceDocumentChangedListener);
		
		this.repository.removePropertyChangeListener(
				Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
				userMarkupDocumentChangedListener);	
		
		this.repository.removePropertyChangeListener(
				RepositoryChangeEvent.corpusChanged, corpusChangedListener);
		
		closeListener = null;
	}
	
	public void addClickshortCuts() {
		btExecSearch.setClickShortcut(KeyCode.ENTER);
	}
	
	public void removeClickshortCuts() {
		btExecSearch.removeClickShortcut();
	}
	
	public List<String> getRelevantUserMarkupCollectionIDs() {
		return Collections.unmodifiableList(relevantUserMarkupCollIDs);
	}
	
	public Corpus getCorpus() {
		return corpus;
	}
	
	@Override
	public void tagResults() {
		final TagResultsDialog tagResultsDialog = new TagResultsDialog(
			repository, lastTagResultsDialogTagLibrarySelection, lastTagResultsDialogTagsetSelection);
		tagResultsDialog.addCloseListener(new com.vaadin.ui.Window.CloseListener() {
			
			@Override
			public void windowClose(CloseEvent e) {
				lastTagResultsDialogTagLibrarySelection = tagResultsDialog.getCurrenTagLibraryTreeSelection();
				lastTagResultsDialogTagsetSelection = tagResultsDialog.getCurrentTagsetTreeSelection();
				lastTagResultsDialogTagLibrarySelection = tagResultsDialog.getCurrenTagLibraryTreeSelection();
				lastTagResultsDialogTagsetSelection = tagResultsDialog.getCurrentTagsetTreeSelection();
			}
		});
		
		tagResultsDialog.addResizeListener(new ResizeListener() {
			
			@Override
			public void windowResized(ResizeEvent e) {
				lastTagResultsDialogHeight = tagResultsDialog.getHeight();
				lastTagResultsDialogWidth = tagResultsDialog.getWidth();
				lastTagResultsDialogUnit = tagResultsDialog.getHeightUnits();
			}
			
		});

		tagResultsDialog.show(lastTagResultsDialogHeight, lastTagResultsDialogWidth, lastTagResultsDialogUnit);
	}
}
