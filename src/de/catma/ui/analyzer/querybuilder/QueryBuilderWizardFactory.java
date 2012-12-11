package de.catma.ui.analyzer.querybuilder;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.ui.dialog.wizard.WizardFactory;

public class QueryBuilderWizardFactory extends WizardFactory {
	
	private QueryTree queryTree;
	private QueryOptions queryOptions;
	private TagsetDefinitionDictionary tagsetDefinitionDictionary;

	public QueryBuilderWizardFactory(
			WizardProgressListener wizardProgressListener, QueryTree queryTree, 
			QueryOptions queryOptions,
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		super(wizardProgressListener);
		this.queryTree = queryTree;
		this.queryOptions = queryOptions;
		this.tagsetDefinitionDictionary = tagsetDefinitionDictionary;
	}

	@Override
	protected void addSteps(Wizard wizard) {
		SearchTypeSelectionPanel searchTypeSelectionPanel = 
				new SearchTypeSelectionPanel(
					new ToggleButtonStateListener(wizard), 
					queryTree, queryOptions,
					tagsetDefinitionDictionary);
		
		wizard.addStep(searchTypeSelectionPanel);
	}
	
	
	
}
