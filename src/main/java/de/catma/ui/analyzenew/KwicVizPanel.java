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
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class KwicVizPanel extends AbstractOkCancelDialog<VizSnapshot>  implements VizPanel{
private Iterator<Component> allResultsIterator;
private	HorizontalLayout mainLayout;
private Panel left;
private	Panel right;
private Button test;
private ComboBox<String> comboBox;
private List<String> availableResultSets;
private VerticalLayout vertical;
private Panel treeGridPanelKwic;
private ArrayList<TreeGrid<TagRowItem>> currentTeeGrids;


	public KwicVizPanel(String dialogCaption, ArrayList<TreeGrid<TagRowItem>> currentTreeGrids, SaveCancelListener<VizSnapshot> saveCancelListener) {
		super(dialogCaption, saveCancelListener);

	this.currentTeeGrids= currentTreeGrids;

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
		return	new VizSnapshot("KWIC Visualisation");
	}

	
	@Override
	protected void addContent(ComponentContainer content) {
		
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
		           swichToResultTree(queryAsString);
			
				
			}
		});
		
		mainLayout = new HorizontalLayout();
		left = createResourcePanel(comboBox);
		right= new Panel("Visualisation");
		mainLayout.addComponents(left,right);
		mainLayout.setExpandRatio(left,  1);
		mainLayout.setExpandRatio(right,  1);
		mainLayout.setWidth("100%");

		content.addComponent(mainLayout);	
	}

	

	





	private ArrayList<TreeGrid<TagRowItem>> getCurrentTeeGrids() {
		return currentTeeGrids;
	}




	private void setCurrentTeeGrids(ArrayList<TreeGrid<TagRowItem>> currentTeeGrids) {
		this.currentTeeGrids = currentTeeGrids;
	}




	private void swichToResultTree(String queryAsString) {
		Iterator<TreeGrid<TagRowItem>> allResultsIterator =getCurrentTeeGrids().iterator();
		TreeGrid<TagRowItem> selectedTreeGrid = new TreeGrid<TagRowItem> ();

		while (allResultsIterator.hasNext()) {
			TreeGrid<TagRowItem> treeGrid = allResultsIterator.next();
		
			if (treeGrid.getCaption().equalsIgnoreCase(queryAsString)) {
				selectedTreeGrid = treeGrid;

			}
		}

		treeGridPanelKwic.setContent(selectedTreeGrid);
	}
	
	
	private Panel createResourcePanel(ComboBox<String> comboBox) {
	Iterator<TreeGrid<TagRowItem>> allResultsIterator	= getCurrentTeeGrids().iterator();
	Panel selectResultsPanel = new Panel();	
	vertical= new VerticalLayout();
	vertical.addComponent(comboBox);
	treeGridPanelKwic= new Panel();
	vertical.addComponent(treeGridPanelKwic);
	selectResultsPanel.setContent(vertical);
	while(allResultsIterator.hasNext()) {
		TreeGrid<TagRowItem> treeGrid=allResultsIterator.next();
	
		 availableResultSets.add(treeGrid.getCaption());
	}
	 return selectResultsPanel;
	}

}
