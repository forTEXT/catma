package de.catma.ui.visualizer.doubletree;

import java.util.List;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.KeywordInContext;
import de.catma.ui.component.tabbedview.ClosableTab;
import de.catma.ui.component.tabbedview.TabCaptionChangeListener;

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
		cbCaseSensitive.addValueChangeListener(new ValueChangeListener<Boolean>() {
			
			public void valueChange(ValueChangeEvent<Boolean> event) {
				doubleTree.setupFromArrays(kwics, cbCaseSensitive.getValue());
			}
		});
		
	}

	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		content.setWidth("1600px"); //$NON-NLS-1$
		setHeight("100%"); //$NON-NLS-1$
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setSpacing(true);
		headerPanel.setWidth("500px"); //$NON-NLS-1$
		
		cbCaseSensitive = new CheckBox(Messages.getString("DoubleTreeView.caseSensitive"), true); //$NON-NLS-1$
		
		headerPanel.addComponent(cbCaseSensitive);
		headerPanel.setComponentAlignment(cbCaseSensitive, Alignment.TOP_LEFT);
		
		Link citeLink = new Link(
			Messages.getString("DoubleTreeView.about"), //$NON-NLS-1$
			new ExternalResource(
				"http://linguistics.chrisculy.net/lx/software/DoubleTreeJS/index.html")); //$NON-NLS-1$
		
		citeLink.setTargetName("_blank"); //$NON-NLS-1$
		headerPanel.addComponent(citeLink);
		headerPanel.setComponentAlignment(citeLink, Alignment.TOP_RIGHT);

		content.addComponent(headerPanel);
		content.setComponentAlignment(headerPanel, Alignment.TOP_CENTER);
		
		doubleTree = new DoubleTree();
		doubleTree.setHeight("100%"); //$NON-NLS-1$
		doubleTree.setWidth("1600px"); //$NON-NLS-1$
		
		content.addComponent(doubleTree);
		setContent(content);
		setScrollLeft(400);
	}
	
	public void close() { /* noop */ }

	public void addClickshortCuts() { /* noop */ }
	
	public void removeClickshortCuts() { /* noop */ }	
	
	@Override
	public String toString() {
		return Messages.getString("DoubleTreeView.kwicAsDoubleTree"); //$NON-NLS-1$
	}
	
	@Override
	public void setTabNameChangeListener(TabCaptionChangeListener tabNameChangeListener) {
		// TODO Auto-generated method stub
		
	}
}
