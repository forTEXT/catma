package de.catma.ui.visualizer.doubletree;

import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.KeywordInContext;
import de.catma.ui.tabbedview.ClosableTab;

public class DoubleTreeView  extends Panel implements ClosableTab {
	
	private DoubleTree doubleTree;
	private List<KeywordInContext> kwics;
	private CheckBox cbCaseSensitive;

	public DoubleTreeView(List<KeywordInContext> kwics) {
		initComponents();
		initActions();
		this.kwics = kwics;
		doubleTree.setupFromArrays(kwics, true);
	}

	private void initActions() {
		cbCaseSensitive.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				doubleTree.setupFromArrays(kwics, cbCaseSensitive.getValue());
				markAsDirty();
			}
		});
		
	}

	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		content.setWidth("1600px");
		setHeight("100%");
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setSpacing(true);
		headerPanel.setWidth("500px");
		
		cbCaseSensitive = new CheckBox("case sensitive", true);
		cbCaseSensitive.setImmediate(true);
		
		headerPanel.addComponent(cbCaseSensitive);
		headerPanel.setComponentAlignment(cbCaseSensitive, Alignment.TOP_LEFT);
		
		Link citeLink = new Link(
				"About DoubleTreeJS",
				new ExternalResource(
						"http://www.sfs.uni-tuebingen.de/~cculy/software/DoubleTreeJS/index.html"));
		
		citeLink.setTargetName("_blank");
		headerPanel.addComponent(citeLink);
		headerPanel.setComponentAlignment(citeLink, Alignment.TOP_RIGHT);

		content.addComponent(headerPanel);
		content.setComponentAlignment(headerPanel, Alignment.TOP_CENTER);
		
		doubleTree = new DoubleTree();
		doubleTree.setHeight("100%");
		doubleTree.setWidth("1600px");
		
		content.addComponent(doubleTree);
		setContent(content);
		setScrollLeft(400);
	}
	
	public void close() { /* noop */ }

	public void addClickshortCuts() { /* noop */ }
	
	public void removeClickshortCuts() { /* noop */ }	
	
	@Override
	public String toString() {
		return "KWIC as a DoubleTree";
	}
}
