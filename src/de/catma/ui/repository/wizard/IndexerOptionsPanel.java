package de.catma.ui.repository.wizard;

import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

public class IndexerOptionsPanel extends HorizontalLayout implements DynamicWizardStep {
	
	private boolean onAdvance;
	private WizardStepListener wizardStepListener;
	private WizardResult wizardResult;
	

	public IndexerOptionsPanel(WizardStepListener wizardStepListener,
			WizardResult wizardResult) {
		super();
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
	}

	public Component getContent() {
		return this;
	}

	public boolean onAdvance() {
		return onAdvance;
	}

	public boolean onBack() {
		return true;
	}

	public void stepActivated() {
		// TODO Auto-generated method stub

	}

}
