package de.catma.ui.module.analyze.visualization.doubletree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.KeywordInContext;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.IconButton;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualization;

public class DoubleTreePanel  extends VerticalLayout implements Visualization {
	
	private DoubleTree doubleTree;
	private List<KeywordInContext> kwics;
	private CheckBox cbCaseSensitive;
	private LoadingCache<String, KwicProvider> kwicProviderCache;
	private DisplaySetting displaySettings;
	private int contextSize = 5;
	private Button btExpandCompressRight;
	private ExpansionListener expansionListener;
	
	private boolean expanded = false;

	public DoubleTreePanel( LoadingCache<String, KwicProvider> kwicProviderCache) {
		this.kwicProviderCache= kwicProviderCache;
		this.kwics = new ArrayList<KeywordInContext>();
		initComponents();
		initActions();
	}

	private void initActions() {
		cbCaseSensitive.addValueChangeListener(new ValueChangeListener<Boolean>() {	
			public void valueChange(ValueChangeEvent<Boolean> event) {
				doubleTree.setupFromArrays(kwics, cbCaseSensitive.getValue());
			}
		});	
		btExpandCompressRight.addClickListener(clickEvent -> handleMaxMinRequest());	
	}

	private void initComponents() {
		setSizeFull();
		setMargin(true);
		setSpacing(true);
		
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setSpacing(true);
		headerPanel.setWidth("100%"); 
		
		cbCaseSensitive = new CheckBox("case sensitive", true); 
		
		headerPanel.addComponent(cbCaseSensitive);
		headerPanel.setComponentAlignment(cbCaseSensitive, Alignment.TOP_LEFT);
		
		Link citeLink = new Link(
			"About DoubleTreeJS", 
			new ExternalResource(
				"http://linguistics.chrisculy.net/lx/software/DoubleTreeJS/index.html")); 
		
		citeLink.setTargetName("_blank"); //$NON-NLS-1$
		headerPanel.addComponent(citeLink);
		headerPanel.setComponentAlignment(citeLink, Alignment.TOP_CENTER);
		
		btExpandCompressRight = new IconButton(VaadinIcons.EXPAND_SQUARE);
		headerPanel.addComponent(btExpandCompressRight);
		headerPanel.setComponentAlignment(btExpandCompressRight, Alignment.TOP_RIGHT);
		

		addComponent(headerPanel);
		setComponentAlignment(headerPanel, Alignment.TOP_CENTER);

		
		doubleTree = new DoubleTree();
		doubleTree.setSizeFull();
		addComponent(doubleTree);
		setExpandRatio(doubleTree, 1f);
		
	}
	

	@Override
	public String toString() {
		return "KWIC as a DoubleTree"; 
	}

	@Override
	public void setExpansionListener(ExpansionListener expansionListener) {
		this.expansionListener = expansionListener;	
	}
	
	private void handleMaxMinRequest() {
		expanded = !expanded;
		
		if (expanded) {
	
			btExpandCompressRight.setIcon(VaadinIcons.COMPRESS_SQUARE);
			if (expansionListener != null) {
				expansionListener.expand();
			}
		}
		else {
	
			btExpandCompressRight.setIcon(VaadinIcons.EXPAND_SQUARE);
			if (expansionListener != null) {
				expansionListener.compress();
			}
		}
	}


	@Override
	public void setSelectedQueryResultRows(Iterable<QueryResultRow> selectedRows) {
		kwics.clear();
		if ((selectedRows.iterator().next() instanceof TagQueryResultRow)
				&& displaySettings.equals(DisplaySetting.GROUPED_BY_TAG)) {
			for (QueryResultRow row : selectedRows) {
				TagQueryResultRow tqrr = (TagQueryResultRow) row;
				String tagPath = tqrr.getTagDefinitionPath().replace("/", "");
				KwicProvider kwicProvider = null;
				try {
					kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
				} catch (ExecutionException e1) {
					((CatmaApplication) UI.getCurrent()).showAndLogError("Error visualizing group by tag", e1);
				}
				KeywordInSpanContext kwic = null;
				try {
					kwic = kwicProvider.getKwic(row.getRange(), contextSize);
					KeywordInSpanContext newKwic = new KeywordInSpanContext(tagPath, kwic.getKwic(),
							kwic.getKwicSourceRange(), kwic.getRelativeKeywordStartPos(), kwic.isRightToLeft(),
							kwic.getSpanContext());
					kwics.add(newKwic);
				} catch (IOException e) {
					((CatmaApplication) UI.getCurrent()).showAndLogError("Error visualizing group by tag", e);
				}
			}
			doubleTree.setupFromArrays(kwics, true);
		} else {
			for (QueryResultRow row : selectedRows) {
				KwicProvider kwicProvider = null;
				try {
					kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
				} catch (ExecutionException e1) {
					((CatmaApplication) UI.getCurrent()).showAndLogError("Error visualizing selected data", e1);
				}
				KeywordInSpanContext kwic = null;
				try {
					kwic = kwicProvider.getKwic(row.getRange(), contextSize);
				} catch (IOException e) {
					((CatmaApplication) UI.getCurrent()).showAndLogError("Error visualizing selected data", e);
				}
				kwics.add(kwic);
			}
			doubleTree.setupFromArrays(kwics, true);
		}
	}


	@Override
	public void setDisplaySetting(DisplaySetting displaySetting) {
		this.displaySettings = displaySetting;		
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
	public void close() {
		// TODO Auto-generated method stub	
	}

}
