package de.catma.ui.module.analyze.visualization.kwic;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.ItemClickListener;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.Version;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridBar;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.events.QueryResultRowInAnnotateEvent;
import de.catma.ui.module.analyze.CSVExportFlatStreamSource;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualization;
import de.catma.ui.module.analyze.visualization.kwic.annotation.AnnotationWizard;
import de.catma.ui.module.analyze.visualization.kwic.annotation.AnnotationWizardContextKey;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;


public class KwicPanel extends VerticalLayout implements Visualization {
	private enum ColumnId {
		COLLECION_NAME, TAG, PROPERTY_NAME, PROPERTY_VALUE, START_POS,
		;
	}

	private Grid<QueryResultRow> kwicGrid;
	private ActionGridComponent<Grid<QueryResultRow>> kwicGridComponent;
	private ListDataProvider<QueryResultRow> kwicDataProvider;
	private KwicItemHandler kwicItemHandler;

	private Project project;
	private IconButton btExpandCompress;
	private boolean expanded = false;
	private ExpansionListener expansionListener;
	private IDGenerator idGenerator = new IDGenerator();
	private VaadinIcons expandResource = VaadinIcons.EXPAND_SQUARE;
	private VaadinIcons compressResource = VaadinIcons.COMPRESS_SQUARE;
	private Registration defaultDoubleClickRegistration;
	private MenuItem miRemoveAnnotations;
	private IconButton btnClearSelectedRows;

	public KwicPanel(
			EventBus eventBus,
			Project project, 
			LoadingCache<String, KwicProvider> kwicProviderCache) {
		this.project = project;
		this.kwicItemHandler = new KwicItemHandler(project, kwicProviderCache);
		initComponents();
		initActions(eventBus);
	}

