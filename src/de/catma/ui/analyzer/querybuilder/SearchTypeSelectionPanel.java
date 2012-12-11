package de.catma.ui.analyzer.querybuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class SearchTypeSelectionPanel 
	extends VerticalLayout implements DynamicWizardStep {
	
	private ToggleButtonStateListener toggleButtonStateListener;
	private OptionGroup searchTypeSelect;
	private DynamicWizardStep nextStep;
	private PhrasePanel phrasePanel;
	private boolean onBack;

	public SearchTypeSelectionPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree,
			QueryOptions queryOptions, 
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		this(toggleButtonStateListener, queryTree, queryOptions, false, 
				tagsetDefinitionDictionary);
	}
	
	public SearchTypeSelectionPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree,
			QueryOptions queryOptions,
			boolean onBack, TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		this.toggleButtonStateListener = toggleButtonStateListener;
		this.onBack = onBack;
		initComponents(queryTree, queryOptions, tagsetDefinitionDictionary);
		initActions();
	}

	private void initActions() {
		
		searchTypeSelect.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				if (value != null) {
					setNextStep(
						(DynamicWizardStep)value);
				}
			}
		});
		
	}

	private void setNextStep(DynamicWizardStep step) {
		if (nextStep != null) {
			toggleButtonStateListener.getWizard().removeStep(nextStep);
		}
		nextStep = step;
		
		toggleButtonStateListener.getWizard().addStep(nextStep);
	}

	private void initComponents(
			QueryTree queryTree, QueryOptions queryOptions, 
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		
		setSpacing(true);
		setWidth("100%");
		List<Component> nextSteps = new ArrayList<Component>();
		phrasePanel = 
			new PhrasePanel(
				toggleButtonStateListener,
				queryTree, queryOptions,
				tagsetDefinitionDictionary);
		nextSteps.add(phrasePanel);
		
		nextSteps.add(
			new SimilPanel(
				toggleButtonStateListener, 
				queryTree, queryOptions,
				tagsetDefinitionDictionary));
		
		nextSteps.add(
			new TagPanel(
				toggleButtonStateListener, queryTree, queryOptions, 
				tagsetDefinitionDictionary));
		
		nextSteps.add(
			new CollocPanel(
				toggleButtonStateListener, 
				queryTree, queryOptions,
				tagsetDefinitionDictionary));
		
		nextSteps.add(
				new FreqPanel(
					toggleButtonStateListener, 
					queryTree, queryOptions,
					tagsetDefinitionDictionary));

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
		return onBack;
	}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepAdded() {
	}
	
	public void stepActivated(boolean forward) {
		if(forward) {
			setNextStep(phrasePanel);
		}
	}
	
	public void stepDeactivated(boolean forward) {
		if (!forward) {
			toggleButtonStateListener.getWizard().removeStep(nextStep);
			nextStep = null;
		}
	}
}
