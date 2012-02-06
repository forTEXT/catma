package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.WizardStep;

public interface DynamicWizardStep extends WizardStep {
	public void stepActivated(); 
	
	public boolean onFinish();
	
	public boolean onFinishOnly();

	public void stepDeactivated();
}
