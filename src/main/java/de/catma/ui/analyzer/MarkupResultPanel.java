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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.server.ClassResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.HierarchicalContainer;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.TreeTable;
import com.vaadin.v7.ui.VerticalLayout;

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
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;
import de.catma.user.Permission;

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
	private Button btTagResults;
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
	
	private MarkupResultHelpWindow markupResultHelpWindow = new MarkupResultHelpWindow();
	private TagKwicResultsProvider tagKwicResultsProvider;
	private Button btSelectAllRows;
	private Button btDeselectAllRows;
	private Button btVega;
	private QueryOptionsProvider queryOptionsProvider;
	
	public MarkupResultPanel(
			Repository repository, 
			GroupedQueryResultSelectionListener resultSelectionListener, 
			RelevantUserMarkupCollectionProvider relevantUserMarkupCollectionProvider,
			TagKwicResultsProvider tagKwicResultsProvider,
			QueryOptionsProvider queryOptionsProvider) {
		this.curQueryResult = new QueryResultRowArray();
		this.repository = repository;
		this.resultSelectionListener = resultSelectionListener;
		this.relevantUserMarkupCollectionProvider = relevantUserMarkupCollectionProvider;
		this.tagKwicResultsProvider = tagKwicResultsProvider;
		this.queryOptionsProvider = queryOptionsProvider;
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
		
		btVega.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				((CatmaApplication)UI.getCurrent()).addVega(curQueryResult, queryOptionsProvider, repository);
			}
		});
		

		cbPropAsColumns.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				QueryResult queryResult = getQueryResult();
				
				try {
					setQueryResult(queryResult);
				} catch (Exception e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("MarkupResultPanel.errorConvertingResults"), e); //$NON-NLS-1$
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
							Messages.getString("MarkupResultPanel.infoTitle"), Messages.getString("MarkupResultPanel.rowSelectionHint"),  //$NON-NLS-1$ //$NON-NLS-2$
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
		
		btTagResults.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				tagResults();
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
				//FIXME:
//            	try {
//					ExcelExport excelExport = 
//							new HierarchicalExcelExport(kwicPanel.getKwicTable(), 
//									Messages.getString("MarkupResultPanel.kwicQueryResult")); //$NON-NLS-1$
//					excelExport.excludeCollapsedColumns();
//					excelExport.setReportTitle(Messages.getString("MarkupResultPanel.kwicQueryResult")); //$NON-NLS-1$
//					excelExport.export();
//				} catch (IllegalArgumentException e) {
//					HTMLNotification.show(
//						Messages.getString("MarkupResultPanel.error"),  //$NON-NLS-1$
//						MessageFormat.format(Messages.getString("MarkupResultPanel.excelExportError"), //$NON-NLS-1$
//								e.getMessage()),
//						Type.WARNING_MESSAGE);
//					
//					e.printStackTrace();
//				}
			}
		});
		
		btKwicCsvExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {       
				//FIXME:
//				try {
//					CsvExport csvExport = new CsvExport(kwicPanel.getKwicTable());
//					csvExport.convertTable();
//					csvExport.sendConverted();
//				}
//				catch (CsvExportException e) {
//					((CatmaApplication)UI.getCurrent()).showAndLogError(
//							Messages.getString("MarkupResultPanel.csvExportError"), e); //$NON-NLS-1$
//				}
			}
		});

		btResultExcelExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				//FIXME:
//				try {
//	            	ExcelExport excelExport = 
//	            			new HierarchicalExcelExport(resultTable, Messages.getString("MarkupResultPanel.queryResult")); //$NON-NLS-1$
//	                excelExport.excludeCollapsedColumns();
//	                excelExport.setReportTitle(Messages.getString("MarkupResultPanel.queryResult")); //$NON-NLS-1$
//	                excelExport.export();
//				} catch (IllegalArgumentException e) {
//					HTMLNotification.show(
//						Messages.getString("MarkupResultPanel.error"),  //$NON-NLS-1$
//						MessageFormat.format(Messages.getString("MarkupResultPanel.excelExportError"), //$NON-NLS-1$
//								e.getMessage()),
//						Type.WARNING_MESSAGE);
//					
//					e.printStackTrace();
//				}
			}
		});
		
		btResultCsvExport.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				//FIXME:
