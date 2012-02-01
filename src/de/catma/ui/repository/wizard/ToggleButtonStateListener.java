package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

public class ToggleButtonStateListener implements WizardStepListener {

	private Wizard wizard;
	
	public ToggleButtonStateListener(Wizard wizard) {
		this.wizard = wizard;
	}

	public void stepChanged(WizardStep source) {
		System.out.println(source + " a:"+source.onAdvance());
		wizard.getNextButton().setEnabled(source.onAdvance());
		wizard.getBackButton().setEnabled(source.onBack());
	}

}
