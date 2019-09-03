package de.catma.ui.analyzenew.visualization.vega;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzenew.QueryOptionsProvider;
import de.catma.ui.analyzenew.queryresultpanel.DisplaySetting;
import de.catma.ui.analyzenew.visualization.ExpansionListener;
import de.catma.ui.analyzenew.visualization.Visualisation;
import de.catma.ui.analyzenew.visualization.kwic.KwicPanelNew;
import de.catma.ui.analyzer.KwicPanel;
import de.catma.ui.analyzer.KwicWindow;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.analyzer.RelevantUserMarkupCollectionProvider;
import de.catma.ui.component.IconButton;
import de.catma.util.IDGenerator;

public class VegaPanel extends HorizontalSplitPanel implements Visualisation {
	
	private static String CATMA_QUERY_URL = "CATMA_QUERY_URL";
	
	private JSONQueryResultRequestHandler queryResultRequestHandler;
	private Vega vega;
	private TextArea specEditor;
	private Button btUpdate;
	private VegaHelpWindow vegaHelpWindow = new VegaHelpWindow();

	private Button btHelp;

	private String vegaViewId;

	private String queryResultUrl;

	private Repository project;

	private KwicPanelNew kwicPanel;

	private DisplaySettingHandler displaySettingsHandler;
	private DisplaySettingHandler defaultDisplaySettingHandler;

	public VegaPanel(EventBus eventBus, Repository project, LoadingCache<String, KwicProvider> kwicProviderCache, 
			Supplier<Corpus> corpusProvider, QueryOptionsProvider queryOptionsProvider, 
			DisplaySettingHandler displaySettingsHandler) {
		
		this.vegaViewId = new IDGenerator().generate().toLowerCase();
		String queryResultPath = vegaViewId+"/queryresult/default.json";
		this.queryResultUrl = RepositoryPropertyKey.BaseURL.getValue() + queryResultPath;

		this.queryResultRequestHandler = 
				new JSONQueryResultRequestHandler(
						queryOptionsProvider, queryResultPath, vegaViewId);
		VaadinSession.getCurrent().addRequestHandler(queryResultRequestHandler);
		
		this.project = project;
		this.displaySettingsHandler = displaySettingsHandler;
		this.defaultDisplaySettingHandler = displaySettingsHandler;
		initComponents(eventBus, kwicProviderCache, corpusProvider);
		initActions();
	}