//				try {
//					if(!cbFlatTable.getValue()) {
//						cbFlatTable.setValue(Boolean.TRUE);
//					}
//					CsvExport csvExport = new CsvExport(resultTable);
//					csvExport.convertTable();
//					csvExport.sendConverted();
//				}
//				catch (CsvExportException e) {
//					((CatmaApplication)UI.getCurrent()).showAndLogError(
//							Messages.getString("MarkupResultPanel.csvExportError"), e); //$NON-NLS-1$
//				}
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
		
		kwicSizeSlider.addValueListener(new com.vaadin.data.HasValue.ValueChangeListener<Double>() {
			
			@Override
			public void valueChange(com.vaadin.data.HasValue.ValueChangeEvent<Double> event) {
				try {
					Double kwicSize = (Double) event.getValue();
					kwicPanel.setKwicSize(kwicSize.intValue());
				}
				catch (Exception e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
							Messages.getString("MarkupResultPanel.errorKwicSize"), e); //$NON-NLS-1$
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
	
	private void handleCbFlatTableRequest() {
		cbPropAsColumns.setVisible(cbFlatTable.getValue());
		QueryResult queryResult = getQueryResult();
		
		try {
			setQueryResult(queryResult);
		} catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("MarkupResultPanel.errorConvertingResults"), e); //$NON-NLS-1$
		}
	}

	private QueryResult getQueryResult() {
		return curQueryResult;
	}
	
	private void tagResults() {
		if (kwicPanel.getSelection().isEmpty()) {
			Notification.show(Messages.getString("MarkupResultPanel.infoTitle"), Messages.getString("MarkupResultPanel.kwicSelectionHint"), Type.TRAY_NOTIFICATION); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			tagKwicResultsProvider.tagResults();
		}
	}

	private void untagResults() {
		final Set<QueryResultRow> selection = kwicPanel.getSelection();
		if ((selection != null) && !selection.isEmpty()) {
			ConfirmDialog.show(UI.getCurrent(), 
					Messages.getString("MarkupResultPanel.removeTag"),  //$NON-NLS-1$
					Messages.getString("MarkupResultPanel.wantToRemoveSelectedTag"),  //$NON-NLS-1$
					Messages.getString("MarkupResultPanel.yes"), Messages.getString("MarkupResultPanel.no"), new ConfirmDialog.Listener() { //$NON-NLS-1$ //$NON-NLS-2$
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
													null, //TODO: revisionHash might be needed
													new ContentInfoSet(),
													tagRow.getSourceDocumentId(),
													"" //TODO: sourceDocRev
													)));
								}
								toBeDeletedIDs.add(tagRow.getTagInstanceId());
							}
							umcManager.removeTagInstance(toBeDeletedIDs, true);
							
						} catch (IOException e) {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
									Messages.getString("MarkupResultPanel.errorUntaggingSearchResults"), e); //$NON-NLS-1$
						}
					}
				}

			});

		}
		else {
			Notification.show(
					Messages.getString("MarkupResultPanel.infoTitle"),  //$NON-NLS-1$
					Messages.getString("MarkupResultPanel.kwicViewRowSelectionHint"), //$NON-NLS-1$
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
		leftComponent.addStyleName("analyzer-panel-padding"); //$NON-NLS-1$
		
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
		buttonPanel.setWidth("100%"); //$NON-NLS-1$
		
		btDist = new Button();
		btDist.setIcon(new ClassResource("analyzer/resources/chart.gif")); //$NON-NLS-1$
		buttonPanel.addComponent(btDist);

		btVega = new Button("Vega");

		btVega.setDescription(
			"Roll your own visualization");
		
		buttonPanel.addComponent(btVega);

		btVega.setVisible(repository.getUser().hasPermission(Permission.vega));
		
		btResultExcelExport = new Button();
		btResultExcelExport.setIcon(new ClassResource("analyzer/resources/excel.png")); //$NON-NLS-1$
		btResultExcelExport.setDescription(
				Messages.getString("MarkupResultPanel.exportResultToExcel")); //$NON-NLS-1$
		buttonPanel.addComponent(btResultExcelExport);
		
		
		btResultCsvExport = new Button();
		btResultCsvExport.setIcon(new ClassResource(
				"analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text //$NON-NLS-1$
		btResultCsvExport.setDescription(
				Messages.getString("MarkupResultPanel.exportResultToCSV")); //$NON-NLS-1$
		buttonPanel.addComponent(btResultCsvExport);
		
		cbFlatTable = new CheckBox(Messages.getString("MarkupResultPanel.flatTable"), false); //$NON-NLS-1$
		cbFlatTable.setDescription(
			Messages.getString("MarkupResultPanel.flatTableHint")); //$NON-NLS-1$
		cbFlatTable.setImmediate(true);
		
		buttonPanel.addComponent(cbFlatTable);
		buttonPanel.setComponentAlignment(cbFlatTable, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(cbFlatTable, 1f);
		
		cbPropAsColumns = new CheckBox(Messages.getString("MarkupResultPanel.propertiesAsColumns"), false); //$NON-NLS-1$
		cbPropAsColumns.setDescription(
			Messages.getString("MarkupResultPanel.propsAsColumnsHint")); //$NON-NLS-1$
		
		cbPropAsColumns.setImmediate(true);
		buttonPanel.addComponent(cbPropAsColumns);
		buttonPanel.setComponentAlignment(cbPropAsColumns, Alignment.MIDDLE_RIGHT);
		cbPropAsColumns.setVisible(false);
		
		btSelectAllRows = new Button(Messages.getString("MarkupResultPanel.selectAll")); //$NON-NLS-1$
		buttonPanel.addComponent(btSelectAllRows);
		buttonPanel.setComponentAlignment(btSelectAllRows, Alignment.MIDDLE_RIGHT);
		
		btDeselectAllRows = new Button(Messages.getString("MarkupResultPanel.deselectAll")); //$NON-NLS-1$
		buttonPanel.addComponent(btDeselectAllRows);
		buttonPanel.setComponentAlignment(btDeselectAllRows, Alignment.MIDDLE_RIGHT);
		
		
		btSelectAll = new Button(Messages.getString("MarkupResultPanel.selectAllKwic")); //$NON-NLS-1$
		
		buttonPanel.addComponent(btSelectAll);
		buttonPanel.setComponentAlignment(btSelectAll, Alignment.MIDDLE_RIGHT);

		btDeselectAll = new Button(Messages.getString("MarkupResultPanel.deselectAllKwic")); //$NON-NLS-1$
		buttonPanel.addComponent(btDeselectAll);
		buttonPanel.setComponentAlignment(btDeselectAll, Alignment.MIDDLE_RIGHT);
		
		leftComponent.addComponent(buttonPanel);

		splitPanel.addComponent(leftComponent);
		
		VerticalLayout rightComponent = new VerticalLayout();
		rightComponent.setSpacing(true);
		rightComponent.setSizeFull();
		rightComponent.addStyleName("analyzer-panel-padding"); //$NON-NLS-1$
		
		this.kwicPanel = 
				new KwicPanel(
					repository, relevantUserMarkupCollectionProvider,  true);
		rightComponent.addComponent(kwicPanel);
		rightComponent.setExpandRatio(kwicPanel, 1f);
		
		HorizontalLayout kwicButtonPanel = new HorizontalLayout();
		kwicButtonPanel.setSpacing(true);
		kwicButtonPanel.setWidth("100%"); //$NON-NLS-1$
		kwicButtonPanel.setStyleName("help-padding-fix"); //$NON-NLS-1$
		
		btKwicExcelExport = new Button();
		btKwicExcelExport.setIcon(new ClassResource("analyzer/resources/excel.png")); //$NON-NLS-1$
		btKwicExcelExport.setDescription(
				Messages.getString("MarkupResultPanel.exportResultToExcel")); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btKwicExcelExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicExcelExport, Alignment.MIDDLE_LEFT);
		
		btKwicCsvExport = new Button();
		btKwicCsvExport.setIcon(new ClassResource(
				"analyzer/resources/csv_text.png")); //http://findicons.com/icon/84601/csv_text //$NON-NLS-1$
		btKwicCsvExport.setDescription(
				Messages.getString("MarkupResultPanel.exportResultToCSV")); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btKwicCsvExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicCsvExport, Alignment.MIDDLE_LEFT);
		
		kwicSizeSlider = new Slider(null, 1, 30, Messages.getString("MarkupResultPanel.tokenContext")); //$NON-NLS-1$
		kwicSizeSlider.setValue(5.0);
		kwicButtonPanel.addComponent(kwicSizeSlider);
		kwicButtonPanel.setComponentAlignment(
				kwicSizeSlider, Alignment.MIDDLE_LEFT);

		btSelectAllKwic = new Button(Messages.getString("MarkupResultPanel.selectAll")); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btSelectAllKwic);
		kwicButtonPanel.setComponentAlignment(btSelectAllKwic, Alignment.MIDDLE_RIGHT);
		kwicButtonPanel.setExpandRatio(btSelectAllKwic, 1f);
		
		btTagResults = new Button(Messages.getString("MarkupResultPanel.tagSelectedResults")); //$NON-NLS-1$
		btTagResults.addStyleName("primary-button"); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btTagResults);
		kwicButtonPanel.setComponentAlignment(btTagResults, Alignment.MIDDLE_RIGHT);
		
		btUntagResults = new Button(Messages.getString("MarkupResultPanel.untagSelectedResults")); //$NON-NLS-1$
		btUntagResults.addStyleName("secondary-button"); //$NON-NLS-1$
		kwicButtonPanel.addComponent(btUntagResults);
		kwicButtonPanel.setComponentAlignment(btUntagResults, Alignment.MIDDLE_RIGHT);
		kwicButtonPanel.setExpandRatio(btUntagResults, 0f);
		
		btHelp = new Button(FontAwesome.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$
		
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
		resultTable.setColumnHeader(TreePropertyName.caption, Messages.getString("MarkupResultPanel.Tag")); //$NON-NLS-1$
		
		resultTable.addContainerProperty(
				TreePropertyName.sourcedocument, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.sourcedocument, Messages.getString("MarkupResultPanel.SourceDoc")); //$NON-NLS-1$
		
		resultTable.addContainerProperty(
				TreePropertyName.markupcollection, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.markupcollection, Messages.getString("MarkupResultPanel.MarkupCollection")); //$NON-NLS-1$

		resultTable.addContainerProperty(
				TreePropertyName.phrase, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.phrase, Messages.getString("MarkupResultPanel.phrase")); //$NON-NLS-1$

		resultTable.addContainerProperty(
				TreePropertyName.propertyname, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.propertyname, Messages.getString("MarkupResultPanel.propertyName")); //$NON-NLS-1$

		resultTable.addContainerProperty(
				TreePropertyName.propertyvalue, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.propertyvalue, Messages.getString("MarkupResultPanel.propertyValue")); //$NON-NLS-1$
		
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, Messages.getString("MarkupResultPanel.frequency")); //$NON-NLS-1$
		resultTable.addContainerProperty(
				TreePropertyName.visible, AbstractComponent.class, null);
		resultTable.setColumnHeader(TreePropertyName.visible, Messages.getString("MarkupResultPanel.visibleInKwic")); //$NON-NLS-1$
		
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

	public void setQueryResult(QueryResult queryResult) throws Exception {
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
				MessageFormat.format(Messages.getString("MarkupResultPanel.totalCount"), tagDefinitions.size())); //$NON-NLS-1$
		resultTable.setColumnFooter(
				TreePropertyName.frequency,
				MessageFormat.format(Messages.getString("MarkupResultPanel.totalFrequency"), totalFreq)); //$NON-NLS-1$
		
	}
	
	@SuppressWarnings("unchecked")
	private int setQueryResultGroupedByTagInstance(
			QueryResult queryResult,
			Set<String> tagDefinitions) throws Exception {
		
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
			Set<String> tagDefinitions) throws Exception {
		
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
	private void addFlatTagQueryResultRow(TagQueryResultRow row) throws Exception {
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
				throws Exception {
		//TODO: use expand
		String tagDefinitionId = row.getTagDefinitionId();
		String markupCollectionsId = row.getMarkupCollectionId();
		SourceDocument sourceDocument = 
				repository.getSourceDocument(row.getSourceDocumentId());
		
		String tagDefinitionItemID = 
				row.getTagDefinitionId();// + "#" + row.getTagDefinitionVersion(); TODO: as long as we don't show the version, a differentiation is more confusing then helpful here

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
		
		final String sourceDocumentItemID = tagDefinitionId+ "@" + sourceDocument; //$NON-NLS-1$
		
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
		
		final String umcItemID = sourceDocumentItemID + "@" + markupCollectionsId; //$NON-NLS-1$
		
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
			} catch (Exception e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("MarkupResultPanel.errorShowingKwicResults"), e); //$NON-NLS-1$
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
