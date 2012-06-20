package de.catma.ui.analyzer.querybuilder;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.dialog.wizard.DynamicWizardStep;

public class SimilPanel extends VerticalLayout implements DynamicWizardStep {
	
	@Override
	public String getCaption() {
		return "Similarity";
	}
	
	public void stepActivated() {
		// TODO Auto-generated method stub

	}

	public boolean onFinish() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onFinishOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	public void stepDeactivated() {
		// TODO Auto-generated method stub

	}

	public Component getContent() {
		return this;
	}

	public boolean onAdvance() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onBack() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String toString() {
		return "by grade of similarity";
	}

}
