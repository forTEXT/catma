package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import de.catma.document.repository.Repository;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.ui.dialog.wizard.WizardFactory;

public class AddSourceDocWizardFactory extends WizardFactory {
	
	private AddSourceDocWizardResult wizardResult;
	private Repository repository;

	public AddSourceDocWizardFactory(
			WizardProgressListener wizardProgressListener,
			AddSourceDocWizardResult wizardResult,
			Repository repository) {
		super(wizardProgressListener);
		this.wizardResult = wizardResult;
		this.repository = repository;
	}

	@Override
	protected void addSteps(Wizard wizard) {
		
		wizard.addStep(
				new LocationPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new FileTypePanel(
						new ToggleButtonStateListener(wizard), wizardResult, repository));
		
		wizard.addStep(
				new IndexerOptionsPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new ContentInfoPanel(
						new ToggleButtonStateListener(wizard), wizardResult));	
	}
}
