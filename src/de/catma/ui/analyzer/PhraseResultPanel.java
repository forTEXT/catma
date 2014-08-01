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

import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ClassResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

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
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.component.export.CsvExport;
import de.catma.ui.component.export.CsvExport.CsvExportException;
import de.catma.ui.component.export.HierarchicalExcelExport;
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
	private Button btExcelExport;
	private Button btKwicExcelExport;
	private Button btKwicCsvExport;
	private Button btCsvExport;
	private Table hiddenFlatTable;
	private Button btSelectAllKwic;
	
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
		btDoubleTree.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Set<?> selection = (Set<?>) resultTable.getValue();
				
				try {
					if (selection.size() == 1) {
						GroupedQueryResult result = 
								extractGroupedQueryResult(selection.iterator().next());
						List<KeywordInContext> kwics = new ArrayList<KeywordInContext>();
						for (QueryResultRow row : result) {
						
							SourceDocument sourceDocument = 
									repository.getSourceDocument(row.getSourceDocumentId());
								
							KwicProvider kwicProvider = new KwicProvider(sourceDocument);
							KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), 5);
							kwics.add(kwic);
						}	
						((CatmaApplication)UI.getCurrent()).addDoubleTree(kwics);
					}		
					else {
						Notification.show(
							"Information", 
							"Please select exactly one phrase!", 
							Type.TRAY_NOTIFICATION);
					}
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"error while preparing kwic in doubletree visualization", e);
				}
				
			}
		});
		
		btDist.addClickListener(new ClickListener() {
			
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
					Notification.show(
							"Information", "Please select one or more result rows!",
							Type.TRAY_NOTIFICATION);
				}
			}


		});
		btSelectAll.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				selectAllForKwic(true);
			}
		});
		btDeselectAll.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				selectAllForKwic(false);
			}
		});
		
		btKwicExcelExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
            	try {
					ExcelExport excelExport = 
							new HierarchicalExcelExport(kwicPanel.getKwicTable(), 
									"CATMA Query Result Kwic");
					excelExport.excludeCollapsedColumns();
					excelExport.setReportTitle("CATMA Query Result Kwic");
					excelExport.export();
				} catch (IllegalArgumentException e) {
					HTMLNotification.show(
						"Error", 
						"Excel export failed. " + "<br>" + "Reason: " 
						+ e.getMessage() + "<br>" + "Please use CSV export.", 
						Type.WARNING_MESSAGE);
					
					e.printStackTrace();
				}
			}
		});
		
		btKwicCsvExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {         
				try {
					CsvExport csvExport = new CsvExport(kwicPanel.getKwicTable());
					csvExport.convertTable();
					csvExport.sendConverted();
				}
				catch (CsvExportException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error creating CSV export!", e);
				}
			}
		});

		
		btSelectAllKwic.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				kwicPanel.selectAll();
			}
		});
		
		btExcelExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try{
	            	ExcelExport excelExport = 
	            			new HierarchicalExcelExport(resultTable, "CATMA Query Result");
	                excelExport.excludeCollapsedColumns();
	                excelExport.setReportTitle("CATMA Query Result");
	                excelExport.export();
				} catch (IllegalArgumentException e) {
					HTMLNotification.show(
						"Error", 
						"Excel export failed. " + "<br>" + "Reason: " 
						+ e.getMessage() + "<br>" + "Please use CSV export.", 
						Type.WARNING_MESSAGE);
					
					e.printStackTrace();
				}	                
			}
		});
		
		btCsvExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					CsvExport csvExport = new CsvExport(fillFlatTable());
					csvExport.convertTable();
					csvExport.sendConverted();
				}
				catch (CsvExportException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error creating CSV export!", e);
				}
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private Table fillFlatTable() {
		hiddenFlatTable.removeAllItems();
		
		hiddenFlatTable.addContainerProperty(
				"source document", String.class, null);
		hiddenFlatTable.addContainerProperty(
				"phrase", String.class, null);		
		hiddenFlatTable.addContainerProperty(
				"frequency", Integer.class, null);
		
		for (Object itemId : resultTable.getItemIds()) {
			if (itemId instanceof GroupedQueryResult) {
				GroupedQueryResult result= (GroupedQueryResult)itemId;
				for (String sourceDocumentID : result.getSourceDocumentIDs()) {
					SourceDocument sourceDocument = 
							repository.getSourceDocument(sourceDocumentID);
					
					Item curItem = hiddenFlatTable.addItem(result+sourceDocumentID);
					curItem.getItemProperty("source document").setValue(sourceDocument.toString());
					curItem.getItemProperty("phrase").setValue(result.getGroup().toString());
					curItem.getItemProperty("frequency").setValue(result.getFrequency(sourceDocumentID));
				}
			}
		}

		return hiddenFlatTable;
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
		btDist.setIcon(new ClassResource("ui/analyzer/resources/chart.gif"));
		btDist.setDescription(
			"Show selected phrases as a distribution trend in a " +
			"chart like visualization.");
		
		buttonPanel.addComponent(btDist);
		
		btDoubleTree = new Button();
		btDoubleTree.setIcon(new ClassResource("ui/analyzer/resources/doubletree.gif"));
		btDoubleTree.setDescription(
			"Show a selected phrase with a doubletree kwic visualization.");
		
		buttonPanel.addComponent(btDoubleTree);
		
		btExcelExport = new Button();
		btExcelExport.setIcon(new ClassResource("ui/analyzer/resources/excel.png"));
		btExcelExport.setDescription("Export all Query result data as an Excel spreadsheet.");
		buttonPanel.addComponent(btExcelExport);
		
		
		btCsvExport = new Button();
		btCsvExport.setIcon(new ClassResource(
				"ui/analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text
		btCsvExport.setDescription(
				"Export all Query result data as flat CSV File.");
		buttonPanel.addComponent(btCsvExport);
		
		
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

		HorizontalLayout kwicButtonPanel = new HorizontalLayout();
		kwicButtonPanel.setSpacing(true);
		kwicButtonPanel.setWidth("100%");
		
		btKwicExcelExport = new Button();
		btKwicExcelExport.setIcon(new ClassResource("ui/analyzer/resources/excel.png"));
		btKwicExcelExport.setDescription(
				"Export all Query result data as an Excel spreadsheet.");
		kwicButtonPanel.addComponent(btKwicExcelExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicExcelExport, Alignment.MIDDLE_LEFT);
		
		btKwicCsvExport = new Button();
		btKwicCsvExport.setIcon(new ClassResource(
				"ui/analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text
		btKwicCsvExport.setDescription(
				"Export all Query result data as CSV File.");
		kwicButtonPanel.addComponent(btKwicCsvExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicCsvExport, Alignment.MIDDLE_LEFT);

		
		btSelectAllKwic = new Button("Select all");
		kwicButtonPanel.addComponent(btSelectAllKwic);
		kwicButtonPanel.setComponentAlignment(btSelectAllKwic, Alignment.MIDDLE_RIGHT);
		kwicButtonPanel.setExpandRatio(btSelectAllKwic, 1f);
		
		Label helpLabel = new Label();

		helpLabel.setIcon(new ClassResource("ui/resources/icon-help.gif"));
		
		helpLabel.setWidth("20px");
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
		
		kwicButtonPanel.addComponent(helpLabel);
		kwicButtonPanel.setComponentAlignment(helpLabel, Alignment.MIDDLE_RIGHT);
		
		rightComponent.addComponent(kwicButtonPanel);
		rightComponent.setComponentAlignment(kwicButtonPanel, Alignment.MIDDLE_RIGHT);
		
		hiddenFlatTable = new Table();
		hiddenFlatTable.setVisible(false);
		kwicButtonPanel.addComponent(hiddenFlatTable);
		
		splitPanel.addComponent(rightComponent);
		addComponent(splitPanel);
	}
	
	public void setQueryResult(QueryResult queryResult) {
		kwicPanel.clear();
		resultTable.removeAllItems();
		int totalCount = 0;
		int totalFreq = 0;
		
		for (GroupedQueryResult phraseResult : //hat eine methode filter; muss die ganze QuerryResult durchgehen; es gibt die methode getQueryResult
				queryResult.asGroupedSet()) { //holt sich den Rückgabewert von der methode asGroupedSet
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

	@SuppressWarnings("unchecked")
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
					phraseResult.getGroup().toString());

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
		cbShowInKwicView.addValueChangeListener(new ValueChangeListener() {
			
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
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error showing KWIC results!", e);
			}
		}
		else {
			kwicPanel.removeQueryResultRows(phraseResult);
		}
		
	}

	public boolean isEmpty() {
		return resultTable.getItemIds().isEmpty();
	}
}
