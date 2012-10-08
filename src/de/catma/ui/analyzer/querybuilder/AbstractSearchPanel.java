package de.catma.ui.analyzer.querybuilder;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public abstract class AbstractSearchPanel extends VerticalLayout implements DynamicWizardStep {

	private CheckBox cbComplexQuery;
	protected ToggleButtonStateListener toggleButtonStateListener;
	protected QueryTree queryTree;
	protected QueryOptions queryOptions;
	
	private ComplexTypeSelectionPanel complexTypeSelectionPanel;
	private SearchTypeSelectionPanel searchTypeSelectionPanel;
	
	protected boolean onFinish;
	protected boolean onFinishOnly;
	protected boolean onAdvance;
	protected String curQuery = null;


	public AbstractSearchPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, QueryOptions queryOptions) {
		this.toggleButtonStateListener = toggleButtonStateListener;
		this.queryTree = queryTree;
		this.queryOptions = queryOptions;
		onFinish = false;
		onFinishOnly = true;
		onAdvance = false;
	}

	protected void initComponents(Component content) {
		setSizeFull();
		setSpacing(true);
		cbComplexQuery = new CheckBox("continue to build a complex query");
		cbComplexQuery.setImmediate(true);
		addComponent(cbComplexQuery);
		
		setExpandRatio(content, 0.9f);
		setExpandRatio(cbComplexQuery, 0.1f);
		setComponentAlignment(cbComplexQuery, Alignment.BOTTOM_RIGHT);
		
		initActions();
	}

	private void initActions() {
		cbComplexQuery.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				
				if (cbComplexQuery.booleanValue()) {
					if (complexTypeSelectionPanel == null) {
						complexTypeSelectionPanel = 
								new ComplexTypeSelectionPanel(queryTree);
						searchTypeSelectionPanel = new SearchTypeSelectionPanel(
								toggleButtonStateListener, queryTree, queryOptions, true);
					}
					toggleButtonStateListener.getWizard().addStep(
							complexTypeSelectionPanel);
					toggleButtonStateListener.getWizard().addStep(
							searchTypeSelectionPanel);
				}
				else {
					if (complexTypeSelectionPanel != null) {
						toggleButtonStateListener.getWizard().removeStep(
								searchTypeSelectionPanel);
						toggleButtonStateListener.getWizard().removeStep(
								complexTypeSelectionPanel);
					}
				}
				onFinishOnly = !cbComplexQuery.booleanValue();
				toggleButtonStateListener.stepChanged(AbstractSearchPanel.this);
			}
			
		});
	}

	public void addCbComplexQueryListener(ClickListener listener) {
		cbComplexQuery.addListener(listener);
	}
	
	public void stepActivated() { /* noop */ }
	public void stepDeactivated(boolean forward) {		
		if (!forward && !(curQuery == null)) {
			queryTree.removeLast();
		}
	}


	public void stepAdded() {/* noop */}
	
	public boolean onAdvance() {
		return onAdvance;
	}

	public boolean onBack() {
		return true;
	}

	public boolean onFinish() {
		return onFinish;
	}

	public boolean onFinishOnly() {
		return onFinishOnly;
	}

	public boolean isComplexQuery() {
		return cbComplexQuery.booleanValue();
	}
}