	private void initActions(EventBus eventBus) {
		ContextMenu moreOptionsMenu = kwicGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		moreOptionsMenu.addItem("Annotate selected rows", mi -> handleAnnotateSelectedRequest(eventBus));
		
		ActionGridBar actionBar = kwicGridComponent.getActionGridBar();
		
		actionBar.addButtonAfterSearchField(btnClearSelectedRows);
		btnClearSelectedRows.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				handleRemoveRowRequest();
				
			}
		});
			
		miRemoveAnnotations = 
			moreOptionsMenu.addItem(
				"Remove selected Annotations", mi -> handleRemoveAnnotationsRequest(eventBus));
		miRemoveAnnotations.setEnabled(false);
		
		MenuItem miExport = moreOptionsMenu.addItem("Export");
		MenuItem miCSVFlatExport = miExport.addItem("Export as CSV");
		
		StreamResource csvFlatExportResource = new StreamResource(
					new CSVExportFlatStreamSource(
						() -> getFilteredQueryResult(), 
						project, 
						kwicItemHandler.getKwicProviderCache(), 
						((BackgroundServiceProvider)UI.getCurrent())),
					"CATMA-KWIC_Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".csv");
		csvFlatExportResource.setCacheTime(0);
		csvFlatExportResource.setMIMEType("text/comma-separated-values");
		
		FileDownloader csvFlatExportFileDownloader = 
			new FileDownloader(csvFlatExportResource);
		
		csvFlatExportFileDownloader.extend(miCSVFlatExport);
		
		kwicGridComponent.setSearchFilterProvider(new SearchFilterProvider<QueryResultRow>() {
			@Override
			public SerializablePredicate<QueryResultRow> createSearchFilter(String searchInput) {
				return (row) -> kwicItemHandler.containsSearchInput(row, searchInput);
			}
		});
		
		btExpandCompress.addClickListener(clickEvent -> handleMaxMinRequest());
		defaultDoubleClickRegistration = 
		kwicGrid.addItemClickListener(clickEvent -> handleKwicItemClick(clickEvent, eventBus));
	}
	
	public QueryResultRowArray getFilteredQueryResult() {
		QueryResultRowArray result = new QueryResultRowArray();
		kwicDataProvider.fetch(
				new Query<QueryResultRow, SerializablePredicate<QueryResultRow>>())
		.forEach(row -> result.add(row));

		return result;
	}

	private void handleRemoveAnnotationsRequest(EventBus eventBus) {

		final Set<QueryResultRow> selectedRows = kwicGrid.getSelectedItems();
		
		if (selectedRows.isEmpty()) {
			Notification.show(
				"Info", 
				"Please select one or more Annotation rows!", 
				Type.HUMANIZED_MESSAGE);
			return;
		}
		
		int annotationRows = 0;
		List<AnnotationCollectionReference> annotationCollectionReferences = 
				new ArrayList<>();
		boolean resourcesMissing = false;
		boolean permissionsMissing = false;
		
		Set<String> tagInstanceIdsToBeRemoved = new HashSet<String>();
		Set<QueryResultRow> rowsToBeRemoved = new HashSet<>();
		
		try {
			for (QueryResultRow row : selectedRows) {
				if (row instanceof TagQueryResultRow) {
					annotationRows++;
					if (project.hasDocument(row.getSourceDocumentId())) {
						SourceDocument document = project.getSourceDocument(row.getSourceDocumentId());
						AnnotationCollectionReference collRef = 
							document.getUserMarkupCollectionReference(
									((TagQueryResultRow) row).getMarkupCollectionId());
						
						if (collRef != null) {
							if (collRef.isResponsable(project.getUser().getIdentifier())) {
								annotationCollectionReferences.add(collRef);
								tagInstanceIdsToBeRemoved.add(((TagQueryResultRow) row).getTagInstanceId());
								rowsToBeRemoved.add(row);
							}
							else {
								permissionsMissing = true;
							}
						}
						else {
							resourcesMissing = true;
						}
					}
					else {
						resourcesMissing = true;
					}
				}
			}
			
			if (permissionsMissing) {
				Notification.show(
					"Info", 
					"You do not have the write permission for one or more Collections referenced by your selection. Those Collections will be ignored!", 
					Type.HUMANIZED_MESSAGE);
			}
			
			if (annotationRows == 0) {
				Notification.show(
					"Info", 
					"Your selection does not contain any Annotations! Please select Annotations only!", 
					Type.HUMANIZED_MESSAGE);
				return;
			}
			
			if (annotationCollectionReferences.isEmpty()) {
				Notification.show(
					"Info", 
					"The Documents and/or Collections referenced by your selection are no longer part of the Project!", 
					Type.HUMANIZED_MESSAGE);
					return;			
			}
			
			if (resourcesMissing) {
				Notification.show(
					"Info", 
					"Some of the Documents and/or Collections referenced by your selection "
					+ "are no longer part of the Project and will be ignored, "
					+ "see columns 'Document' and 'Collection' for details!", 
					Type.HUMANIZED_MESSAGE);
			}
			
			if (annotationRows != selectedRows.size()) {
				Notification.show(
					"Info", 
					"Some rows of your selection do not represent Annotations and will be ignored, see column 'Tag' for details!", 
					Type.HUMANIZED_MESSAGE);
			}
			
			AnnotationCollectionManager collectionManager = new AnnotationCollectionManager(project);
			for (AnnotationCollectionReference ref : annotationCollectionReferences) {
				collectionManager.add(project.getUserMarkupCollection(ref));
			}
			
			collectionManager.removeTagInstance(tagInstanceIdsToBeRemoved, true);
			kwicDataProvider.getItems().removeAll(rowsToBeRemoved);
			kwicDataProvider.refreshAll();
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error deleting Annotations!", e);
		}
	}
	
	private void handleRemoveRowRequest() {
		final Set<QueryResultRow> selectedRows = kwicGrid.getSelectedItems();
		removeQueryResultRows(selectedRows);
		
	}

	private void handleKwicItemClick(ItemClick<QueryResultRow> clickEvent, EventBus eventBus) {
		if (clickEvent.getMouseEventDetails().isDoubleClick()) {
			QueryResultRow selectedRow = clickEvent.getItem();
			final String documentId = selectedRow.getSourceDocumentId();
			List<QueryResultRow> documentRows = 
					kwicDataProvider.getItems()
					.parallelStream()
					.filter(row -> row.getSourceDocumentId().equals(documentId))
					.collect(Collectors.toList());
			try {
				if (project.hasDocument(documentId)) {
					eventBus.post(new QueryResultRowInAnnotateEvent(
						documentId, selectedRow, documentRows, project));
				}
				else {
					Notification.show(
							"Info", 
							"The corresponding Document is no longer part of the Project!", 
							Type.WARNING_MESSAGE);
				}
			}
			catch (Exception e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("error accessing project data", e);
			}
		}
	}

	private void handleAnnotateSelectedRequest(EventBus eventBus) {
		
		final Set<QueryResultRow> selectedRows = kwicGrid.getSelectedItems();
		
		if (selectedRows.isEmpty()) {
			Notification.show(
				"Info", 
				"Please select one or more rows to annotate!", 
				Type.HUMANIZED_MESSAGE);
			return;
		}
		
		Set<String> documentIds = kwicDataProvider.getItems()
			.stream()
			.map(row -> row.getSourceDocumentId()).collect(Collectors.toSet());
		
		WizardContext wizardContext = new WizardContext();
		wizardContext.put(AnnotationWizardContextKey.DOCUMENTIDS, documentIds);
		
		AnnotationWizard wizard = new AnnotationWizard(
				eventBus, project, wizardContext, 
				new SaveCancelListener<WizardContext>() {
		
					@Override
					public void savePressed(WizardContext result) {
						try {
							annotateSelection(selectedRows, result);
						} catch (Exception e) {
							((ErrorHandler)UI.getCurrent()).showAndLogError(
									"error annotating selected rows", e);
						}						
					}
					
				});
		wizard.show();
		
	}

	@SuppressWarnings("unchecked")
	private void annotateSelection(Set<QueryResultRow> selectedRows, WizardContext result) throws Exception {
		List<Property> properties = (List<Property>) result.get(AnnotationWizardContextKey.PROPERTIES);
		Map<String, AnnotationCollectionReference> collectionRefsByDocId = 
				(Map<String, AnnotationCollectionReference>) result.get(AnnotationWizardContextKey.COLLECTIONREFS_BY_DOCID);
		TagDefinition tag = (TagDefinition) result.get(AnnotationWizardContextKey.TAG);
		
		AnnotationCollectionManager collectionManager = new AnnotationCollectionManager(project);
		for (AnnotationCollectionReference ref : collectionRefsByDocId.values()) {
			collectionManager.add(project.getUserMarkupCollection(ref));
		}

		for (QueryResultRow row : selectedRows) {
			AnnotationCollectionReference collectionRef = collectionRefsByDocId.get(row.getSourceDocumentId());
			
			TagInstance tagInstance = 
					new TagInstance(
						idGenerator.generate(), 
						tag.getUuid(),
						project.getUser().getIdentifier(),
			        	ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
			        	tag.getUserDefinedPropertyDefinitions(),
			        	tag.getTagsetDefinitionUuid());
			
			List<TagReference> tagReferences = new ArrayList<TagReference>();
			
			for (Property protoProp : properties) {
				tagInstance.getUserDefinedPropetyByUuid(
					protoProp.getPropertyDefinitionId()).setPropertyValueList(
							protoProp.getPropertyValueList());
			}


			Set<Range> ranges = row.getRanges();
			
			for (Range range : ranges) {
				TagReference tagReference = 
						new TagReference(tagInstance, row.getSourceDocumentId(), range, collectionRef.getId());
				tagReferences.add(tagReference);
			}
			collectionManager.addTagReferences(
					tagReferences,
					collectionRef.getId());
		}
		
		project.addAndCommitCollections(collectionRefsByDocId.values(), 
				"Auto-Committing KWIC Annotations");
	}

	private void handleMaxMinRequest() {
		expanded = !expanded;
		
		if (expanded) {
			btExpandCompress.setIcon(compressResource);
			if (expansionListener != null) {
				expansionListener.expand();
			}
		}
		else {
			btExpandCompress.setIcon(expandResource);
			if (expansionListener != null) {
				expansionListener.compress();
			}
		}
	}

	private void initComponents() {
		setSizeFull();
		setMargin(false);
		setSpacing(false);
		
		btExpandCompress = new IconButton(expandResource);
		btExpandCompress.setVisible(false);

		kwicDataProvider = new ListDataProvider<>(new HashSet<>());
		kwicGrid = new Grid<QueryResultRow>(kwicDataProvider);

		kwicGrid.setSizeFull();

		kwicGrid.addColumn(row -> kwicItemHandler.getDocumentName(row)).setCaption("Document")
			.setWidth(200)
			.setHidable(true);
		kwicGrid.addColumn(row -> kwicItemHandler.getCollectionName(row))
			.setCaption("Collection")
			.setWidth(200)
			.setId(ColumnId.COLLECION_NAME.name())
			.setHidable(true)
			.setHidden(true);
		
		Column<QueryResultRow, ?> backwardCtxColumn = 
			kwicGrid.addColumn(row -> kwicItemHandler.getBackwardContext(row))
			.setCaption("Left Context")
			.setStyleGenerator(row -> kwicItemHandler.getBackwardContextStyle(row))
			.setWidth(200);

		Column<QueryResultRow, ?> keywordColumn = kwicGrid.addColumn(row -> kwicItemHandler.getKeyword(row))
			.setCaption("Keyword")
			.setWidth(200)
			.setRenderer(new HtmlRenderer())
			.setStyleGenerator(row -> kwicItemHandler.getKeywordStyle(row))
			.setDescriptionGenerator(row -> kwicItemHandler.getKeywordDescription(row), ContentMode.HTML);

		kwicGrid.addColumn(row -> kwicItemHandler.getForwardContext(row))
			.setCaption("Right Context")
			.setStyleGenerator(row -> kwicItemHandler.getForwardContextStyle(row))
			.setWidth(200);
		
		kwicGrid.addColumn(row -> row.getRange().getStartPoint())
			.setCaption("Start Point")
			.setWidth(100)
			.setId(ColumnId.START_POS.name())
			.setHidable(true);
		kwicGrid.addColumn(row -> row.getRange().getEndPoint())
			.setCaption("End Point")
			.setWidth(100)
			.setHidable(true);

		kwicGrid.addColumn(row -> kwicItemHandler.getTagPath(row))
			.setCaption("Tag")
			.setHidable(true)
			.setHidden(true)
			.setId(ColumnId.TAG.name())
			.setWidth(200);
		
		kwicGrid.addColumn(row -> kwicItemHandler.getPropertyName(row))
			.setCaption("Property")
			.setHidable(true)
			.setHidden(true)
			.setId(ColumnId.PROPERTY_NAME.name())
			.setWidth(200);
		
		kwicGrid.addColumn(row -> kwicItemHandler.getPropertyValue(row))
			.setCaption("Value")
			.setHidable(true)
			.setHidden(true)
			.setId(ColumnId.PROPERTY_VALUE.name())
			.setWidth(200);

		kwicGrid.sort(keywordColumn);
		
		kwicGrid.getDefaultHeaderRow().getCell(keywordColumn).setStyleName("kwic-panel-keyword-header");
		kwicGrid.getDefaultHeaderRow().getCell(backwardCtxColumn).setStyleName("kwic-panel-backwardctx-header");
		
		kwicGridComponent = new ActionGridComponent<>(new Label("Keyword in context"), kwicGrid);
		kwicGridComponent.getActionGridBar().setAddBtnVisible(false);
		kwicGridComponent.getActionGridBar().addButtonRight(btExpandCompress);
		kwicGridComponent.setMargin(new MarginInfo(false, false, false, true));
		addComponent(kwicGridComponent);
		setExpandRatio(kwicGridComponent, 1f);
		
		btnClearSelectedRows = new IconButton(VaadinIcons.ERASER);
		btnClearSelectedRows.setVisible(false);
		btnClearSelectedRows.setDescription("Remove the selected rows from this list");
	}
	
	public void setBtnClearSelectedRowsVisible(boolean visible) {
		btnClearSelectedRows.setVisible(visible);
	}

	public void addQueryResultRows(Iterable<QueryResultRow> queryResult)  {	
		
		boolean showTagColumns = false;
		boolean showPropertyColumns = false;
		
		for (QueryResultRow row : queryResult) {
			if (kwicDataProvider.getItems().add(row)) {
				if (row instanceof TagQueryResultRow) {
					showTagColumns = true;
					
					if (((TagQueryResultRow) row).getPropertyDefinitionId() != null) {
						showPropertyColumns = true;
					}
				}
			}
		}

		if (showTagColumns) {
			kwicGrid.getColumn(ColumnId.COLLECION_NAME.name()).setHidden(false);
			kwicGrid.getColumn(ColumnId.TAG.name()).setHidden(false);
			if (showPropertyColumns) {
				kwicGrid.getColumn(ColumnId.PROPERTY_NAME.name()).setHidden(false);
				kwicGrid.getColumn(ColumnId.PROPERTY_VALUE.name()).setHidden(false);
			}
			miRemoveAnnotations.setEnabled(true);
		}
		
		kwicGrid.getDataProvider().refreshAll();
	}

	public void removeQueryResultRows(Iterable<QueryResultRow> queryResult) {
		for (QueryResultRow row : queryResult) {
			kwicDataProvider.getItems().remove(row);
		}
		kwicGrid.getDataProvider().refreshAll();
	}

	public void setExpansionListener(ExpansionListener expansionListener) {
		this.expansionListener = expansionListener;
		btExpandCompress.setVisible(true);
	}
	
	public void setExpandResource(VaadinIcons expandResource) {
		this.expandResource = expandResource;
		if (!expanded) {
			btExpandCompress.setIcon(expandResource);
		}
	}
	
	public void setCompressResource(VaadinIcons compressResource) {
		this.compressResource = compressResource;
		if (expanded) {
			btExpandCompress.setIcon(compressResource);
		}
	}
	
	public void expand() {
		if (!expanded) {
			handleMaxMinRequest();
		}
	}
	
	public void clear() {
		kwicDataProvider.getItems().clear();
		kwicGrid.getDataProvider().refreshAll();
	}
	
	@Override
	public void close() {
		// noop
	}
	
	@Override
	public void setSelectedQueryResultRows(Iterable<QueryResultRow> selectedRows) {
		// noop
	}
	
	@Override
	public void setDisplaySetting(DisplaySetting displaySettings) {
		// noop
	}
	
	public void addItemClickListener(ItemClickListener<QueryResultRow> itemClickListener) {
		defaultDoubleClickRegistration.remove();
		kwicGrid.addItemClickListener(itemClickListener);
	}
	
	public void setSelectedItem(QueryResultRow row) {
		kwicGrid.setSelectionMode(SelectionMode.SINGLE).select(row);
	}
	
	public void sortByStartPosAsc() {
		kwicGrid.sort(ColumnId.START_POS.name());
	}
}
