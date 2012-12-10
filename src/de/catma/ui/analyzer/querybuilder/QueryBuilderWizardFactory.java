package de.catma.ui.analyzer.querybuilder;

import java.util.Map;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.ui.dialog.wizard.WizardFactory;

public class QueryBuilderWizardFactory extends WizardFactory {
	
	private QueryTree queryTree;
	private QueryOptions queryOptions;
	private TagsetDefinitionDictionaryListener tagsetDefinitionDictionaryListener;
	private Map<String, TagsetDefinition> tagsetDefinitionsByUuid;

	public QueryBuilderWizardFactory(
			WizardProgressListener wizardProgressListener, QueryTree queryTree, 
			QueryOptions queryOptions,
			TagsetDefinitionDictionaryListener tagsetDefinitionDictionaryListener, 
			Map<String, TagsetDefinition> tagsetDefinitionsByUuid) {
		super(wizardProgressListener);
		this.queryTree = queryTree;
		this.queryOptions = queryOptions;
		this.tagsetDefinitionDictionaryListener = tagsetDefinitionDictionaryListener;
		this.tagsetDefinitionsByUuid = tagsetDefinitionsByUuid;
	}

	@Override
	protected void addSteps(Wizard wizard) {
		SearchTypeSelectionPanel searchTypeSelectionPanel = 
				new SearchTypeSelectionPanel(
					new ToggleButtonStateListener(wizard), 
					queryTree, queryOptions,
					tagsetDefinitionDictionaryListener,
					tagsetDefinitionsByUuid);
		
		wizard.addStep(searchTypeSelectionPanel);
	}
	
	
	
}
