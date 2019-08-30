package de.catma.ui.analyzenew.visualization.kwic;

import java.util.HashSet;

import com.google.common.cache.LoadingCache;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.visualization.ExpansionListener;
import de.catma.ui.analyzenew.visualization.Visualisation;
import de.catma.ui.analyzer.RelevantUserMarkupCollectionProvider;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;


public class KwicPanelNew extends VerticalLayout implements Visualisation {
	private enum ColumnId {
		COLLECION_NAME, TAG, PROPERTY_NAME, PROPERTY_VALUE,
		;
	}

	private Grid<QueryResultRow> kwicGrid;
	private ActionGridComponent<Grid<QueryResultRow>> kwicGridComponent;
	private ListDataProvider<QueryResultRow> kwicDataProvider;
	private KwicItemHandler kwicItemHandler;

	private Repository project;
	private IconButton btExpandCompress;
	private boolean expanded = false;
	private ExpansionListener expansionListener;

	public KwicPanelNew(Repository project, LoadingCache<String, KwicProvider> kwicProviderCache ) {
		this.kwicItemHandler = new KwicItemHandler(kwicProviderCache);
		initComponents();
		initActions();
	}

	private void initActions() {
		ContextMenu moreOptionsMenu = kwicGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		//TODO:
//		moreOptionsMenu.addItem("Annotate all selected rows", clickEvent -> handleAnnotateAllRequest());
//		moreOptionsMenu.addItem("Annotate single row", clickEvent -> handleAnnotateSingleRequest());
//		moreOptionsMenu.addItem("Export Visualisation", clickEvent -> handleExportVisualizationRequest());
		
		kwicGridComponent.setSearchFilterProvider(new SearchFilterProvider<QueryResultRow>() {
			@Override
			public SerializablePredicate<QueryResultRow> createSearchFilter(String searchInput) {
				return (row) -> kwicItemHandler.containsSearchInput(row, searchInput);
			}
		});
		
		btExpandCompress.addClickListener(clickEvent -> handleMaxMinRequest());
	}

	private void handleMaxMinRequest() {
		expanded = !expanded;
		
		if (expanded) {
			btExpandCompress.setIcon(VaadinIcons.COMPRESS_SQUARE);
			if (expansionListener != null) {
				expansionListener.expand();
			}
		}
		else {
			btExpandCompress.setIcon(VaadinIcons.EXPAND_SQUARE);
			if (expansionListener != null) {
				expansionListener.compress();
			}
		}
	}

	private void initComponents() {
		setSizeFull();
		setMargin(false);
		setSpacing(false);
		
		btExpandCompress = new IconButton(VaadinIcons.EXPAND_SQUARE);
//		addComponent(btExpandCompress);
		btExpandCompress.setVisible(false);
//		
//		setComponentAlignment(btExpandCompress, Alignment.TOP_RIGHT);
//		
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
			.setStyleGenerator(row -> kwicItemHandler.getKeywordStyle(row))
			.setDescriptionGenerator(row -> kwicItemHandler.getKeywordDescription(row));

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
}
