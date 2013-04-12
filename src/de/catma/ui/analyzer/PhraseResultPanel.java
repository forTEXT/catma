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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.AccumulativeGroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class PhraseResultPanel extends VerticalLayout {
	
	private static enum TreePropertyName {
		caption,
		frequency, 
		visibleInKwic,
		;
	}
	
	private TreeTable resultTable;
	private Repository repository;
	private KwicPanel kwicPanel;
	private GroupedQueryResultSelectionListener resultSelectionListener;
	private Button btDist;
	private boolean init = false;
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;
	private Button btSelectAll;
	private Button btDeselectAll;
	private Button btDoubleTree;

	public PhraseResultPanel(
			Repository repository, 
			GroupedQueryResultSelectionListener resultSelectionListener, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider) {
		this.repository = repository;
		this.resultSelectionListener = resultSelectionListener;
		this.relevantUserMarkupCollectionProvider = relevantUserMarkupCollectionProvider;
	}
	
	@Override
	public void attach() {
		super.attach();
		if (!init) {
			initComponents();
			initActions();
			init = true;
		}
	}

	private void initActions() {
		btDoubleTree.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Set<?> selection = (Set<?>) resultTable.getValue();
				
				try {
					if (selection.size() == 1) {
						GroupedQueryResult result = extractGroupedQueryResult(selection.iterator().next());
						List<KeywordInContext> kwics = new ArrayList<KeywordInContext>();
						for (QueryResultRow row : result) {
						
							SourceDocument sourceDocument = 
									repository.getSourceDocument(row.getSourceDocumentId());
								
							KwicProvider kwicProvider = new KwicProvider(sourceDocument);
							KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), 5);
							kwics.add(kwic);
						}	
						((CatmaApplication)getApplication()).addDoubleTree(kwics);
					}					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		
		btDist.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				GroupedQueryResultSet set = new GroupedQueryResultSet();

				Set<?> selection = (Set<?>) resultTable.getValue();
				
				if (selection.size() > 1) {
					AccumulativeGroupedQueryResult accResult =
							new AccumulativeGroupedQueryResult();
					
					for (Object value : selection) {
						accResult.addGroupedQueryResult(extractGroupedQueryResult(value));
					}
					
					set.add(accResult);
				}
				else if (selection.size() == 1) {
					Object value = selection.iterator().next();
					set.add(extractGroupedQueryResult(value));
				}
				
				if (selection.size() > 0) {
					resultSelectionListener.resultsSelected(set);
				}
				else {
					getWindow().showNotification(
							"Information", "Please select one or more result rows!",
							Notification.TYPE_TRAY_NOTIFICATION);
				}
			}


		});
		btSelectAll.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				selectAllForKwic(true);
			}
		});
		btDeselectAll.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				selectAllForKwic(false);
			}
		});
	}
	
	private void selectAllForKwic(boolean selected) {
		for (Object o : resultTable.getItemIds()) {
			if (resultTable.getParent(o) == null) {
				CheckBox cbVisibleInKwic = 
					(CheckBox) resultTable.getItem(o).getItemProperty(
						TreePropertyName.visibleInKwic).getValue();
				cbVisibleInKwic.setValue(selected);
			}
		}
	}

	private GroupedQueryResult extractGroupedQueryResult(Object value) {
		if (value instanceof SourceDocumentItemID) {
			GroupedQueryResult parentResult = (GroupedQueryResult)resultTable.getParent(value);
			return parentResult.getSubResult(
					((SourceDocumentItemID)value).getSourceDocumentID());
		}
		else {
			return (GroupedQueryResult)value;
		}
	}

	private void initComponents() {
		setSizeFull();
		
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setSizeFull();
		VerticalLayout leftComponent = new VerticalLayout();
		leftComponent.setSpacing(true);
		leftComponent.setSizeFull();
		
		resultTable = new TreeTable();
		resultTable.setSelectable(true);
		resultTable.setMultiSelect(true);
		HierarchicalContainer container = new HierarchicalContainer();
		container.setItemSorter(
				new PropertyDependentItemSorter(
						TreePropertyName.caption, 
						new PropertyToTrimmedStringCIComparator()));
		
		resultTable.setContainerDataSource(container);
		
		resultTable.addContainerProperty(
				TreePropertyName.caption, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.caption, "Phrase");
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, "Frequency");
		resultTable.addContainerProperty(
				TreePropertyName.visibleInKwic, AbstractComponent.class, null);
		resultTable.setColumnHeader(TreePropertyName.visibleInKwic, "Visible in Kwic");
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		resultTable.setPageLength(10); //TODO: config
		resultTable.setSizeFull();
		
		leftComponent.addComponent(resultTable);
		leftComponent.setExpandRatio(resultTable, 1.0f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		
		btDist = new Button();
		btDist.setIcon(new ClassResource(
				"ui/analyzer/resources/chart.gif", 
				getApplication()));
		buttonPanel.addComponent(btDist);
		
		btDoubleTree = new Button("DT");
		buttonPanel.addComponent(btDoubleTree);
		
		btSelectAll = new Button("Select all for Kwic");
		
		buttonPanel.addComponent(btSelectAll);
		buttonPanel.setComponentAlignment(btSelectAll, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(btSelectAll, 1f);
		btDeselectAll = new Button("Deselect all for Kwic");
		buttonPanel.addComponent(btDeselectAll);
		buttonPanel.setComponentAlignment(btDeselectAll, Alignment.MIDDLE_RIGHT);
		
		leftComponent.addComponent(buttonPanel);
		splitPanel.addComponent(leftComponent);
		
		VerticalLayout rightComponent = new VerticalLayout();
		rightComponent.setSpacing(true);
		rightComponent.setSizeFull();
		
		this.kwicPanel = new KwicPanel(repository, relevantUserMarkupCollectionProvider);
		rightComponent.addComponent(kwicPanel);
		rightComponent.setExpandRatio(kwicPanel, 1f);

		Label helpLabel = new Label();
		helpLabel.setIcon(new ClassResource(
				"ui/resources/icon-help.gif", 
				getApplication()));
		
		helpLabel.setDescription(
				"<h3>Hints</h3>" +
				"<h4>Tagging search results</h4>" +
				"You can tag the search results in the Kwic-view: " +
				"<p>First select one or more rows and then drag the desired " +
				"Tag from the Tag Manager over the Kwic-results.</p>" +
				"<h4>Take a closer look</h4>" +
				"You can jump to the location in the full text by double " +
				"clicking on a row in the Kwic-view." +
				"<h4>Untag search results</h4>" +
				"The \"Results by markup\" tab gives you the opportunity " +
				"to untag markup for selected search results in the Kwic-view.");
		
		rightComponent.addComponent(helpLabel);
		rightComponent.setComponentAlignment(helpLabel, Alignment.TOP_RIGHT);
		
		splitPanel.addComponent(rightComponent);
		addComponent(splitPanel);
	}
	
	public void setQueryResult(QueryResult queryResult) {
		kwicPanel.clear();
		resultTable.removeAllItems();
		int totalCount = 0;
		int totalFreq = 0;
		
		for (GroupedQueryResult phraseResult : 
				queryResult.asGroupedSet()) {
			addPhraseResult(phraseResult);
			totalFreq+=phraseResult.getTotalFrequency();
			totalCount++;
		}
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, "Total count: " + totalCount);
		resultTable.setColumnFooter(
				TreePropertyName.frequency, "Total frequency: " + totalFreq);
	}

	private void addPhraseResult(GroupedQueryResult phraseResult) {
		resultTable.addItem( 
				new Object[] {
					phraseResult.getGroup(), 
					phraseResult.getTotalFrequency(),
					createKwicCheckbox(phraseResult) 
				},
				phraseResult);

		resultTable.getContainerProperty(
			phraseResult, TreePropertyName.caption).setValue(
					phraseResult.getGroup());

		for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(sourceDocumentID);
			SourceDocumentItemID sourceDocumentItemID = 
					new SourceDocumentItemID(
							phraseResult.getGroup() 
								+ "@" + sourceDocument, 
							sourceDocumentID);
			
			resultTable.addItem(sourceDocumentItemID);
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.frequency).setValue(
							phraseResult.getFrequency(sourceDocumentID));
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.caption).setValue(
							sourceDocument.toString());
			resultTable.setParent(sourceDocumentItemID, phraseResult);
			
			resultTable.setChildrenAllowed(sourceDocumentItemID, false);
		}
		
	}

	private CheckBox createKwicCheckbox(final GroupedQueryResult phraseResult) {
		final CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				boolean selected = 
						(Boolean)event.getProperty().getValue();

				fireShowInKwicViewSelected(phraseResult, selected);
			}


		});
		return cbShowInKwicView;
	}
	
	private void fireShowInKwicViewSelected(GroupedQueryResult phraseResult,
			boolean selected) {

		if (selected) {
			try {
				kwicPanel.addQueryResultRows(phraseResult);
			} catch (IOException e) {
				((CatmaApplication)getApplication()).showAndLogError(
					"Error showing KWIC results!", e);
			}
		}
		else {
			kwicPanel.removeQueryResultRows(phraseResult);
		}
		
	}
	
}
