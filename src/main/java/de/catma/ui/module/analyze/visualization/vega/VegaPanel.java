package de.catma.ui.module.analyze.visualization.vega;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.properties.CATMAPropertyKey;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.ui.CatmaApplication;
import de.catma.ui.CatmaApplicationServlet.DelegateRequestHandler;
import de.catma.ui.component.IconButton;
import de.catma.ui.module.analyze.QueryOptionsProvider;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualization;
import de.catma.ui.module.analyze.visualization.kwic.KwicPanel;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;

public class VegaPanel extends HorizontalSplitPanel implements Visualization {
	private static final String CATMA_QUERY_URL = "CATMA_QUERY_URL";

	private final String vegaViewId;
	private final String queryResultUrl;
	private final JSONQueryResultRequestHandler queryResultRequestHandler;
	private final Project project;

	private DisplaySettingHandler displaySettingsHandler;

	private Vega vega;
	private TextArea specEditor;
	private Button btUpdate;
	private VegaHelpWindow vegaHelpWindow = new VegaHelpWindow();
	private Button btHelp;
	private KwicPanel kwicPanel;
	private IconButton btExpandCompressTopLeft;
	private IconButton btShowCode;
	private IconButton btHideCode;
	private IconButton btExpandCompressRight;
	private ExpansionListener expansionListener;
	private boolean expanded = false;
	private VerticalSplitPanel leftSplitPanel;
	private CheckBox cbPublicExposure;

	public VegaPanel(
			EventBus eventBus,
			Project project,
			LoadingCache<String, KwicProvider> kwicProviderCache,
			QueryOptionsProvider queryOptionsProvider, 
			DisplaySettingHandler displaySettingsHandler
	) {
		this.vegaViewId = new IDGenerator().generate().toLowerCase();

		String queryResultPath = this.vegaViewId + "/queryresult/selection.json";
		this.queryResultUrl = CATMAPropertyKey.BASE_URL.getValue() + queryResultPath;

		this.queryResultRequestHandler = new JSONQueryResultRequestHandler(
				queryOptionsProvider, queryResultPath, this.vegaViewId
		);
		VaadinSession.getCurrent().addRequestHandler(this.queryResultRequestHandler);

		this.project = project;
		this.displaySettingsHandler = displaySettingsHandler;

		initComponents(eventBus, kwicProviderCache);
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
		
		btShowCode.addClickListener(event -> toggleSpecView(true));
		btHideCode.addClickListener(event -> toggleSpecView(false));
		
		btExpandCompressTopLeft.addClickListener(clickEvent -> handleMaxMinRequest());
		btExpandCompressRight.addClickListener(clickEvent -> handleMaxMinRequest());
		
		kwicPanel.setExpansionListener(new ExpansionListener() {
			
			@Override
			public void expand() {
				toggleKwicVisible(true);
			}
			
			@Override
			public void compress() {
				toggleKwicVisible(false);
			}
		});
		
		cbPublicExposure.addValueChangeListener(event -> handlePublicExposureValueChange(event));

	}

	private void handlePublicExposureValueChange(ValueChangeEvent<Boolean> event) {

		if (event.getValue()) {
			VaadinSession.getCurrent().removeRequestHandler(queryResultRequestHandler);
			for (RequestHandler handler : VaadinService.getCurrent().getRequestHandlers()) {
				if (handler instanceof DelegateRequestHandler) {
					((DelegateRequestHandler) handler).add(queryResultRequestHandler);
					break;
				}
			}
			
			Notification.show(
				"Info", 
				"Please keep in mind that your selection is now publicly accessible for everyone who knows the URL "
				+ "until you uncheck this box or until you close this analyze session!",
				Type.ERROR_MESSAGE);
		}
		else {
			
			if (!VaadinSession.getCurrent().getRequestHandlers().contains(queryResultRequestHandler)) {
				VaadinSession.getCurrent().addRequestHandler(queryResultRequestHandler);
			}

			for (RequestHandler handler : VaadinService.getCurrent().getRequestHandlers()) {
				if (handler instanceof DelegateRequestHandler) {
					((DelegateRequestHandler) handler).remove(queryResultRequestHandler);
					break;
				}
			}
		}
	}


