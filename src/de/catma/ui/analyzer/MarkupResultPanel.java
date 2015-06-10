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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ClassResource;
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
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.queryengine.result.AccumulativeGroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.CatmaApplication;
import de.catma.ui.Slider;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.component.export.CsvExport;
import de.catma.ui.component.export.CsvExport.CsvExportException;
import de.catma.ui.component.export.HierarchicalExcelExport;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class MarkupResultPanel extends VerticalLayout {
	
	/**
	 * A TagQueryResultRow can appear more than once (if tagged with different
	 * tags for example). As the base class of TagQueryResultRow {@link QueryResultRow}
	 * has its own idea of equality and the vaadin table is a set without duplicates and 
	 * identity is computed by itemId object identity we need this wrapper to provide 
	 * that object identity. 
	 * 
	 */
	private static class RowWrapper {
		TagQueryResultRow row;

		public RowWrapper(TagQueryResultRow row) {
			this.row = row;
		}
		
	}
	
	private static interface TreeEntrySelectionHandler {
		public QueryResultRowArray getResultRows(boolean selected);
	}
	
	private static class UmcTreeEntrySelectionHandler implements TreeEntrySelectionHandler {
		private TreeTable resultTable;
		private String umcItemID;
		
		public UmcTreeEntrySelectionHandler(TreeTable resultTable,
				String umcItemID) {
			this.resultTable = resultTable;
			this.umcItemID = umcItemID;
		}

		public QueryResultRowArray getResultRows(boolean selected) {
			QueryResultRowArray result = new QueryResultRowArray();
			@SuppressWarnings("unchecked")
			Collection<RowWrapper> rows = 
					(Collection<RowWrapper>)resultTable.getChildren(
							umcItemID);
			if (rows != null) {
				for (RowWrapper wrapper : rows) {
					((CheckBox)resultTable.getItem(wrapper).getItemProperty(
							TreePropertyName.visible).getValue()).setValue(selected);
					result.add(wrapper.row);
				}
			}
			return result;
		}
	}
	
	private static class TagQueryResultRowTreeEntrySelectionHandler 
		implements TreeEntrySelectionHandler {
		
		private QueryResultRow row;
		
		public TagQueryResultRowTreeEntrySelectionHandler(QueryResultRow row) {
			this.row = row;
		}

		public QueryResultRowArray getResultRows(boolean selected) {
			QueryResultRowArray result = new QueryResultRowArray();
			result.add(row);
			return result;
		}
		
	}

	private static class SourceDocumentTreeEntrySelectionHandler implements TreeEntrySelectionHandler {
		
		private TreeTable resultTable;
		private String sourceDocumentItemID;

		public SourceDocumentTreeEntrySelectionHandler(TreeTable resultTable,
				String sourceDocumentItemID) {
			this.resultTable = resultTable;
			this.sourceDocumentItemID = sourceDocumentItemID;
		}

		public QueryResultRowArray getResultRows(boolean selected) {
			@SuppressWarnings("unchecked")
			Collection<String> umcItemIDs = 
					(Collection<String>)resultTable.getChildren(
							sourceDocumentItemID);
			QueryResultRowArray result = new QueryResultRowArray();
			
			for (String umcItemID : umcItemIDs) {
				((CheckBox)resultTable.getItem(umcItemID).getItemProperty(
						TreePropertyName.visible).getValue()).setValue(selected);
				result.addAll(
					new UmcTreeEntrySelectionHandler(
							resultTable, umcItemID).getResultRows(selected));
			}
			
			return result;
		}
	}
	
	private static class TagDefinitionTreeEntrySelectionHandler implements TreeEntrySelectionHandler {
		
		private TreeTable resultTable;
		private String tagDefinitionItemID;
		
		public TagDefinitionTreeEntrySelectionHandler(TreeTable resultTable,
				String tagDefinitionItemID) {
			this.resultTable = resultTable;
			this.tagDefinitionItemID = tagDefinitionItemID;
		}
		
		public QueryResultRowArray getResultRows(boolean selected) {
			@SuppressWarnings("unchecked")
			Collection<String> sourceDocItemIDs = 
					(Collection<String>)resultTable.getChildren(
							tagDefinitionItemID);
			QueryResultRowArray result = new QueryResultRowArray();
			
			if (sourceDocItemIDs != null) {
				for (String sourceDocItemID : sourceDocItemIDs) {
					((CheckBox)resultTable.getItem(sourceDocItemID).getItemProperty(
							TreePropertyName.visible).getValue()).setValue(selected);
					result.addAll(
						new SourceDocumentTreeEntrySelectionHandler(
								resultTable, sourceDocItemID).getResultRows(selected));
				}
			}
			
			return result;
		}
		
	}
	
	private static enum TreePropertyName {
		caption,
		propertyname,
		propertyvalue,
		frequency, 
		visible, 
		sourcedocument, 
		markupcollection, 
		phrase,
		;
	}
	
	private TreeTable resultTable;
	private Repository repository;
	private KwicPanel kwicPanel;
	private Button btDist;
	private boolean init = false;
	private GroupedQueryResultSelectionListener resultSelectionListener;
	private RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider;
	private Button btSelectAll;
	private Button btDeselectAll;
	private Button btUntagResults;
	private Button btResultExcelExport;
	private Button btKwicExcelExport;
	private Button btKwicCsvExport;
	private Button btHelp;
	private Button btResultCsvExport;

	private CheckBox cbFlatTable;
	private CheckBox cbPropAsColumns;
	private boolean resetColumns = false;
	private QueryResult curQueryResult;
	private Button btSelectAllKwic;
	private Slider kwicSizeSlider;
	
	MarkupResultHelpWindow markupResultHelpWindow = new MarkupResultHelpWindow();
	
	public MarkupResultPanel(
			Repository repository, 
			GroupedQueryResultSelectionListener resultSelectionListener, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider) {
		this.curQueryResult = new QueryResultRowArray();
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
		cbPropAsColumns.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				QueryResult queryResult = getQueryResult();
				
				try {
					setQueryResult(queryResult);
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"error converting Query Result!", e);
				}
			}
		});
		cbFlatTable.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				handleCbFlatTableRequest();
			}
		});
		btDist.addClickListener(new ClickListener() {
			
			@SuppressWarnings("unchecked")
			public void buttonClick(ClickEvent event) {
				GroupedQueryResultSet set = new GroupedQueryResultSet();
				
				Set<GroupedQueryResult> selection = new HashSet<GroupedQueryResult>();
				
				selection.addAll(
						getSelectionAsGroupedQueryResults(
								(Set<Object>)resultTable.getValue()));
				
				if (selection.size() > 1) {
					AccumulativeGroupedQueryResult accResult =
							new AccumulativeGroupedQueryResult(selection);
					
					set.add(accResult);
				}
				else if (selection.size() == 1) {
					set.add(selection.iterator().next());
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
		
		btUntagResults.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				untagResults();
			}
		});
		
		btSelectAllKwic.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				kwicPanel.selectAll();
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

		btResultExcelExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
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
		
		btResultCsvExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				try {
					if(!cbFlatTable.getValue()) {
						cbFlatTable.setValue(Boolean.TRUE);
					}
					CsvExport csvExport = new CsvExport(resultTable);
					csvExport.convertTable();
					csvExport.sendConverted();
				}
				catch (CsvExportException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							"Error creating CSV export!", e);
				}
			}
		});
		
		btHelp.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if(markupResultHelpWindow.getParent() == null){
					UI.getCurrent().addWindow(markupResultHelpWindow);
				} else {
					UI.getCurrent().removeWindow(markupResultHelpWindow);
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
							"Error adjusting KWIC size!", e);
				}
			}
		});
	}
	
	private void handleCbFlatTableRequest() {
		cbPropAsColumns.setVisible(cbFlatTable.getValue());
		QueryResult queryResult = getQueryResult();
		
		try {
			setQueryResult(queryResult);
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"error converting Query Result!", e);
		}
	}

	private QueryResult getQueryResult() {
		return curQueryResult;
	}

	private void untagResults() {
		final Set<QueryResultRow> selection = kwicPanel.getSelection();
		if ((selection != null) && !selection.isEmpty()) {
			ConfirmDialog.show(UI.getCurrent(), 
					"Remove Tag", 
					"Do you want to remove the selected Tag?", 
					"Yes", "No", new ConfirmDialog.Listener() {
				public void onClose(ConfirmDialog dialog) {
					if (dialog.isConfirmed()) {
						try {
							UserMarkupCollectionManager umcManager = 
									new UserMarkupCollectionManager(repository);
							List<String> toBeDeletedIDs = new ArrayList<String>();
							
							for (QueryResultRow row : selection) {
								TagQueryResultRow tagRow = (TagQueryResultRow)row;
								if (!umcManager.contains(tagRow.getMarkupCollectionId())) {
									umcManager.add(
										repository.getUserMarkupCollection(
											new UserMarkupCollectionReference(
													tagRow.getMarkupCollectionId(), 
													new ContentInfoSet())));
								}
								toBeDeletedIDs.add(tagRow.getTagInstanceId());
							}
							umcManager.removeTagInstance(toBeDeletedIDs);
							
						} catch (IOException e) {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
									"Error untagging search results!", e);
						}
					}
				}

			});

		}
		else {
			Notification.show(
					"Information", 
					"Please select one or more rows in the Kwic view first!",
					Type.TRAY_NOTIFICATION);
		}
	}

	private void selectAllForKwic(boolean selected) {
		for (Object o : resultTable.getItemIds()) {
			if (resultTable.getParent(o) == null) {
				CheckBox cbVisibleInKwic = 
					(CheckBox) resultTable.getItem(o).getItemProperty(
						TreePropertyName.visible).getValue();
				cbVisibleInKwic.setValue(selected);
			}
		}
	}

	
	private Collection<TagQueryResult> getSelectionAsGroupedQueryResults(
			Set<Object> selection) {
		
		Set<TagQueryResultRow> rows = new HashSet<TagQueryResultRow>();
		for (Object selValue : selection) {
			rows.addAll(getTagQueryResultRows(selValue));
		}
		
		Map<String, TagQueryResult> tagQueryResultsByTagDefPath = 
				new HashMap<String, TagQueryResult>();
		
		for(TagQueryResultRow row : rows) {
			String tagPath = row.getTagDefinitionPath();

			if (!tagQueryResultsByTagDefPath.containsKey(tagPath)) {
				tagQueryResultsByTagDefPath.put(tagPath, new TagQueryResult(tagPath));
			}
			
			TagQueryResult tagQueryResult = tagQueryResultsByTagDefPath.get(tagPath);
			tagQueryResult.add(row);
		}
		
		
		return tagQueryResultsByTagDefPath.values();
	}

	private Collection<? extends TagQueryResultRow> getTagQueryResultRows(Object selValue) {
		HashSet<TagQueryResultRow> result = new HashSet<TagQueryResultRow>();
		if (resultTable.hasChildren(selValue)) {
			for (Object child : resultTable.getChildren(selValue)) {
				if (child instanceof RowWrapper) {
					result.add(((RowWrapper)child).row);
				}
				else {
					result.addAll(getTagQueryResultRows(child));
				}
			}
		}
		return result;
	}

	private void initComponents() {
		setSizeFull();
		
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setSizeFull();
		VerticalLayout leftComponent = new VerticalLayout();
		leftComponent.setSpacing(true);
		leftComponent.setSizeFull();
		leftComponent.addStyleName("analyzer-panel-padding");
		
		resultTable = new TreeTable();
		resultTable.setSelectable(true);
		resultTable.setMultiSelect(true);
		HierarchicalContainer container = new HierarchicalContainer();
		container.setItemSorter(
				new PropertyDependentItemSorter(
						TreePropertyName.caption, 
						new PropertyToTrimmedStringCIComparator()));
		
		resultTable.setContainerDataSource(container);
		setupContainerProperties();
		resultTable.setSizeFull();
		
		//TODO: a description generator that shows the version of a Tag
//		resultTable.setItemDescriptionGenerator(generator);
		
		leftComponent.addComponent(resultTable);
		leftComponent.setExpandRatio(resultTable, 1.0f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		
		btDist = new Button();
		btDist.setIcon(new ClassResource("analyzer/resources/chart.gif"));
		buttonPanel.addComponent(btDist);
		
		btResultExcelExport = new Button();
		btResultExcelExport.setIcon(new ClassResource("analyzer/resources/excel.png"));
		btResultExcelExport.setDescription(
				"Export all Query result data as an Excel spreadsheet.");
		buttonPanel.addComponent(btResultExcelExport);
		
		
		btResultCsvExport = new Button();
		btResultCsvExport.setIcon(new ClassResource(
				"analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text
		btResultCsvExport.setDescription(
				"Export all Query result data as a flat CSV File.");
		buttonPanel.addComponent(btResultCsvExport);
		
		cbFlatTable = new CheckBox("flat table", false);
		cbFlatTable.setDescription(
			"<p>Display query results as a flat table for sortability by Source " +
			"Document, Markup Collection and phrase.</p>" +
			"<p>If the query is a <i>property</i> query this ensures sortability " +
			"of properties as well.</p>");
		cbFlatTable.setImmediate(true);
		
		buttonPanel.addComponent(cbFlatTable);
		buttonPanel.setComponentAlignment(cbFlatTable, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(cbFlatTable, 1f);
		
		cbPropAsColumns = new CheckBox("properties as columns", false);
		cbPropAsColumns.setDescription(
			"Create a column for each property in the QueryResult selected " +
			"by a <i>property</i> query.");
		
		cbPropAsColumns.setImmediate(true);
		buttonPanel.addComponent(cbPropAsColumns);
		buttonPanel.setComponentAlignment(cbPropAsColumns, Alignment.MIDDLE_RIGHT);
		cbPropAsColumns.setVisible(false);
		
		btSelectAll = new Button("Select all for Kwic");
		
		buttonPanel.addComponent(btSelectAll);
		buttonPanel.setComponentAlignment(btSelectAll, Alignment.MIDDLE_RIGHT);

		btDeselectAll = new Button("Deselect all for Kwic");
		buttonPanel.addComponent(btDeselectAll);
		buttonPanel.setComponentAlignment(btDeselectAll, Alignment.MIDDLE_RIGHT);
		
		leftComponent.addComponent(buttonPanel);

		splitPanel.addComponent(leftComponent);
		
		VerticalLayout rightComponent = new VerticalLayout();
		rightComponent.setSpacing(true);
		rightComponent.setSizeFull();
		rightComponent.addStyleName("analyzer-panel-padding");
		
		this.kwicPanel = 
				new KwicPanel(
					repository, relevantUserMarkupCollectionProvider,  true);
		rightComponent.addComponent(kwicPanel);
		rightComponent.setExpandRatio(kwicPanel, 1f);
		
		HorizontalLayout kwicButtonPanel = new HorizontalLayout();
		kwicButtonPanel.setSpacing(true);
		kwicButtonPanel.setWidth("100%");
		kwicButtonPanel.setStyleName("help-padding-fix");
		
		btKwicExcelExport = new Button();
		btKwicExcelExport.setIcon(new ClassResource("analyzer/resources/excel.png"));
		btKwicExcelExport.setDescription(
				"Export all Query result data as an Excel spreadsheet.");
		kwicButtonPanel.addComponent(btKwicExcelExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicExcelExport, Alignment.MIDDLE_LEFT);
		
		btKwicCsvExport = new Button();
		btKwicCsvExport.setIcon(new ClassResource(
				"analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text
		btKwicCsvExport.setDescription(
				"Export all Query result data as CSV File.");
		kwicButtonPanel.addComponent(btKwicCsvExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicCsvExport, Alignment.MIDDLE_LEFT);
		
		kwicSizeSlider = new Slider(null, 1, 30, "token(s) context");
		kwicSizeSlider.setValue(5.0);
		kwicButtonPanel.addComponent(kwicSizeSlider);
		kwicButtonPanel.setComponentAlignment(
				kwicSizeSlider, Alignment.MIDDLE_LEFT);

		btSelectAllKwic = new Button("Select all");
		kwicButtonPanel.addComponent(btSelectAllKwic);
		kwicButtonPanel.setComponentAlignment(btSelectAllKwic, Alignment.MIDDLE_RIGHT);
		kwicButtonPanel.setExpandRatio(btSelectAllKwic, 1f);
		
		btUntagResults = new Button("Untag selected Kwics");
		kwicButtonPanel.addComponent(btUntagResults);
		kwicButtonPanel.setComponentAlignment(btUntagResults, Alignment.MIDDLE_RIGHT);
		kwicButtonPanel.setExpandRatio(btUntagResults, 0f);
		
		btHelp = new Button("");
		btHelp.addStyleName("icon-button"); // for top-margin
		btHelp.setIcon(new ClassResource("resources/icon-help.gif"));
		btHelp.addStyleName("help-button");
		
		kwicButtonPanel.addComponent(btHelp);

		kwicButtonPanel.setComponentAlignment(btHelp, Alignment.MIDDLE_RIGHT);
		
		rightComponent.addComponent(kwicButtonPanel);
		rightComponent.setComponentAlignment(kwicButtonPanel, Alignment.MIDDLE_RIGHT);
		
		splitPanel.addComponent(rightComponent);
		
		addComponent(splitPanel);
	}
	
	private void setupContainerProperties() {
		resultTable.addContainerProperty(
				TreePropertyName.caption, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.caption, "Tag Type Definition");
		
		resultTable.addContainerProperty(
				TreePropertyName.sourcedocument, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.sourcedocument, "Source Document");
		
		resultTable.addContainerProperty(
				TreePropertyName.markupcollection, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.markupcollection, "Markup Collection");

		resultTable.addContainerProperty(
				TreePropertyName.phrase, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.phrase, "Phrase");

		resultTable.addContainerProperty(
				TreePropertyName.propertyname, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.propertyname, "Property");

		resultTable.addContainerProperty(
				TreePropertyName.propertyvalue, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.propertyvalue, "Property value");
		
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, "Frequency");
		resultTable.addContainerProperty(
				TreePropertyName.visible, AbstractComponent.class, null);
		resultTable.setColumnHeader(TreePropertyName.visible, "Visible in Kwic");
		
		resultTable.setItemCaptionPropertyId(TreePropertyName.caption);
		
		resultTable.setVisibleColumns(new Object[] {
				TreePropertyName.caption,
				TreePropertyName.sourcedocument,
				TreePropertyName.markupcollection,
				TreePropertyName.phrase,
				TreePropertyName.frequency,
				TreePropertyName.visible,
		});
	}

	public void setQueryResult(QueryResult queryResult) throws IOException {
		curQueryResult = queryResult;
		
		kwicPanel.clear();
		resultTable.removeAllItems();
		
		if (resetColumns) {
			for (Object propId : resultTable.getContainerPropertyIds().toArray()) {
				resultTable.removeContainerProperty(propId);
			}
			setupContainerProperties();
			resetColumns = false;
		}

		int totalFreq = 0;
	
		Set<String> tagDefinitions = new HashSet<String>();
		
		if (!(queryResult instanceof GroupedQueryResultSet)) { // performance opt for Wordlists which are freqency based GroupedQueryResultSets
																// and we want to avoid expensive iteration
			if (cbFlatTable.getValue() && cbPropAsColumns.getValue()) {
				resetColumns = true;
				totalFreq = setQueryResultGroupedByTagInstance(
						queryResult, tagDefinitions);
			}
			else {
				totalFreq = setQueryResultByRow(
						queryResult, tagDefinitions);
			}
		}
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, 
				"Total count: " + tagDefinitions.size());
		resultTable.setColumnFooter(
				TreePropertyName.frequency, "Total frequency: " + totalFreq);
		
	}
	
	@SuppressWarnings("unchecked")
	private int setQueryResultGroupedByTagInstance(
			QueryResult queryResult,
			Set<String> tagDefinitions) throws IOException {
		
		int totalFreq = 0;
		
		HashMap<String, QueryResultRowArray> rowsGroupedByTagInstance = 
				new HashMap<String, QueryResultRowArray>();
		
		for (QueryResultRow row : queryResult) {
			
			if (row instanceof TagQueryResultRow) {
				TagQueryResultRow tRow = (TagQueryResultRow) row;
				QueryResultRowArray rows = 
						rowsGroupedByTagInstance.get(tRow.getTagInstanceId());
				
				if (rows == null) {
					rows = new QueryResultRowArray();
					rowsGroupedByTagInstance.put(tRow.getTagInstanceId(), rows);
				}
				rows.add(tRow);
			}
		}

		Set<String> propNames = new TreeSet<String>();
		
		for (Map.Entry<String, QueryResultRowArray> entry : rowsGroupedByTagInstance.entrySet()) {
			
			QueryResultRowArray rows = entry.getValue();
			
			TagQueryResultRow masterRow = (TagQueryResultRow) rows.get(0); 
			totalFreq++;
			
			tagDefinitions.add(masterRow.getTagDefinitionId());
			String markupCollectionsId = masterRow.getMarkupCollectionId();
			SourceDocument sourceDocument = 
					repository.getSourceDocument(masterRow.getSourceDocumentId());

			RowWrapper itemId = new RowWrapper(masterRow);
			resultTable.addItem(itemId);
			resultTable.setChildrenAllowed(itemId, false);
			
			resultTable.getContainerProperty(
				itemId, TreePropertyName.caption).setValue(
					masterRow.getTagDefinitionPath());
			resultTable.getContainerProperty(
					itemId, TreePropertyName.sourcedocument).setValue(
					sourceDocument.toString());
			resultTable.getContainerProperty(
					itemId, TreePropertyName.markupcollection).setValue(
					sourceDocument.getUserMarkupCollectionReference(markupCollectionsId).getName());
			resultTable.getContainerProperty(
					itemId, TreePropertyName.phrase).setValue(
					masterRow.getPhrase());

			resultTable.getContainerProperty(itemId, TreePropertyName.frequency).setValue(
					1);
			resultTable.getContainerProperty(itemId, TreePropertyName.visible).setValue(
					createCheckbox(
							new TagQueryResultRowTreeEntrySelectionHandler(masterRow)));
			
			for (QueryResultRow row : rows) {
				
				TagQueryResultRow tRow = (TagQueryResultRow)row;
				
				String propDefId = tRow.getPropertyDefinitionId();
				if (propDefId != null) {
					String propertyName = tRow.getPropertyName();
					if (!resultTable.getContainerPropertyIds().contains(propertyName)) {
						resultTable.addContainerProperty(propertyName, String.class, null);
						propNames.add(propertyName);
					}
					resultTable.getContainerProperty(
							itemId, propertyName).setValue(tRow.getPropertyValue());
				}				
			}
		}
		
		ArrayList<Object> visibleColumns = new ArrayList<Object>();
		visibleColumns.add(TreePropertyName.caption);
		visibleColumns.add(TreePropertyName.sourcedocument);
		visibleColumns.add(TreePropertyName.markupcollection);
		visibleColumns.add(TreePropertyName.phrase);

		for (String propDefName : propNames) {
			visibleColumns.add(propDefName);
		}
		
		visibleColumns.add(TreePropertyName.frequency);
		visibleColumns.add(TreePropertyName.visible);
		
		resultTable.setVisibleColumns(visibleColumns.toArray());

		
		return totalFreq;
	}

	private int setQueryResultByRow(
			QueryResult queryResult, 
			Set<String> tagDefinitions) throws IOException {
		
		int totalFreq = 0;
		boolean displayProperties = false;

		for (QueryResultRow row : queryResult) {
			if (row instanceof TagQueryResultRow) {
				TagQueryResultRow tRow = (TagQueryResultRow)row;
				tagDefinitions.add(tRow.getTagDefinitionId());
				if (cbFlatTable.getValue()) {
					addFlatTagQueryResultRow(tRow);
				}
				else {
					addTagQueryResultRow(tRow);
				}
				if (!displayProperties && (tRow.getPropertyDefinitionId() != null)) {
					displayProperties = true;
				}
				totalFreq++;
			}
		}
		
		ArrayList<Object> visibleColumns = new ArrayList<Object>();
		visibleColumns.add(TreePropertyName.caption);
		
		if (cbFlatTable.getValue()) {
			visibleColumns.add(TreePropertyName.sourcedocument);
			visibleColumns.add(TreePropertyName.markupcollection);
			visibleColumns.add(TreePropertyName.phrase);
			if (displayProperties) {
				visibleColumns.add(TreePropertyName.propertyname);
				visibleColumns.add(TreePropertyName.propertyvalue);
			}
		}
		else if (displayProperties) {
			visibleColumns.add(TreePropertyName.propertyname);
			visibleColumns.add(TreePropertyName.propertyvalue);
		}
		
		visibleColumns.add(TreePropertyName.frequency);
		visibleColumns.add(TreePropertyName.visible);
		
		resultTable.setVisibleColumns(visibleColumns.toArray());

		
		return totalFreq;
	}

	@SuppressWarnings("unchecked")
	private void addFlatTagQueryResultRow(TagQueryResultRow row) throws IOException {
		String markupCollectionsId = row.getMarkupCollectionId();
		SourceDocument sourceDocument = 
				repository.getSourceDocument(row.getSourceDocumentId());
		
		RowWrapper wrapper = new RowWrapper(row);
		
		resultTable.addItem(wrapper);
		
		resultTable.getContainerProperty(wrapper, TreePropertyName.caption).setValue(
				row.getTagDefinitionPath());
		resultTable.getContainerProperty(wrapper, TreePropertyName.sourcedocument).setValue(
				sourceDocument.toString());
		resultTable.getContainerProperty(wrapper, TreePropertyName.markupcollection).setValue(
				sourceDocument.getUserMarkupCollectionReference(markupCollectionsId).getName());
		resultTable.getContainerProperty(wrapper, TreePropertyName.phrase).setValue(
				row.getPhrase());
		if (row.getPropertyDefinitionId()!=null) {
			resultTable.getContainerProperty(wrapper, TreePropertyName.propertyname).setValue(
				row.getPropertyName());
			resultTable.getContainerProperty(wrapper, TreePropertyName.propertyvalue).setValue(
				row.getPropertyValue());
		}
		resultTable.getContainerProperty(wrapper, TreePropertyName.frequency).setValue(
				1);
		resultTable.getContainerProperty(wrapper, TreePropertyName.visible).setValue(
				createCheckbox(
						new TagQueryResultRowTreeEntrySelectionHandler(row)));
		
		resultTable.setChildrenAllowed(wrapper, false);
	}

	@SuppressWarnings("unchecked")
	private void addTagQueryResultRow(final TagQueryResultRow row) 
				throws IOException {
		
		String tagDefinitionId = row.getTagDefinitionId();
		String markupCollectionsId = row.getMarkupCollectionId();
		SourceDocument sourceDocument = 
				repository.getSourceDocument(row.getSourceDocumentId());
		
		String tagDefinitionItemID = 
				row.getTagDefinitionId() + "#" + row.getTagDefinitionVersion();

		if (!resultTable.containsId(tagDefinitionItemID)) {
			resultTable.addItem(tagDefinitionItemID);
			resultTable.getContainerProperty(tagDefinitionItemID, TreePropertyName.caption).setValue(
						row.getTagDefinitionPath());
			resultTable.getContainerProperty(tagDefinitionItemID, TreePropertyName.frequency).setValue(
						0);
			resultTable.getContainerProperty(tagDefinitionItemID, TreePropertyName.visible).setValue(
						createCheckbox(
							new TagDefinitionTreeEntrySelectionHandler(
								resultTable, tagDefinitionItemID)));
		}

		Property<Integer> tagDefFreqProperty = 
				resultTable.getItem(tagDefinitionItemID).getItemProperty(
						TreePropertyName.frequency);
		tagDefFreqProperty.setValue(((Integer)tagDefFreqProperty.getValue())+1);
		
		final String sourceDocumentItemID = tagDefinitionId+ "@" + sourceDocument;
		
		if (!resultTable.containsId(sourceDocumentItemID)) {
		
			resultTable.addItem(sourceDocumentItemID);
			resultTable.getContainerProperty(sourceDocumentItemID, TreePropertyName.caption).setValue(
						sourceDocument.toString());
			resultTable.getContainerProperty(sourceDocumentItemID, TreePropertyName.frequency).setValue(
						0);
			resultTable.getContainerProperty(sourceDocumentItemID, TreePropertyName.visible).setValue(
						createCheckbox(
							new SourceDocumentTreeEntrySelectionHandler(
									resultTable, sourceDocumentItemID)));
			resultTable.setParent(sourceDocumentItemID, tagDefinitionItemID);
		}
		
		Property<Integer> sourceDocFreqProperty = 
				resultTable.getItem(sourceDocumentItemID).getItemProperty(
						TreePropertyName.frequency);
		sourceDocFreqProperty.setValue(
				((Integer)sourceDocFreqProperty.getValue())+1);
		
		final String umcItemID = sourceDocumentItemID + "@" + markupCollectionsId;
		
		if (!resultTable.containsId(umcItemID)) {
			resultTable.addItem(umcItemID);
			resultTable.getContainerProperty(umcItemID, TreePropertyName.caption).setValue(
					sourceDocument.getUserMarkupCollectionReference(markupCollectionsId).getName());
			resultTable.getContainerProperty(umcItemID, TreePropertyName.frequency).setValue(
					0);
			resultTable.getContainerProperty(umcItemID, TreePropertyName.visible).setValue(
					createCheckbox(
						new UmcTreeEntrySelectionHandler(resultTable, umcItemID)));
			resultTable.setParent(umcItemID, sourceDocumentItemID);
		}
		
		Property<Integer> userMarkupCollFreqProperty = 
				resultTable.getItem(umcItemID).getItemProperty(
						TreePropertyName.frequency);
		userMarkupCollFreqProperty.setValue(
				((Integer)userMarkupCollFreqProperty.getValue())+1);
		RowWrapper wrapper = new RowWrapper(row);
		resultTable.addItem(wrapper);
		
		resultTable.getContainerProperty(wrapper, TreePropertyName.caption).setValue(
				row.getTagDefinitionPath());
		resultTable.getContainerProperty(wrapper, TreePropertyName.sourcedocument).setValue(
				sourceDocument.toString());
		resultTable.getContainerProperty(wrapper, TreePropertyName.markupcollection).setValue(
				sourceDocument.getUserMarkupCollectionReference(row.getMarkupCollectionId()).getName());
		resultTable.getContainerProperty(wrapper, TreePropertyName.phrase).setValue(
				row.getPhrase());
		if (row.getPropertyDefinitionId()!=null) {
			resultTable.getContainerProperty(wrapper, TreePropertyName.propertyname).setValue(
				row.getPropertyName());
			resultTable.getContainerProperty(wrapper, TreePropertyName.propertyvalue).setValue(
				row.getPropertyValue());
		}
		resultTable.getContainerProperty(wrapper, TreePropertyName.frequency).setValue(
				1);
		resultTable.getContainerProperty(wrapper, TreePropertyName.visible).setValue(
				createCheckbox(
						new TagQueryResultRowTreeEntrySelectionHandler(row)));
		
		resultTable.setParent(wrapper, umcItemID);
		resultTable.setChildrenAllowed(wrapper, false);
	}

	private CheckBox createCheckbox(
			final TreeEntrySelectionHandler treeEntrySelectionHandler) {
		final CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				boolean selected = 
						cbShowInKwicView.getValue();

				fireShowInKwicViewSelected(
					treeEntrySelectionHandler, selected);
			}
		});
		return cbShowInKwicView;
	}

	private void fireShowInKwicViewSelected(
			TreeEntrySelectionHandler treeEntrySelectionHandler,
			boolean selected) {

		QueryResultRowArray queryResult = new QueryResultRowArray();
		
		queryResult.addAll(treeEntrySelectionHandler.getResultRows(selected));
		
		if (selected) {
			try {
				kwicPanel.addQueryResultRows(queryResult);
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error showing KWIC results!", e);
			}
		}
		else {
			kwicPanel.removeQueryResultRows(queryResult);
		}
		
	}
	
	public boolean isEmpty() {
		return resultTable.getItemIds().isEmpty();
	}
}
