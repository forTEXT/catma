package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;


public class KwicVizPanelNew extends HorizontalLayout implements VizPanel{
	

	private VerticalLayout leftSide;

	private Panel header;
	private Button arrowLeft;
	private	CloseVizViewListener leaveViewListener;
	private Iterator<Component> allResultsIterator;
	private	HorizontalLayout mainLayout;
	private Panel left;
	private	Panel right;
	private Button test;
	private ComboBox<String> comboBox;
	private List<String> availableResultSets;
	private VerticalLayout vertical;
	private Panel treeGridPanelKwic;
	private HorizontalLayout mainContentSplitPanel;
	private Panel rightSide;

	public KwicVizPanelNew(CloseVizViewListener leaveVizListener) {
		leaveViewListener= leaveVizListener;
		initComponents();
		initActions();
		initListeners();
	}
	
	private void initComponents() {
		leftSide= new VerticalLayout();
		rightSide = new Panel("Visualisation");
		header = new Panel();
		arrowLeft= new Button("<");
		header.setContent(arrowLeft);
		leftSide.addComponent(header);
		mainContentSplitPanel = new HorizontalLayout();
		mainContentSplitPanel.addComponents(leftSide,rightSide);
		addComponent(mainContentSplitPanel);
		mainContentSplitPanel.setHeight("100%");
		mainContentSplitPanel.setWidth("100%");
		
		// setExpandRatio(mainContentSplitPanel, 1);
		
		comboBox = new ComboBox<String>();
		comboBox.setWidth("100%");
		comboBox.setCaption("select one resultset");
	
		
		availableResultSets = new ArrayList<>();
	
    	comboBox.setItems(availableResultSets);
    	
 /*   	comboBox.addValueChangeListener(event -> {
           String queryAsString= event.getSource().getValue();
           swichToResultTree(queryAsString);
        });*/
    	
    	comboBox.addSelectionListener(new SingleSelectionListener<String>() {
			
			@Override
			public void selectionChange(SingleSelectionEvent<String> event) {
			    String queryAsString= event.getSource().getValue();
		          // swichToResultTree(queryAsString);
			
				
			}
		});
		
		
	

          leftSide.addComponent(comboBox);	
	}

	



	private void initActions() {
		arrowLeft.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				leaveViewListener.onClose();
				
			}
		});
		
	}
	private void initListeners() {
		
	}
	
	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub
		
	}

}
