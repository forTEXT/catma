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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItemClickEvent;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItemClickListener;

import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ClassResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.AccumulativeGroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.CatmaApplication;
import de.catma.ui.Slider;
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
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;
	private Button btSelectAll;
	private Button btDeselectAll;
	private Button btTagResults;
	private Button btDoubleTree;
	private Button btExcelExport;
	private Button btKwicExcelExport;
	private Button btKwicCsvExport;
	private Button btCsvExport;
	private Button btHelp;
	private Table hiddenFlatTable;
	private Button btSelectAllKwic;
	private Slider kwicSizeSlider;	
	
	PhraseResultHelpWindow phraseResultHelpWindow = new PhraseResultHelpWindow();
	private TagKwicResultsProvider tagKwicResultsProvider;
	private Button btSelectAllRows;
	private Button btDeselectAllRows;
	
	public PhraseResultPanel(
			Repository repository, 
			GroupedQueryResultSelectionListener resultSelectionListener, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider,
			TagKwicResultsProvider tagKwicResultsProvider) {
		this.repository = repository;
		
		this.resultSelectionListener = resultSelectionListener;
		this.relevantUserMarkupCollectionProvider = relevantUserMarkupCollectionProvider;
		this.tagKwicResultsProvider = tagKwicResultsProvider;
		initComponents();
		initActions();
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
							Messages.getString("PhraseResultPanel.infoTitle"),  //$NON-NLS-1$
							Messages.getString("PhraseResultPanel.phraseSelectionHint"),  //$NON-NLS-1$
							Type.TRAY_NOTIFICATION);
					}
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("PhraseResultPanel.errorKwicInDoubleTree"), e); //$NON-NLS-1$
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
							Messages.getString("PhraseResultPanel.infoTitle"), Messages.getString("PhraseResultPanel.rowSelectionHint"), //$NON-NLS-1$ //$NON-NLS-2$
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
		
		kwicPanel.addTagResultsContextMenuClickListener(new ContextMenuItemClickListener() {
			
			@Override
			public void contextMenuItemClicked(ContextMenuItemClickEvent event) {
				tagResults();
			}
		});
		
		btTagResults.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				tagResults();
			}
		});
		
		btKwicExcelExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
            	try {
					ExcelExport excelExport = 
							new HierarchicalExcelExport(kwicPanel.getKwicTable(), 
									Messages.getString("PhraseResultPanel.kwicQueryResults")); //$NON-NLS-1$
					excelExport.excludeCollapsedColumns();
					excelExport.setReportTitle(Messages.getString("PhraseResultPanel.kwicQueryResults")); //$NON-NLS-1$
					excelExport.export();
				} catch (IllegalArgumentException e) {
					HTMLNotification.show(
						Messages.getString("PhraseResultPanel.error"),  //$NON-NLS-1$
						MessageFormat.format(Messages.getString("PhraseResultPanel.excelExportError"), e.getMessage()),  //$NON-NLS-1$
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
							Messages.getString("PhraseResultPanel.csvExportError"), e); //$NON-NLS-1$
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
	            			new HierarchicalExcelExport(resultTable, Messages.getString("PhraseResultPanel.queryResults")); //$NON-NLS-1$
	                excelExport.excludeCollapsedColumns();
	                excelExport.setReportTitle(Messages.getString("PhraseResultPanel.queryResults")); //$NON-NLS-1$
	                excelExport.export();
				} catch (IllegalArgumentException e) {
					HTMLNotification.show(
						Messages.getString("PhraseResultPanel.error"),  //$NON-NLS-1$
						MessageFormat.format(Messages.getString("PhraseResultPanel.excelExportError"), e.getMessage()),  //$NON-NLS-1$
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
							Messages.getString("PhraseResultPanel.csvExportError"), e); //$NON-NLS-1$
				}
			}
		});
		
		btHelp.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if(phraseResultHelpWindow.getParent() == null){
					UI.getCurrent().addWindow(phraseResultHelpWindow);
				} else {
					UI.getCurrent().removeWindow(phraseResultHelpWindow);
				}
								
			}
		});
		
		kwicSizeSlider.addValueListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				try {
					Double kwicSize = (Double) event.getProperty().getValue();
					kwicPanel.setKwicSize(kwicSize.intValue());
				}
				catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("PhraseResultPanel.kwicSizeError"), e); //$NON-NLS-1$
				}
			}
		});
		
		btSelectAllRows.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				resultTable.setValue(resultTable.getItemIds());
			}
		});
		
		btDeselectAllRows.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				resultTable.setValue(null);
			}
		});

	}
	
	@SuppressWarnings("unchecked")
	private Table fillFlatTable() {
		hiddenFlatTable.removeAllItems();
		
		hiddenFlatTable.addContainerProperty(
				Messages.getString("PhraseResultPanel.sourceDocument"), String.class, null); //$NON-NLS-1$
		hiddenFlatTable.addContainerProperty(
				Messages.getString("PhraseResultPanel.phrase"), String.class, null);		 //$NON-NLS-1$
		hiddenFlatTable.addContainerProperty(
				Messages.getString("PhraseResultPanel.frequency"), Integer.class, null); //$NON-NLS-1$
		
		for (Object itemId : resultTable.getItemIds()) {
			if (itemId instanceof GroupedQueryResult) {
				GroupedQueryResult result= (GroupedQueryResult)itemId;
				for (String sourceDocumentID : result.getSourceDocumentIDs()) {
					SourceDocument sourceDocument = 
							repository.getSourceDocument(sourceDocumentID);
					
					Item curItem = hiddenFlatTable.addItem(result+sourceDocumentID);
					curItem.getItemProperty(Messages.getString("PhraseResultPanel.sourceDocument")).setValue(sourceDocument.toString()); //$NON-NLS-1$
					curItem.getItemProperty(Messages.getString("PhraseResultPanel.phrase")).setValue(result.getGroup().toString()); //$NON-NLS-1$
					curItem.getItemProperty(Messages.getString("PhraseResultPanel.frequency")).setValue(result.getFrequency(sourceDocumentID)); //$NON-NLS-1$
				}
			}
		}

		return hiddenFlatTable;
	}
	
	private void tagResults() {
		if (kwicPanel.getSelection().isEmpty()) {
			Notification.show(Messages.getString("PhraseResultPanel.infoTitle"), Messages.getString("PhraseResultPanel.kwicSelectionHint"), Type.TRAY_NOTIFICATION); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			tagKwicResultsProvider.tagResults();
		}
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
		leftComponent.addStyleName("analyzer-panel-padding"); //$NON-NLS-1$
		
		resultTable = new TreeTable();
		resultTable.setSelectable(true);
		resultTable.setMultiSelect(true);
		HierarchicalContainer container = createContainer();
		
		resultTable.setContainerDataSource(container);
		
		resultTable.setColumnHeader(TreePropertyName.caption, Messages.getString("PhraseResultPanel.phrase")); //$NON-NLS-1$
		resultTable.setColumnHeader(TreePropertyName.frequency, Messages.getString("PhraseResultPanel.frequency")); //$NON-NLS-1$
		resultTable.setColumnHeader(TreePropertyName.visibleInKwic, Messages.getString("PhraseResultPanel.visibleInKwic")); //$NON-NLS-1$
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		resultTable.setSizeFull();
		
		leftComponent.addComponent(resultTable);
		leftComponent.setExpandRatio(resultTable, 1.0f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%"); //$NON-NLS-1$
		
		btDist = new Button();
		btDist.setIcon(new ClassResource("analyzer/resources/chart.gif")); //$NON-NLS-1$
		btDist.setDescription(
			Messages.getString("PhraseResultPanel.showPhraseInDistChart")); //$NON-NLS-1$
		
		buttonPanel.addComponent(btDist);
		
		btDoubleTree = new Button();
		btDoubleTree.setIcon(new ClassResource("analyzer/resources/doubletree.gif")); //$NON-NLS-1$
		btDoubleTree.setDescription(
			Messages.getString("PhraseResultPanel.showPhraseInDoubleTree")); //$NON-NLS-1$
		
		buttonPanel.addComponent(btDoubleTree);
		
		btExcelExport = new Button();
		btExcelExport.setIcon(new ClassResource("analyzer/resources/excel.png")); //$NON-NLS-1$
		btExcelExport.setDescription(Messages.getString("PhraseResultPanel.exportQueryResultsToExcel")); //$NON-NLS-1$
		buttonPanel.addComponent(btExcelExport);
		
		
		btCsvExport = new Button();
		btCsvExport.setIcon(new ClassResource(
				"analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text //$NON-NLS-1$
		btCsvExport.setDescription(
				Messages.getString("PhraseResultPanel.exportQueryResultsToCSV")); //$NON-NLS-1$
		buttonPanel.addComponent(btCsvExport);
		
		btSelectAllRows = new Button(Messages.getString("PhraseResultPanel.selectAll")); //$NON-NLS-1$
		buttonPanel.addComponent(btSelectAllRows);
		buttonPanel.setComponentAlignment(btSelectAllRows, Alignment.MIDDLE_RIGHT);
		
		btDeselectAllRows = new Button(Messages.getString("PhraseResultPanel.deselectAll")); //$NON-NLS-1$
		buttonPanel.addComponent(btDeselectAllRows);
		buttonPanel.setComponentAlignment(btDeselectAllRows, Alignment.MIDDLE_RIGHT);

		
		btSelectAll = new Button(Messages.getString("PhraseResultPanel.selectAllKwic")); //$NON-NLS-1$
		buttonPanel.addComponent(btSelectAll);
		buttonPanel.setComponentAlignment(btSelectAll, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(btSelectAll, 1f);

		btDeselectAll = new Button(Messages.getString("PhraseResultPanel.deselectAllKwic")); //$NON-NLS-1$
		buttonPanel.addComponent(btDeselectAll);
		buttonPanel.setComponentAlignment(btDeselectAll, Alignment.MIDDLE_RIGHT);
		
		leftComponent.addComponent(buttonPanel);
		splitPanel.addComponent(leftComponent);
		
		VerticalLayout rightComponent = new VerticalLayout();
		rightComponent.setSpacing(true);
		rightComponent.setSizeFull();
		rightComponent.addStyleName("analyzer-panel-padding"); //$NON-NLS-1$
		
		this.kwicPanel = new KwicPanel(repository, relevantUserMarkupCollectionProvider);
		rightComponent.addComponent(kwicPanel);
		rightComponent.setExpandRatio(kwicPanel, 1f);

		HorizontalLayout kwicButtonPanel = new HorizontalLayout();
		kwicButtonPanel.setSpacing(true);
		kwicButtonPanel.setWidth("100%"); //$NON-NLS-1$
		kwicButtonPanel.setStyleName("help-padding-fix"); //$NON-NLS-1$
		
		btKwicExcelExport = new Button();
		btKwicExcelExport.setIcon(new ClassResource("analyzer/resources/excel.png")); //$NON-NLS-1$
		btKwicExcelExport.setDescription(
				Messages.getString("PhraseResultPanel.exportQueryResultsToExcel")); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btKwicExcelExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicExcelExport, Alignment.MIDDLE_LEFT);
		
		btKwicCsvExport = new Button();
		btKwicCsvExport.setIcon(new ClassResource(
				"analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text //$NON-NLS-1$
		btKwicCsvExport.setDescription(
				Messages.getString("PhraseResultPanel.exportQueryResultsToCSV")); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btKwicCsvExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicCsvExport, Alignment.MIDDLE_LEFT);

		kwicSizeSlider = new Slider(null, 1, 30, Messages.getString("PhraseResultPanel.tokenContext")); //$NON-NLS-1$
		kwicSizeSlider.setValue(5.0);
		kwicButtonPanel.addComponent(kwicSizeSlider);
		kwicButtonPanel.setComponentAlignment(
				kwicSizeSlider, Alignment.MIDDLE_LEFT);
		
		btTagResults = new Button(Messages.getString("PhraseResultPanel.tagSelectedResults")); //$NON-NLS-1$
		btTagResults.addStyleName("primary-button"); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btTagResults);
		kwicButtonPanel.setComponentAlignment(btTagResults, Alignment.MIDDLE_RIGHT);		
		kwicButtonPanel.setExpandRatio(btTagResults, 1f);
		
		btSelectAllKwic = new Button(Messages.getString("PhraseResultPanel.selectAll")); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btSelectAllKwic);
		kwicButtonPanel.setComponentAlignment(btSelectAllKwic, Alignment.MIDDLE_RIGHT);		
		
		btHelp = new Button(FontAwesome.QUESTION_CIRCLE);
//		btHelp.addStyleName("icon-button"); // for top-margin
		btHelp.addStyleName("help-button"); //$NON-NLS-1$
		
		kwicButtonPanel.addComponent(btHelp);
		kwicButtonPanel.setComponentAlignment(btHelp, Alignment.MIDDLE_RIGHT);
		
		rightComponent.addComponent(kwicButtonPanel);
		rightComponent.setComponentAlignment(kwicButtonPanel, Alignment.MIDDLE_RIGHT);
		
		hiddenFlatTable = new Table();
		hiddenFlatTable.setVisible(false);
		kwicButtonPanel.addComponent(hiddenFlatTable);
		
		splitPanel.addComponent(rightComponent);
		addComponent(splitPanel);
	}
	
	private HierarchicalContainer createContainer() {
		HierarchicalContainer container = new HierarchicalContainer();
		container.setItemSorter(
				new PropertyDependentItemSorter(
						TreePropertyName.caption, 
						new PropertyToTrimmedStringCIComparator()));
		container.addContainerProperty(
				TreePropertyName.caption, String.class, null);
		
		container.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		
		container.addContainerProperty(
				TreePropertyName.visibleInKwic, AbstractComponent.class, null);

		
		return container;
	}
	
	public void setQueryResult(QueryResult queryResult) {
		kwicPanel.clear();
		HierarchicalContainer container = createContainer();
		
		int totalCount = 0;
		int totalFreq = 0;

		for (GroupedQueryResult phraseResult : queryResult.asGroupedSet()) { 
			addPhraseResult(phraseResult, container);
			totalFreq+=phraseResult.getTotalFrequency();
			totalCount++;
		}
		
		resultTable.setContainerDataSource(container);
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, MessageFormat.format(Messages.getString("PhraseResultPanel.totalCount"), totalCount)); //$NON-NLS-1$
		resultTable.setColumnFooter(
				TreePropertyName.frequency, MessageFormat.format(Messages.getString("PhraseResultPanel.totalFrequency"), totalFreq)); //$NON-NLS-1$
	}

	
	@SuppressWarnings("unchecked")
	private void addPhraseResult(GroupedQueryResult phraseResult,
			HierarchicalContainer container) {
		Item phraseResultItem = container.addItem(phraseResult);
		phraseResultItem.getItemProperty(TreePropertyName.caption).setValue(
						phraseResult.getGroup().toString());
		phraseResultItem.getItemProperty(TreePropertyName.frequency).setValue(
				phraseResult.getTotalFrequency());
		phraseResultItem.getItemProperty(TreePropertyName.visibleInKwic).setValue(
				createKwicCheckbox(phraseResult));
		
		for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
			SourceDocument sourceDocument = 
					repository.getSourceDocument(sourceDocumentID);
			
			SourceDocumentItemID sourceDocumentItemID = 
					new SourceDocumentItemID(
							phraseResult.getGroup() 
								+ "@" + sourceDocument,  //$NON-NLS-1$
							sourceDocumentID);

			container.addItem(sourceDocumentItemID);

			container.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.frequency).setValue(
							phraseResult.getFrequency(sourceDocumentID));
			
			container.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.caption).setValue(
							sourceDocument.toString());

			container.setParent(sourceDocumentItemID, phraseResult);
			
			container.setChildrenAllowed(sourceDocumentItemID, false);
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
					Messages.getString("PhraseResultPanel.errorShowingKwicResults"), e); //$NON-NLS-1$
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