	private void initActions() {
		vega.setValueChangeListener(changeEvent -> handleVegaValueChange(changeEvent.getValue()));
		
		btUpdate.addClickListener(event -> handleScriptUpdate(true));
		
		btHelp.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if(vegaHelpWindow.getParent() == null){
					UI.getCurrent().addWindow(vegaHelpWindow);
				} else {
					UI.getCurrent().removeWindow(vegaHelpWindow);
				}
				
			}
		});
	}

	private void handleScriptUpdate(boolean changeDisplaySettingHandler) {
		if (changeDisplaySettingHandler) {
			displaySettingsHandler = (displaySetting, vegaPanel) -> {}; // noop handler
		}
		
		String spec = specEditor.getValue();
		if ((spec == null) || spec.trim().isEmpty()) {
			Notification.show("Info", "Vega Specification must not be empty!", Type.TRAY_NOTIFICATION);
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			try {
				ObjectNode specNode = mapper.readValue(spec, ObjectNode.class);
				
				JsonNode dataNode = specNode.get("data");
				if (dataNode.isArray()) {
					ArrayNode dataArray = (ArrayNode)dataNode;
					for (int i=0; i<dataArray.size(); i++) {
						ObjectNode curDataNode = (ObjectNode)dataArray.get(i);
						setQueryUrl(curDataNode);
					}
				}
				else {
					setQueryUrl((ObjectNode)dataNode);
				}
				
				vega.setVegaSpec(specNode.toString());
			}
			catch (Exception e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError("error updating vega viz", e);
			}
		}
	}


	private void handleVegaValueChange(QueryResult rows) {
		
		try {
			boolean markupBased = false;
			if (!rows.iterator().hasNext()) {
				markupBased = (rows.iterator().next() instanceof TagQueryResultRow);
			}
			KwicPanel kwicPanel = 
					new KwicPanel(project, new RelevantUserMarkupCollectionProvider() {
						
						@Override
						public List<String> getRelevantUserMarkupCollectionIDs() {
							return null;
						}
						
						@Override
						public Corpus getCorpus() {
							return null;
						}
					}, markupBased);
			kwicPanel.addQueryResultRows(rows);
			new KwicWindow(
					"Test", //TODO:
					kwicPanel).show(); 
		}
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("AnalyzerView.errorAccessingRepo"), e); //$NON-NLS-1$
		}			
	}

	private void setQueryUrl(ObjectNode dataNode) throws UnsupportedEncodingException {
		if (dataNode.has("url")) {
			String catmaQuery = dataNode.get("url").asText();
					
			if (catmaQuery.startsWith(CATMA_QUERY_URL)) {
				if (catmaQuery.equals(CATMA_QUERY_URL)) {
					dataNode.set("url", new TextNode(queryResultUrl));
				}
				else {
					catmaQuery = catmaQuery.substring(CATMA_QUERY_URL.length()+1, catmaQuery.length()-1);
					catmaQuery = URLEncoder.encode(catmaQuery, "UTF-8");
					String url = RepositoryPropertyKey.BaseURL.getValue() + vegaViewId + "/query/" + catmaQuery;
					dataNode.set("url", new TextNode(url));
				}
			}
		}
	}

	private void initComponents(
			EventBus eventBus, LoadingCache<String, KwicProvider> kwicProviderCache, 
			Supplier<Corpus> corpusProvider) {
		
		VerticalSplitPanel leftSplitPanel = new VerticalSplitPanel();
		addComponent(leftSplitPanel);
		
		this.vega = new Vega();
		this.vega.addStyleName("catma-embedded-vega");
		this.vega.setSizeFull();
		
		leftSplitPanel.addComponent(vega);		
		leftSplitPanel.setSplitPosition(100);
		
		kwicPanel = new KwicPanelNew(eventBus, project, kwicProviderCache, corpusProvider);
		
		leftSplitPanel.addComponent(kwicPanel);

		
		
		VerticalLayout codePanel = new VerticalLayout();
		codePanel.setSizeFull();
		addComponent(codePanel);
		
		
		HorizontalLayout queryResultInfoPanel = new HorizontalLayout();
		queryResultInfoPanel.setWidth("100%");
		queryResultInfoPanel.setSpacing(true);
		codePanel.addComponent(queryResultInfoPanel);
		
		TextField queryResultUrlField = new TextField(
				MessageFormat.format(
					"data URL of your selection or use {0} placeholder for custom queries", 
					CATMA_QUERY_URL), queryResultUrl);
		
		queryResultUrlField.setReadOnly(true);
		queryResultUrlField.setWidth("100%");
		queryResultInfoPanel.addComponent(queryResultUrlField);
		queryResultInfoPanel.setExpandRatio(queryResultUrlField, 1f);

		btHelp = new Button(VaadinIcons.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$
		queryResultInfoPanel.addComponent(btHelp);
		queryResultInfoPanel.setComponentAlignment(btHelp, Alignment.TOP_LEFT);
		
		specEditor = new TextArea("Vega Specification");
		specEditor.setSizeFull();

		codePanel.addComponent(specEditor);
		codePanel.setExpandRatio(specEditor, 1f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		codePanel.addComponent(buttonPanel);
		
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		
		btUpdate = new IconButton(VaadinIcons.REFRESH);
		buttonPanel.addComponent(btUpdate);
		buttonPanel.setComponentAlignment(btUpdate, Alignment.BOTTOM_RIGHT);
	}

	@Override
	public void close() {
		VaadinSession.getCurrent().removeRequestHandler(queryResultRequestHandler);
	}

	@Override
	public void addQueryResultRows(Iterable<QueryResultRow> queryResult) {
		queryResultRequestHandler.addQuerResultRows(queryResult);
		vega.reloadData();
	}

	@Override
	public void removeQueryResultRows(Iterable<QueryResultRow> queryResult) {
		queryResultRequestHandler.removeQuerResultRows(queryResult);
		vega.reloadData();
	}

	@Override
	public void setExpansionListener(ExpansionListener expansionListener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSelectedQueryResultRow(QueryResultRow row) {
		// noop
	}

	@Override
	public void setDisplaySetting(DisplaySetting displaySetting) {
		try {
			displaySettingsHandler.handleDisplaySetting(displaySetting, this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void setVegaScript(String vegaScript) {
		specEditor.setValue(vegaScript);
		handleScriptUpdate(false);
	}
}
