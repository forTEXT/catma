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
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

import de.catma.CatmaApplication;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
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
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.ui.analyzer.querybuilder.QueryBuilderWizardFactory;
import de.catma.ui.analyzer.querybuilder.TagsetDefinitionDictionary;
import de.catma.ui.repository.MarkupCollectionItem;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabComponent;
import de.catma.util.Equal;

public class AnalyzerView extends VerticalLayout 
implements ClosableTab, TabComponent, GroupedQueryResultSelectionListener, RelevantUserMarkupCollectionProvider {
	
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
	private TagsetDefinitionDictionary tagsetDefinitionDictionary;
	private IndexInfoSet indexInfoSet;
	private boolean init = false;
	private Label helpLabel;
	
	public AnalyzerView(
			Corpus corpus, IndexedRepository repository, 
			CloseListener closeListener) {
		
		this.corpus = corpus;
		this.closeListener = closeListener;
		this.relevantSourceDocumentIDs = new ArrayList<String>();
		this.relevantUserMarkupCollIDs = new ArrayList<String>();
		this.relevantStaticMarkupCollIDs = new ArrayList<String>();
		tagsetDefinitionDictionary = new TagsetDefinitionDictionary();
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
		if (!init) {
			init = true;
			helpLabel.setIcon(new ClassResource(
					"ui/resources/icon-help.gif", 
					getApplication()));
		}
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
							tagsetDefinitionDictionary.clear();
							removeSourceDocumentFromTree(sourceDocument);
							if (relevantSourceDocumentIDs.isEmpty()) {
								closeListener.closeRequest(AnalyzerView.this);
							}
						}
					}
				}
				else { //update
					tagsetDefinitionDictionary.clear();
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
					if (relevantUserMarkupCollIDs.contains(
							userMarkupCollectionReference.getId())) {
						tagsetDefinitionDictionary.clear();

						removeUserMarkupCollectionFromTree(
								userMarkupCollectionReference);
					}
				}
				else { // update
					tagsetDefinitionDictionary.clear();
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
							tagsetDefinitionDictionary.clear();
							addSourceDocument((SourceDocument)evt.getOldValue());
						}
						// update usermarkupcoll added
						else if (evt.getOldValue() 
								instanceof UserMarkupCollectionReference) {
							tagsetDefinitionDictionary.clear();
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
					tagsetDefinitionDictionary);
		
		Window wizardWindow = 
				factory.createWizardWindow("Query Builder", "90%", "85%");
		
		getApplication().getMainWindow().addWindow(wizardWindow);
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
				if (t instanceof QueryException) {
					QueryJob.QueryException qe = (QueryJob.QueryException)t;
		            String input = qe.getInput();
		            int idx = ((RecognitionException)qe.getCause()).charPositionInLine;
		            if ((idx >=0) && (input.length() > idx)) {
		                char character = input.charAt(idx);
		            	String message = MessageFormat.format(
		            		"<html><p>There is something wrong with your query <b>{0}</b> approximately at positon {1} character <b>{2}</b>.</p> <p>If you are unsure about how to construct a query try the Query Builder!</p></html>",
		            		input,
	                        idx+1,
	                        character);
		            	getWindow().showNotification("Information", message, Notification.TYPE_TRAY_NOTIFICATION);
		            }
		            else {
		            	String message = MessageFormat.format(
		            			"<html><p>There is something wrong with your query <b>{0}</b>.</p> <p>If you are unsure about how to construct a query try the Query Builder!</p></html>",
			            		input);
		            	getWindow().showNotification("Information", message, Notification.TYPE_TRAY_NOTIFICATION);
		            }
				}
				else {
					((CatmaApplication)getApplication()).showAndLogError(
						"Error during search!", t);
				}
			}
		});
	}
	
	public void resultsSelected(GroupedQueryResultSet groupedQueryResultSet) {
		try {
			handleDistributionChartRequest(groupedQueryResultSet);
		} catch (IOException e) {
			((CatmaApplication)getApplication()).showAndLogError(
				"Error showing the distribution chart",
				e);
		}
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
		markupResultPanel = new MarkupResultPanel(repository, this, this);
		return markupResultPanel;
	}

	private Component createResultByPhraseView() {
		phraseResultPanel = new PhraseResultPanel(repository, this, this);
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
		
		btQueryBuilder = new Button("Query Builder");
		convenienceButtonPanel.addComponent(btQueryBuilder);
		
		btWordList = new Button("Wordlist");
		convenienceButtonPanel.addComponent(btWordList);
		
		helpLabel = new Label();
		helpLabel.setWidth("20px");
		helpLabel.setDescription(
				"<h3>Hints</h3>" +
				"<h4>Using the wordlist</h4>" +
				"Click on the  \"Wordlist\"-Button to get a list of all words of your document together with their frequencies." +
				" You can now sort the list by phrase, i. e. the word, or by frequency." +
				"<h4>Building queries</h4>" +
				"You are free to hack your query directly into the Query box, but a large part of all possible queries can be generated with the Query Builder more conveniently." +
				"<h4>Keywords in Context (KWIC)</h4>" +
				"To see your search results in the context of its surrounding text, tick the \"Visible in Kwic\"-check box " +
				"of the desired results." +
				"<h4>Results by Markup</h4>" +
				"When building Tag Queries where you look for occurrences of certain Tags, sometimes you " +
				"want the results grouped by Tags (especially Subtags) and sometimes you want the results " +
				"grouped by the tagged phrase. The \"Results by markup\" and \"Results by phrase\" tabs give you this choice for Tag Queries.");

		convenienceButtonPanel.addComponent(helpLabel);
	
		return convenienceButtonPanel;
	}

	private Component createSearchPanel() {
		HorizontalLayout searchPanel = new HorizontalLayout();
		searchPanel.setSpacing(true);
		searchPanel.setWidth("100%");
		
		searchInput = new TextField();
		searchInput.setCaption("Query");
		searchInput.setWidth("100%");
		searchInput.setImmediate(true);
		
		searchPanel.addComponent(searchInput);
		searchPanel.setExpandRatio(searchInput, 1.0f);
		
		btExecSearch = new Button("Execute Query");
		
		searchPanel.addComponent(btExecSearch);
		searchPanel.setComponentAlignment(btExecSearch, Alignment.BOTTOM_CENTER);
		
		return searchPanel;
	}

	private void handleDistributionChartRequest(
			GroupedQueryResultSet groupedQueryResultSet) throws IOException {

		DistributionComputation dc = new DistributionComputation(
				groupedQueryResultSet, repository, relevantSourceDocumentIDs);
		dc.compute();
		
		this.visualizationId = 
			((CatmaApplication)getApplication()).addVisualization(
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
}
