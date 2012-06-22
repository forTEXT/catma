package de.catma.ui.analyzer.querybuilder;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class SearchTypeSelectionPanel 
	extends VerticalLayout implements DynamicWizardStep {
	
	private OptionGroup searchTypeSelect;
	private DynamicWizardStep nextStep;
	private PhrasePanel phrasePanel;
	
	public SearchTypeSelectionPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, Wizard wizard, 
			QueryOptions queryOptions) {
		initComponents(toggleButtonStateListener, queryTree, queryOptions);
		initActions(wizard);
	}

	private void initActions(final Wizard wizard) {
		
		searchTypeSelect.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				if (value != null) {
					setNextStep((DynamicWizardStep)value, wizard);
				}
			}
		});
		
	}

	void setNextStep(DynamicWizardStep step, Wizard wizard) {
		if (step == null) {
			step = phrasePanel;
		}
		
		if (nextStep != null) {
			wizard.removeStep(nextStep);
		}
		nextStep = step;
		wizard.addStep(nextStep);
	}

	private void initComponents(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, QueryOptions queryOptions) {
		
		setSpacing(true);
		setWidth("100%");
		List<Component> nextSteps = new ArrayList<Component>();
		phrasePanel = 
			new PhrasePanel(
				toggleButtonStateListener,
				queryTree, queryOptions);
		nextSteps.add(phrasePanel);
		nextSteps.add(
			new TagPanel(
				toggleButtonStateListener, queryTree, queryOptions));
		
//		COLLOC("by collocation", null),
//		FREQ("by frequency", null),

		searchTypeSelect = new OptionGroup("",nextSteps);
		
		searchTypeSelect.setImmediate(true);
		searchTypeSelect.setValue(phrasePanel);
		
		addComponent(searchTypeSelect);
		setComponentAlignment(searchTypeSelect, Alignment.MIDDLE_CENTER);
	}

	
	@Override
	public String getCaption() {
		return "How do you want to search?";
	}
	
	public Component getContent() {
		return this;
	}
	
	public boolean onAdvance() {
		return true;
	}
	
	public boolean onBack() {
		return false;
	}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepActivated() { /* not used */ }
	
	public void stepDeactivated() { /* not used */ }
}