	private void toggleKwicVisible(boolean visible) {
		if (visible) {
			leftSplitPanel.setSplitPosition(50);
		}
		else {
			leftSplitPanel.setSplitPosition(100);
		}
	}


	private void toggleSpecView(boolean specViewVisible) {
		if (specViewVisible) {
			setSplitPosition(50);
		}
		else {
			setSplitPosition(100);
		}
		btShowCode.setVisible(!specViewVisible);
		btHideCode.setVisible(specViewVisible);
		btExpandCompressTopLeft.setVisible(!specViewVisible);
		btExpandCompressRight.setVisible(specViewVisible);
	}


	private void handleScriptUpdate(boolean changeDisplaySettingHandler) {
		if (changeDisplaySettingHandler) {
			displaySettingsHandler = (displaySetting, vegaPanel) -> {}; // noop handler
		}
		
		String spec = specEditor.getValue();
		if ((spec == null) || spec.trim().isEmpty()) {
			Notification.show("Info", "Vega specification must not be empty!", Type.TRAY_NOTIFICATION);
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
				((CatmaApplication) UI.getCurrent()).showAndLogError("Error updating Vega visualization", e);
			}
		}
	}


	private void handleVegaValueChange(QueryResult rows) {
		kwicPanel.addQueryResultRows(rows);
		kwicPanel.expand();
	}

	private void setQueryUrl(ObjectNode dataNode) throws UnsupportedEncodingException {
		if (dataNode.has("url")) {
			String catmaQuery = dataNode.get("url").asText();

			if (catmaQuery.startsWith(CATMA_QUERY_URL)) {
				if (catmaQuery.equals(CATMA_QUERY_URL)) {
					dataNode.set("url", new TextNode(queryResultUrl));
				}
				else {
					catmaQuery = catmaQuery.substring(CATMA_QUERY_URL.length() + 1, catmaQuery.length() - 1);
					catmaQuery = URLEncoder.encode(catmaQuery, "UTF-8");
					String url = CATMAPropertyKey.BASE_URL.getValue() + vegaViewId + "/query/" + catmaQuery;
					dataNode.set("url", new TextNode(url));
				}
			}
		}
	}

	private void initComponents(
			EventBus eventBus, LoadingCache<String, KwicProvider> kwicProviderCache) {
		setSizeFull();
		setSplitPosition(100);
		
		leftSplitPanel = new VerticalSplitPanel();
		addComponent(leftSplitPanel);
		
		HorizontalLayout leftTopPanel = new HorizontalLayout();
		leftTopPanel.setSizeFull();
		leftSplitPanel.addComponent(leftTopPanel);		
		leftSplitPanel.setSplitPosition(100);
		
		this.vega = new Vega();
		this.vega.addStyleName("catma-embedded-vega");
		leftTopPanel.addComponent(this.vega);
		leftTopPanel.setExpandRatio(this.vega, 1f);
		
		btShowCode = new IconButton(VaadinIcons.CODE);
		leftTopPanel.addComponent(btShowCode);
		
		btExpandCompressTopLeft = new IconButton(VaadinIcons.EXPAND_SQUARE);
		leftTopPanel.addComponent(btExpandCompressTopLeft);
		
		VerticalLayout leftBottomPanel = new VerticalLayout();
		leftBottomPanel.setMargin(false);
		leftBottomPanel.setSizeFull();
		leftSplitPanel.addComponent(leftBottomPanel);
		
		kwicPanel = new KwicPanel(eventBus, project, kwicProviderCache);
		leftBottomPanel.addComponent(kwicPanel);
		kwicPanel.setExpandResource(VaadinIcons.ANGLE_DOUBLE_UP);
		kwicPanel.setCompressResource(VaadinIcons.ANGLE_DOUBLE_DOWN);
		kwicPanel.setBtnClearSelectedRowsVisible(true);
		
		VerticalLayout codePanel = new VerticalLayout();
		codePanel.setSizeFull();
		addComponent(codePanel);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		codePanel.addComponent(buttonPanel);
		buttonPanel.setMargin(false);
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		
		btHideCode = new IconButton(VaadinIcons.ANGLE_DOUBLE_RIGHT);
		buttonPanel.addComponent(btHideCode);
		buttonPanel.setComponentAlignment(btHideCode, Alignment.TOP_LEFT);
		buttonPanel.setExpandRatio(btHideCode, 1f);
		
		btHelp = new IconButton(VaadinIcons.QUESTION_CIRCLE);
		buttonPanel.addComponent(btHelp);
		buttonPanel.setComponentAlignment(btHelp, Alignment.TOP_RIGHT);
		
		btUpdate = new IconButton(VaadinIcons.REFRESH);
		buttonPanel.addComponent(btUpdate);
		buttonPanel.setComponentAlignment(btUpdate, Alignment.TOP_RIGHT);
		
		
		btExpandCompressRight = new IconButton(VaadinIcons.EXPAND_SQUARE);
		buttonPanel.addComponent(btExpandCompressRight);
		buttonPanel.setComponentAlignment(btExpandCompressRight, Alignment.TOP_RIGHT);
		
		VerticalLayout queryResultInfoPanel = new VerticalLayout();
		queryResultInfoPanel.setWidth("100%");
		queryResultInfoPanel.setSpacing(true);
		codePanel.addComponent(queryResultInfoPanel);
		
		TextField queryResultUrlField = new TextField(
				MessageFormat.format(
					"Data URL of your selection or use {0} placeholder for custom queries", 
					CATMA_QUERY_URL), queryResultUrl);
		
		queryResultUrlField.setReadOnly(true);
		queryResultUrlField.setWidth("100%");
		queryResultInfoPanel.addComponent(queryResultUrlField);

		cbPublicExposure = new CheckBox("Expose the selected data for access in the Vega Online Editor");
		queryResultInfoPanel.addComponent(cbPublicExposure);

		specEditor = new TextArea("Vega Specification");
		specEditor.setSizeFull();

		codePanel.addComponent(specEditor);
		codePanel.setExpandRatio(specEditor, 1f);
		
	}

	@Override
	public void close() {
		VaadinSession.getCurrent().removeRequestHandler(queryResultRequestHandler);
		for (RequestHandler handler : VaadinService.getCurrent().getRequestHandlers()) {
			if (handler instanceof DelegateRequestHandler) {
				((DelegateRequestHandler) handler).remove(queryResultRequestHandler);
				break;
			}
		}
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
		kwicPanel.removeQueryResultRows(queryResult);
		
	}

	@Override
	public void setExpansionListener(ExpansionListener expansionListener) {
		this.expansionListener = expansionListener;
	}

	@Override
	public void setSelectedQueryResultRows(Iterable<QueryResultRow> selectedRows) {
		// noop
	}

	@Override
	public void setDisplaySetting(DisplaySetting displaySetting) {
		try {
			displaySettingsHandler.handleDisplaySetting(displaySetting, this);
		} catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error changing display setting", e);
		}
	}


	public void setVegaScript(String vegaScript) {
		specEditor.setValue(vegaScript);
		handleScriptUpdate(false);
	}
	
	private void handleMaxMinRequest() {
		expanded = !expanded;
		
		if (expanded) {
			btExpandCompressTopLeft.setIcon(VaadinIcons.COMPRESS_SQUARE);
			btExpandCompressRight.setIcon(VaadinIcons.COMPRESS_SQUARE);
			if (expansionListener != null) {
				expansionListener.expand();
			}
		}
		else {
			btExpandCompressTopLeft.setIcon(VaadinIcons.EXPAND_SQUARE);
			btExpandCompressRight.setIcon(VaadinIcons.EXPAND_SQUARE);
			if (expansionListener != null) {
				expansionListener.compress();
			}
		}
	}
}
