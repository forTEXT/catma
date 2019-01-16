package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class KwicVizPanel extends AbstractOkCancelDialog<VizSnapshot>  implements VizPanel{
private	AnalyzeNewView analyzeNewView;
private	HorizontalLayout mainLayout;
private Panel left;
private	Panel right;
private Button test;
private ComboBox<String> comboBox;


	public KwicVizPanel(String dialogCaption, AnalyzeNewView analyzeNewView, SaveCancelListener<VizSnapshot> saveCancelListener) {
		super(dialogCaption, saveCancelListener);

	this.analyzeNewView= analyzeNewView;

	initListeners();
	initAction();
	
	}
	
	
	
	
	private void initListeners() {
		
	}
	private void initAction() {
		
	}
	
	@Override
	public void setQueryResults() {	
		
	}

	public void attach() {
		super.attach();
//		((FocusHandler)UI.getCurrent()).focusDeferred(textInput);
	}
	@Override
	protected VizSnapshot getResult() {
		return	new VizSnapshot("TempTitle");
	}

	
	@Override
	protected void addContent(ComponentContainer content) {
		
		comboBox = new ComboBox<String>();
		List<String> availableResultSets = new ArrayList<>();
		availableResultSets.add("property= \"%\"");
		availableResultSets.add("tag=\"Tag%\"");
		availableResultSets.add("tag= \"Tag1\"");
		availableResultSets.add("wild= \"und\"");
    	comboBox.setItems(availableResultSets);
		
		
		
		
		
		
		mainLayout = new HorizontalLayout();
		left = createResourcePanel( comboBox,analyzeNewView);
		right= new Panel("Visualisation");
		mainLayout.addComponents(left,right);
		mainLayout.setExpandRatio(left,  1);
		mainLayout.setExpandRatio(right,  1);
		mainLayout.setWidth("100%");

		content.addComponent(mainLayout);
	
		
	}
	private Panel createResourcePanel(ComboBox<String> comboBox,AnalyzeNewView analyzeNewView) {
	Iterator <Component> allResultsIterator	=analyzeNewView.getAllQueryResultPanels();
	Panel allResultsPanel = new Panel();
	VerticalLayout vertical= new VerticalLayout();
	vertical.addComponent(comboBox);
	allResultsPanel.setContent(vertical);
	while(allResultsIterator.hasNext()) {
		Component resultBox=allResultsIterator.next();
		 ResultPanelNew myBox=(ResultPanelNew)resultBox;
		 vertical.addComponent(myBox);
	}

	 return allResultsPanel;
	}

}
