package de.catma.ui.visualizer.vega;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.util.IDGenerator;

public class VegaView extends HorizontalSplitPanel implements ClosableTab {
	
	private static String CATMA_QUERY_URL = "CATMA_QUERY_URL";
	
	private JSONQueryResultRequestHandler queryResultRequestHandler;
	private Vega vega;
	private TextArea specEditor;
	private Button btUpdate;

	public VegaView(QueryResult queryResult, Repository repository) {
		
		String queryResultId = new IDGenerator().generate();
		String queryResultPath = "queryresult/"+ queryResultId+".json";
		String queryResultUrl = RepositoryPropertyKey.BaseURL.getValue() + queryResultPath;
		
		this.queryResultRequestHandler = new JSONQueryResultRequestHandler(queryResult, repository, queryResultPath);
		
		VaadinSession.getCurrent().addRequestHandler(queryResultRequestHandler);
		
		initComponents(queryResultUrl);
		initActions(queryResultUrl);
	}

	private void initActions(final String queryResultUrl) {
		btUpdate.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				
				String spec = specEditor.getValue();
				if ((spec == null) || spec.trim().isEmpty()) {
					Notification.show("Info", "Vega Specification must not be empty!", Type.TRAY_NOTIFICATION);
				}
				else {
					spec = spec.replaceAll(Pattern.quote(CATMA_QUERY_URL), queryResultUrl);
					vega.setVegaSpec(spec);
				}
			}
		});
	}

	private void initComponents(String queryResultUrl) {
		setSizeFull();
		
		VerticalLayout leftPanel = new VerticalLayout();
		leftPanel.setSpacing(true);
		leftPanel.setMargin(true);
		
		leftPanel.setSizeFull();
		addComponent(leftPanel);
		
		TextField queryResultUrlField = new TextField(
				MessageFormat.format(
					"data URL of your query result or use {0} placeholder", 
					CATMA_QUERY_URL), queryResultUrl);
		
		queryResultUrlField.setReadOnly(true);
		queryResultUrlField.setWidth("100%");
		
		leftPanel.addComponent(queryResultUrlField);
		
		specEditor = new TextArea("Vega Specification");
		specEditor.setSizeFull();
		leftPanel.addComponent(specEditor);
		leftPanel.setExpandRatio(specEditor, 1f);
		
		
		btUpdate = new Button("Update");
		leftPanel.addComponent(btUpdate);
		leftPanel.setComponentAlignment(btUpdate, Alignment.BOTTOM_RIGHT);
		
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
