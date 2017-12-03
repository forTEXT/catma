package de.catma.ui.visualizer.vega;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.server.FontAwesome;
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

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzer.QueryOptionsProvider;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.util.IDGenerator;

public class VegaView extends HorizontalSplitPanel implements ClosableTab {
	
	private static String CATMA_QUERY_URL = "CATMA_QUERY_URL";
	
	private JSONQueryResultRequestHandler queryResultRequestHandler;
	private Vega vega;
	private TextArea specEditor;
	private Button btUpdate;
	private VegaHelpWindow vegaHelpWindow = new VegaHelpWindow();

	private Button btHelp;

	private Button btPhraseExample;

	private Button btTagExample;

	private String vegaViewId;

	private String queryResultUrl;

	public VegaView(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider) {
		
		this.vegaViewId = new IDGenerator().generate().toLowerCase();
		String queryResultPath = vegaViewId+"/queryresult/default.json";
		this.queryResultUrl = RepositoryPropertyKey.BaseURL.getValue() + queryResultPath;

		this.queryResultRequestHandler = new JSONQueryResultRequestHandler(queryResult, queryOptionsProvider, queryResultPath, vegaViewId);
		VaadinSession.getCurrent().addRequestHandler(queryResultRequestHandler);
		
		initComponents();
		initActions();
	}

	private void initActions() {
		btUpdate.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				
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
		});
		
		btHelp.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if(vegaHelpWindow.getParent() == null){
					UI.getCurrent().addWindow(vegaHelpWindow);
				} else {
					UI.getCurrent().removeWindow(vegaHelpWindow);
				}
				
			}
		});
		
		btPhraseExample.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
		
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				
				try {
					IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(
							"/de/catma/ui/visualizer/vega/resources/phrase_dist_chart.json"), buffer);
					specEditor.setValue(buffer.toString("UTF-8"));
					
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError("error loading vega example", e);
				}				
			}
		});
		btTagExample.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
		
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				
				try {
					IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(
							"/de/catma/ui/visualizer/vega/resources/tag_dist_chart.json"), buffer);
					specEditor.setValue(buffer.toString("UTF-8"));
					
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError("error loading vega example", e);
				}				
			}
		});
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

	private void initComponents() {
		
		VerticalLayout leftPanel = new VerticalLayout();
		leftPanel.setSpacing(true);
		leftPanel.setMargin(true);
		
		leftPanel.setSizeFull();
		addComponent(leftPanel);
		
		HorizontalLayout queryResultInfoPanel = new HorizontalLayout();
		queryResultInfoPanel.setWidth("100%");
		queryResultInfoPanel.setSpacing(true);
		queryResultInfoPanel.addStyleName("vega-view-left-panel-component");

		
		leftPanel.addComponent(queryResultInfoPanel);
		
		//TODO: show query
		TextField queryResultUrlField = new TextField(
				MessageFormat.format(
					"data URL of your query result or use {0} placeholder", 
					CATMA_QUERY_URL), queryResultUrl);
		
		queryResultUrlField.setReadOnly(true);
		queryResultUrlField.setWidth("100%");
		queryResultInfoPanel.addComponent(queryResultUrlField);
		queryResultInfoPanel.setExpandRatio(queryResultUrlField, 1f);

		btHelp = new Button(FontAwesome.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$
		queryResultInfoPanel.addComponent(btHelp);
		queryResultInfoPanel.setComponentAlignment(btHelp, Alignment.TOP_LEFT);
		
		specEditor = new TextArea("Vega Specification");
		specEditor.setSizeFull();
		specEditor.addStyleName("vega-view-left-panel-component");

		leftPanel.addComponent(specEditor);
		leftPanel.setExpandRatio(specEditor, 1f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		leftPanel.addComponent(buttonPanel);
		buttonPanel.addStyleName("vega-view-left-panel-component");
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		
		btPhraseExample = new Button("Phrase distribution example");
		btPhraseExample.setDescription("A specificaton for a phrase distribution chart");
		buttonPanel.addComponent(btPhraseExample);
		buttonPanel.setComponentAlignment(btPhraseExample, Alignment.BOTTOM_RIGHT);
		buttonPanel.setExpandRatio(btPhraseExample, 1f);
		
		btTagExample = new Button("Tag distribution example");
		btTagExample.setDescription("A specificaton for a Tag distribution chart (needs a Tag based query).");
		buttonPanel.addComponent(btTagExample);
		buttonPanel.setComponentAlignment(btTagExample, Alignment.BOTTOM_RIGHT);
		
		btUpdate = new Button("Update");
		buttonPanel.addComponent(btUpdate);
		buttonPanel.setComponentAlignment(btUpdate, Alignment.BOTTOM_RIGHT);
		
		this.vega = new Vega();
		this.vega.addStyleName("catma-embedded-vega");
		
		addComponent(vega);
	}

	@Override
	public void addClickshortCuts() {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeClickshortCuts() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		VaadinSession.getCurrent().removeRequestHandler(queryResultRequestHandler);
	}

	@Override
	public String toString() {
		return "Vega";
	}
}
