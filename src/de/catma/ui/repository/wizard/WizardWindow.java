package de.catma.ui.repository.wizard;

import com.vaadin.ui.Window;

public class WizardWindow extends Window {

	private WizardResult wizardResult;
	
	public WizardWindow(String caption, WizardResult wizardResult) {
		super(caption);
		this.wizardResult = wizardResult;
	}
	
	public WizardResult getWizardResult() {
		return wizardResult;
	}
	
}
