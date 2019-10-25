package de.catma.ui.module.analyze.visualization.doubletree;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.source.KeywordInContext;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.properties.CATMAPropertyKey;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.component.IconButton;
import de.catma.ui.module.analyze.QueryOptionsProvider;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualization;
import de.catma.ui.module.analyze.visualization.kwic.KwicPanel;
import de.catma.ui.module.analyze.visualization.vega.DisplaySettingHandler;
import de.catma.ui.module.analyze.visualization.vega.JSONQueryResultRequestHandler;
import de.catma.ui.module.analyze.visualization.vega.Vega;
import de.catma.ui.module.analyze.visualization.vega.VegaHelpWindow;
import de.catma.util.IDGenerator;

public class DoubleTreePanel extends VerticalLayout implements Visualization {
	
	private static String CATMA_QUERY_URL = "CATMA_QUERY_URL";
	
	private JSONQueryResultRequestHandler queryResultRequestHandler;
	private Vega vega;
	private TextArea specEditor;
	private Button btUpdate;
	private VegaHelpWindow vegaHelpWindow = new VegaHelpWindow();

	private Button btHelp;

	private String dtViewId;

	private String queryResultUrl;

	private Project project;

	private KwicPanel kwicPanel;


	private IconButton btExpandCompressTopLeft;

	private IconButton btShowCode;

	private IconButton btHideCode;

	private IconButton btExpandCompressRight;

	private ExpansionListener expansionListener;
	
	private boolean expanded = false;

	private VerticalSplitPanel leftSplitPanel;

	private CheckBox cbPublicExposure;

	private DoubleTree doubleTree;
	
	private List<KeywordInContext> kwics;
	
	private CheckBox cbCaseSensitive;
	
	private LoadingCache<String, KwicProvider> kwicProviderCache;
	
	private int contextSize = 5;
	
	public DoubleTreePanel(EventBus eventBus, Project project, LoadingCache<String, KwicProvider> kwicProviderCache,
			QueryOptionsProvider queryOptionsProvider) {
		
		this.dtViewId = new IDGenerator().generate().toLowerCase();
		String queryResultPath = dtViewId+"/queryresult/selection.json";
		this.queryResultUrl = CATMAPropertyKey.BaseURL.getValue() + queryResultPath;
		this.kwicProviderCache = kwicProviderCache;

		this.queryResultRequestHandler = 
				new JSONQueryResultRequestHandler(
						queryOptionsProvider, queryResultPath, dtViewId);

		VaadinSession.getCurrent().addRequestHandler(queryResultRequestHandler);
		
		this.project = project;

		initComponents(eventBus, kwicProviderCache);
		//initActions();
	}
	
	private void initComponents(
			EventBus eventBus, LoadingCache<String, KwicProvider> kwicProviderCache) {
		setSizeFull();

		
		leftSplitPanel = new VerticalSplitPanel();
		addComponent(leftSplitPanel);
		
		HorizontalLayout leftTopPanel = new HorizontalLayout();
		leftTopPanel.setSizeFull();
		leftSplitPanel.addComponent(leftTopPanel);		
		leftSplitPanel.setSplitPosition(100);
		
		this.doubleTree = new DoubleTree();
		this.doubleTree.addStyleName("catma-embedded-vega");

		leftTopPanel.addComponent(this.doubleTree);
		leftTopPanel.setExpandRatio(this.doubleTree, 1f);
		

		
		VerticalLayout queryResultInfoPanel = new VerticalLayout();
		queryResultInfoPanel.setWidth("100%");
		queryResultInfoPanel.setSpacing(true);

		
		TextField queryResultUrlField = new TextField(
				MessageFormat.format(
					"Data URL of your selection or use {0} placeholder for custom queries", 
					CATMA_QUERY_URL), queryResultUrl);
		
		queryResultUrlField.setReadOnly(true);
		queryResultUrlField.setWidth("100%");
		queryResultInfoPanel.addComponent(queryResultUrlField);


	}
	
	private void initActions() {
		cbCaseSensitive.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				doubleTree.setupFromArrays(kwics, cbCaseSensitive.getValue());
			}
		});
	}

	@Override
	public void addQueryResultRows(Iterable<QueryResultRow> queryResult) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeQueryResultRows(Iterable<QueryResultRow> queryResult) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setExpansionListener(ExpansionListener expansionListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	//diese methode in vizmax panel aufrufen
	public void setSelectedQueryResultRows(List<QueryResultRow> rows) {
		List <KeywordInContext> kwics= new ArrayList<KeywordInContext>();
		
		for (QueryResultRow row : rows) {
			
			KwicProvider kwicProvider = null;
			try {
				kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			KeywordInSpanContext kwic = null;
			try {
				kwic = kwicProvider.getKwic(row.getRange(), contextSize);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		kwics.add(kwic);
			
		}
		
		doubleTree.setupFromArrays(kwics, true);
	
	}
	
	@Override
	public void setSelectedQueryResultRow(QueryResultRow row) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDisplaySetting(DisplaySetting displaySettings) {
		// TODO Auto-generated method stub

	}

}
