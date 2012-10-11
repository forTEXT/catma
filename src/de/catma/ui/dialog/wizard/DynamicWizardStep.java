package de.catma.ui.dialog.wizard;

import org.vaadin.teemu.wizards.WizardStep;

public interface DynamicWizardStep extends WizardStep {
	public void stepActivated(boolean forward); 
	
	public boolean onFinish();
	
	public boolean onFinishOnly();

	public void stepDeactivated(boolean forward);
	
	public void stepAdded();
}
