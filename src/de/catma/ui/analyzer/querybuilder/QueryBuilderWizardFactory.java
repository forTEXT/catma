package de.catma.ui.analyzer.querybuilder;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.WizardFactory;

public class QueryBuilderWizardFactory extends WizardFactory {
	
	private QueryTree queryTree;

	public QueryBuilderWizardFactory(
			WizardProgressListener wizardProgressListener, QueryTree queryTree) {
		super(wizardProgressListener);
		this.queryTree = queryTree;
	}

	@Override
	protected void addSteps(Wizard wizard) {
		wizard.addStep(new SearchTypeSelectionPanel());
		
	}
	
	
	
}
