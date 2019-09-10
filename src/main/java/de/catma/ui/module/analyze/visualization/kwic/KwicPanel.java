package de.catma.ui.module.analyze.visualization.kwic;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.corpus.Corpus;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.Version;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualisation;
import de.catma.ui.module.analyze.visualization.kwic.annotation.AnnotationWizard;
import de.catma.ui.module.analyze.visualization.kwic.annotation.AnnotationWizardContextKey;
import de.catma.ui.module.analyze.visualization.kwic.annotation.WizardContext;
import de.catma.util.IDGenerator;


public class KwicPanel extends VerticalLayout implements Visualisation {
	private enum ColumnId {
		COLLECION_NAME, TAG, PROPERTY_NAME, PROPERTY_VALUE,
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
	private Supplier<Corpus> corpusProvider;
	private IDGenerator idGenerator = new IDGenerator();
	private VaadinIcons expandResource = VaadinIcons.EXPAND_SQUARE;
	private VaadinIcons compressResource = VaadinIcons.COMPRESS_SQUARE;

	public KwicPanel(
			EventBus eventBus,
			Project project, 
			LoadingCache<String, KwicProvider> kwicProviderCache, 
			Supplier<Corpus> corpusProvider) {
		this.project = project;
		this.kwicItemHandler = new KwicItemHandler(project, kwicProviderCache);
		this.corpusProvider = corpusProvider;
		initComponents();
		initActions(eventBus);
	}

	private void initActions(EventBus eventBus) {
		ContextMenu moreOptionsMenu = kwicGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		moreOptionsMenu.addItem("Annotate selected rows", clickEvent -> handleAnnotateSelectedRequest(eventBus));
//		moreOptionsMenu.addItem("Export Visualisation", clickEvent -> handleExportVisualizationRequest());
		
		kwicGridComponent.setSearchFilterProvider(new SearchFilterProvider<QueryResultRow>() {
			@Override
			public SerializablePredicate<QueryResultRow> createSearchFilter(String searchInput) {
				return (row) -> kwicItemHandler.containsSearchInput(row, searchInput);
			}
		});
		
		btExpandCompress.addClickListener(clickEvent -> handleMaxMinRequest());
	}

	private void handleAnnotateSelectedRequest(EventBus eventBus) {
		
		final Set<QueryResultRow> selectedRows = kwicGrid.getSelectedItems();
		
		if (selectedRows.isEmpty()) {
			Notification.show(
				"Info", 
				"Please select one ore more rows to annotate!", 
				Type.HUMANIZED_MESSAGE);
			return;
		}
		
		Set<String> sourceDocumentId = kwicDataProvider.getItems()
			.stream()
			.map(row -> row.getSourceDocumentId()).collect(Collectors.toSet());
		
		WizardContext wizardContext = new WizardContext();
		wizardContext.put(AnnotationWizardContextKey.DOCUMENTIDS, sourceDocumentId);
		wizardContext.put(AnnotationWizardContextKey.CORPUS, corpusProvider.get());
		
		AnnotationWizard wizard = new AnnotationWizard(
				eventBus, project, wizardContext, 
				new SaveCancelListener<WizardContext>() {
		
					@Override
					public void savePressed(WizardContext result) {
						try {
							annotateSelection(selectedRows, result);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
	public void setSelectedQueryResultRow(QueryResultRow row) {
		// noop
	}
	
	@Override
	public void setDisplaySetting(DisplaySetting displaySettings) {
		// noop
	}
}
