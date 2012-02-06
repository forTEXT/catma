package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

public class ToggleButtonStateListener implements WizardStepListener {

	private Wizard wizard;
	
	public ToggleButtonStateListener(Wizard wizard) {
		this.wizard = wizard;
	}

	public void stepChanged(WizardStep source) {
		wizard.getNextButton().setEnabled(!((DynamicWizardStep)source).onFinishOnly() && source.onAdvance());
		wizard.getBackButton().setEnabled(source.onBack());
		wizard.getFinishButton().setEnabled(((DynamicWizardStep)source).onFinish());
	}

}
