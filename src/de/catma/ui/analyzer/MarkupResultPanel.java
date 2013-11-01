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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.ThemeResource;
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
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
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
import de.catma.tag.TagDefinition;
import de.catma.ui.HierarchicalExcelExport;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class MarkupResultPanel extends VerticalLayout {
	
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
	private CheckBox cbFlatTable;
	
	public MarkupResultPanel(
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
		cbFlatTable.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				QueryResultRowArray queryResult = new QueryResultRowArray();

				for (Object itemId : resultTable.getItemIds()) {
					if (itemId instanceof RowWrapper) {
						queryResult.add(((RowWrapper)itemId).row);
					}
					else {
						collectQueryResultRows(queryResult, itemId);
					}
				}		

				try {
					setQueryResult(queryResult);
				} catch (IOException e) {
					((CatmaApplication)getApplication()).showAndLogError(
							"error converting Query Result!", e);
				}
			}
		});
		btDist.addListener(new ClickListener() {
			
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
		
		btUntagResults.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				untagResults();
			}
		});
		
		btKwicExcelExport.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
            	ExcelExport excelExport = 
            			new HierarchicalExcelExport(kwicPanel.getKwicTable(), 
            					"CATMA Query Result Kwic");
                excelExport.excludeCollapsedColumns();
                excelExport.setReportTitle("CATMA Query Result Kwic");
                excelExport.export();
			}
		});

		btResultExcelExport.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
            	ExcelExport excelExport = 
            			new HierarchicalExcelExport(resultTable, "CATMA Query Result");
                excelExport.excludeCollapsedColumns();
                excelExport.setReportTitle("CATMA Query Result");
                excelExport.export();
			}
		});
	}
	
	private void collectQueryResultRows(QueryResultRowArray queryResult, Object startItemId) {
		Collection<?> children = resultTable.getChildren(startItemId);
		for (Object itemId : children) {
			if (itemId instanceof RowWrapper) {
				queryResult.add(((RowWrapper)itemId).row);
			}
			else {
				collectQueryResultRows(queryResult, itemId);
			}
		}
	}

	private void untagResults() {
		final Set<QueryResultRow> selection = kwicPanel.getSelection();
		if ((selection != null) && !selection.isEmpty()) {
			ConfirmDialog.show(getApplication().getMainWindow(), 
					"Remove Tag Instances", 
					"Do you want to remove the selected Tag Instances?", 
					"Yes", "No", new ConfirmDialog.Listener() {
				public void onClose(ConfirmDialog dialog) {
					if (dialog.isConfirmed()) {
						try {
							UserMarkupCollectionManager umcManager = 
									new UserMarkupCollectionManager(repository);
							for (QueryResultRow row : selection) {
								TagQueryResultRow tagRow = (TagQueryResultRow)row;
								if (!umcManager.contains(tagRow.getMarkupCollectionId())) {
									umcManager.add(
										repository.getUserMarkupCollection(
											new UserMarkupCollectionReference(
													tagRow.getMarkupCollectionId(), 
													new ContentInfoSet())));
								}
								umcManager.removeTagInstance(tagRow.getTagInstanceId());
							}
						} catch (IOException e) {
							((CatmaApplication)getApplication()).showAndLogError(
									"Error untagging search results!", e);
						}
					}
				}

			});

		}
		else {
			getWindow().showNotification(
					"Information", 
					"Please select one or more rows in the Kwic view first!",
					Notification.TYPE_TRAY_NOTIFICATION);
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
		
		HashMap<String, UserMarkupCollection> umcCache = new HashMap<String, UserMarkupCollection>();
		
		for(TagQueryResultRow row : rows) {
			try {
				if (!umcCache.containsKey(row.getMarkupCollectionId())) {
					SourceDocument sd = repository.getSourceDocument(row.getSourceDocumentId());
					UserMarkupCollectionReference umcRef = 
							sd.getUserMarkupCollectionReference(row.getMarkupCollectionId());
					UserMarkupCollection umc = repository.getUserMarkupCollection(umcRef);
					umcCache.put(umc.getId(), umc);
				}
				UserMarkupCollection umc = umcCache.get(row.getMarkupCollectionId());
				TagDefinition td = umc.getTagLibrary().getTagDefinition(row.getTagDefinitionId());
				String tagPath = umc.getTagLibrary().getTagPath(td);
	
				if (!tagQueryResultsByTagDefPath.containsKey(tagPath)) {
					tagQueryResultsByTagDefPath.put(tagPath, new TagQueryResult(tagPath));
				}
				
				TagQueryResult tagQueryResult = tagQueryResultsByTagDefPath.get(tagPath);
				tagQueryResult.add(row);
			}
			catch (IOException ioe) {
				((CatmaApplication)getApplication()).showAndLogError(
						"Error preparing markup results!",
						ioe);
			}
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
		resultTable.setColumnHeader(TreePropertyName.caption, "Tag Definition");
		
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
		resultTable.setPageLength(10); //TODO: config
		resultTable.setSizeFull();
		resultTable.setColumnCollapsingAllowed(true);
		resultTable.setColumnCollapsible(TreePropertyName.caption, false);
		resultTable.setColumnCollapsible(TreePropertyName.sourcedocument, true);
		resultTable.setColumnCollapsible(TreePropertyName.markupcollection, true);
		resultTable.setColumnCollapsible(TreePropertyName.phrase, true);
		resultTable.setColumnCollapsible(TreePropertyName.propertyname, true);
		resultTable.setColumnCollapsible(TreePropertyName.propertyvalue, true);
		resultTable.setColumnCollapsible(TreePropertyName.frequency, false);
		resultTable.setColumnCollapsible(TreePropertyName.visible, false);
		
		//TODO: a description generator that shows the version of a Tag
//		resultTable.setItemDescriptionGenerator(generator);
		
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
		
		btResultExcelExport = new Button();
		btResultExcelExport.setIcon(new ThemeResource("../images/table-excel.png"));
		btResultExcelExport.setDescription(
				"Export all Query result data as an Excel spreadsheet.");
		buttonPanel.addComponent(btResultExcelExport);
		
		cbFlatTable = new CheckBox("flat table", false);
		cbFlatTable.setImmediate(true);
		
		buttonPanel.addComponent(cbFlatTable);
		buttonPanel.setComponentAlignment(cbFlatTable, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(cbFlatTable, 1f);
		
		btSelectAll = new Button("Select all for Kwic");
		
		buttonPanel.addComponent(btSelectAll);
		buttonPanel.setComponentAlignment(btSelectAll, Alignment.MIDDLE_RIGHT);
//		buttonPanel.setExpandRatio(btSelectAll, 1f);
		btDeselectAll = new Button("Deselect all for Kwic");
		buttonPanel.addComponent(btDeselectAll);
		buttonPanel.setComponentAlignment(btDeselectAll, Alignment.MIDDLE_RIGHT);
		
		leftComponent.addComponent(buttonPanel);

		splitPanel.addComponent(leftComponent);
		
		VerticalLayout rightComponent = new VerticalLayout();
		rightComponent.setSpacing(true);
		rightComponent.setSizeFull();
		
		this.kwicPanel = 
				new KwicPanel(
					repository, relevantUserMarkupCollectionProvider,  true);
		rightComponent.addComponent(kwicPanel);
		rightComponent.setExpandRatio(kwicPanel, 1f);
		
		HorizontalLayout kwicButtonPanel = new HorizontalLayout();
		kwicButtonPanel.setSpacing(true);
		kwicButtonPanel.setWidth("100%");
		
		btKwicExcelExport = new Button();
		btKwicExcelExport.setIcon(new ThemeResource("../images/table-excel.png"));
		btKwicExcelExport.setDescription(
				"Export all Query result data as an Excel spreadsheet.");
		kwicButtonPanel.addComponent(btKwicExcelExport);
		kwicButtonPanel.setComponentAlignment(
				btKwicExcelExport, Alignment.MIDDLE_LEFT);
		
		btUntagResults = new Button("Untag selected Kwics");
		kwicButtonPanel.addComponent(btUntagResults);
		kwicButtonPanel.setComponentAlignment(btUntagResults, Alignment.MIDDLE_RIGHT);
		kwicButtonPanel.setExpandRatio(btUntagResults, 1f);
		
		Label helpLabel = new Label();
		helpLabel.setIcon(new ClassResource(
				"ui/resources/icon-help.gif", 
				getApplication()));
		helpLabel.setWidth("20px");
		
		helpLabel.setDescription(
				"<h3>Hints</h3>" +
				"<h4>Tagging search results</h4>" +
				"You can tag the search results in the Kwic-view: " +
				"<p>First select one or more rows and then drag the desired " +
				"Tag from the Tag Manager over the Kwic-results.</p>" +
				"<h4>Take a closer look</h4>" +
				"You can jump to the location in the full text by double " +
				"clicking on a row in the Kwic-view.");
		kwicButtonPanel.addComponent(helpLabel);

		kwicButtonPanel.setComponentAlignment(helpLabel, Alignment.MIDDLE_RIGHT);
		
		rightComponent.addComponent(kwicButtonPanel);
		rightComponent.setComponentAlignment(kwicButtonPanel, Alignment.MIDDLE_RIGHT);
		
		splitPanel.addComponent(rightComponent);
		
		addComponent(splitPanel);
	}
	
	public void setQueryResult(QueryResult queryResult) throws IOException {
		kwicPanel.clear();
		resultTable.removeAllItems();

		int totalFreq = 0;
	
		HashMap<String, UserMarkupCollection> loadedUserMarkupCollections =
				new HashMap<String, UserMarkupCollection>();
		Set<String> tagDefinitions = new HashSet<String>();
		boolean displayProperties = false;
		
		if (!(queryResult instanceof GroupedQueryResultSet)) { // performance opt for Wordlists which are freqency based GroupedQueryResultSets
																// and we want to avoid expensive iteration
			for (QueryResultRow row : queryResult) {
				if (row instanceof TagQueryResultRow) {
					TagQueryResultRow tRow = (TagQueryResultRow)row;
					tagDefinitions.add(tRow.getTagDefinitionId());
					if (cbFlatTable.booleanValue()) {
						addFlatTagQueryResultRow(tRow, loadedUserMarkupCollections);
					}
					else {
						addTagQueryResultRow(tRow, loadedUserMarkupCollections);
					}
					if (!displayProperties && (tRow.getPropertyDefinitionId() != null)) {
						displayProperties = true;
					}
					totalFreq++;
				}
			}
		}
		resultTable.setColumnCollapsed(TreePropertyName.sourcedocument, !cbFlatTable.booleanValue());
		resultTable.setColumnCollapsed(TreePropertyName.markupcollection, !cbFlatTable.booleanValue());
		resultTable.setColumnCollapsed(TreePropertyName.phrase, !cbFlatTable.booleanValue());
		
		resultTable.setColumnCollapsed(TreePropertyName.propertyname, !displayProperties);
		resultTable.setColumnCollapsed(TreePropertyName.propertyvalue, !displayProperties);
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, 
				"Total count: " + tagDefinitions.size());
		resultTable.setColumnFooter(
				TreePropertyName.frequency, "Total frequency: " + totalFreq);
		
	}
	
	private void addFlatTagQueryResultRow(TagQueryResultRow row,
			HashMap<String, UserMarkupCollection> loadedUserMarkupCollections) throws IOException {
		String tagDefinitionId = row.getTagDefinitionId();
		String markupCollectionsId = row.getMarkupCollectionId();
		SourceDocument sourceDocument = 
				repository.getSourceDocument(row.getSourceDocumentId());
		
		if (!loadedUserMarkupCollections.containsKey(markupCollectionsId)) {
			UserMarkupCollectionReference userMarkupCollRef = 
				sourceDocument.getUserMarkupCollectionReference(
						markupCollectionsId);
		
			loadedUserMarkupCollections.put(
					markupCollectionsId,
					repository.getUserMarkupCollection(userMarkupCollRef));
		}
		
		UserMarkupCollection umc = 
				loadedUserMarkupCollections.get(markupCollectionsId);
		
		TagDefinition tagDefinition = 
				umc.getTagLibrary().getTagDefinition(tagDefinitionId);
		
		RowWrapper wrapper = new RowWrapper(row);
		resultTable.addItem(
			new Object[] {
				umc.getTagLibrary().getTagPath(tagDefinition),
				sourceDocument.toString(),
				umc.getName(),
				row.getPhrase(),
				row.getPropertyDefinitionId()==null?"":
					tagDefinition.getPropertyDefinition(
							row.getPropertyDefinitionId()).getName(),
				row.getPropertyDefinitionId()==null?"":row.getPropertyValue(),
				1,
				createCheckbox(
						new TagQueryResultRowTreeEntrySelectionHandler(row))
			}, wrapper);
		
		resultTable.setChildrenAllowed(wrapper, false);
	}

	private void addTagQueryResultRow(
		final TagQueryResultRow row, 
		Map<String,UserMarkupCollection> loadedUserMarkupCollections) 
				throws IOException {
		
		String tagDefinitionId = row.getTagDefinitionId();
		String markupCollectionsId = row.getMarkupCollectionId();
		SourceDocument sourceDocument = 
				repository.getSourceDocument(row.getSourceDocumentId());
		
		if (!loadedUserMarkupCollections.containsKey(markupCollectionsId)) {
			UserMarkupCollectionReference userMarkupCollRef = 
				sourceDocument.getUserMarkupCollectionReference(
						markupCollectionsId);
		
			loadedUserMarkupCollections.put(
					markupCollectionsId,
					repository.getUserMarkupCollection(userMarkupCollRef));
		}
		
		UserMarkupCollection umc = 
				loadedUserMarkupCollections.get(markupCollectionsId);
		
		TagDefinition tagDefinition = 
				umc.getTagLibrary().getTagDefinition(tagDefinitionId);
		String tagDefinitionItemID = 
				tagDefinition.getUuid() + "#" + tagDefinition.getVersion();
		if (!resultTable.containsId(tagDefinitionItemID)) {
			resultTable.addItem(
				new Object[]{
						umc.getTagLibrary().getTagPath(tagDefinition),
						"",
						"",
						"",
						"",
						"",
						0,
						createCheckbox(
							new TagDefinitionTreeEntrySelectionHandler(
								resultTable, tagDefinitionItemID))
				},
				tagDefinitionItemID);
		}

		Property tagDefFreqProperty = 
				resultTable.getItem(tagDefinitionItemID).getItemProperty(
						TreePropertyName.frequency);
		tagDefFreqProperty.setValue(((Integer)tagDefFreqProperty.getValue())+1);
		
		final String sourceDocumentItemID = tagDefinitionId+ "@" + sourceDocument;
		
		if (!resultTable.containsId(sourceDocumentItemID)) {
		
			resultTable.addItem(
				new Object[] {
						sourceDocument.toString(),
						"",
						"",
						"",
						"",
						"",
						0,
						createCheckbox(
							new SourceDocumentTreeEntrySelectionHandler(
									resultTable, sourceDocumentItemID))
				}, sourceDocumentItemID);
			resultTable.setParent(sourceDocumentItemID, tagDefinitionItemID);
		}
		
		Property sourceDocFreqProperty = 
				resultTable.getItem(sourceDocumentItemID).getItemProperty(
						TreePropertyName.frequency);
		sourceDocFreqProperty.setValue(
				((Integer)sourceDocFreqProperty.getValue())+1);
		
		final String umcItemID = sourceDocumentItemID + "@" + umc.getId();
		
		if (!resultTable.containsId(umcItemID)) {
			resultTable.addItem(
				new Object[] {
					umc.getName(),
					"",
					"",
					"",
					"",
					"",
					0,
					createCheckbox(
						new UmcTreeEntrySelectionHandler(resultTable, umcItemID))
				}, umcItemID);
			resultTable.setParent(umcItemID, sourceDocumentItemID);
		}
		
		Property userMarkupCollFreqProperty = 
				resultTable.getItem(umcItemID).getItemProperty(
						TreePropertyName.frequency);
		userMarkupCollFreqProperty.setValue(
				((Integer)userMarkupCollFreqProperty.getValue())+1);
		RowWrapper wrapper = new RowWrapper(row);
		resultTable.addItem(
			new Object[] {
				row.getPhrase(),
				sourceDocument.toString(),
				umc.getName(),
				row.getPhrase(),
				row.getPropertyDefinitionId()==null?"":
					tagDefinition.getPropertyDefinition(
							row.getPropertyDefinitionId()).getName(),
				row.getPropertyDefinitionId()==null?"":row.getPropertyValue(),
				1,
				createCheckbox(
						new TagQueryResultRowTreeEntrySelectionHandler(row))
			}, wrapper);
		
		resultTable.setParent(wrapper, umcItemID);
		resultTable.setChildrenAllowed(wrapper, false);
	}

	private CheckBox createCheckbox(
			final TreeEntrySelectionHandler treeEntrySelectionHandler) {
		final CheckBox cbShowInKwicView = new CheckBox();
		cbShowInKwicView.setImmediate(true);
		cbShowInKwicView.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				boolean selected = 
						cbShowInKwicView.booleanValue();

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
				((CatmaApplication)getApplication()).showAndLogError(
					"Error showing KWIC results!", e);
			}
		}
		else {
			kwicPanel.removeQueryResultRows(queryResult);
		}
		
	}
}
