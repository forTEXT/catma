package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

class SelectionStep extends VerticalLayout implements WizardStep {
	
	private Project project;
	private WizardContext context;
	private ProgressStep progressStep;

	public SelectionStep(EventBus eventBus, Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		this.project = project;
		this.context = context;
		this.progressStep = progressStepFactory.create(1, "Select Annotations");		
		initComponents();
	}

	private void initComponents() {
		
		
	}

	@Override
	public ProgressStep getProgressStep() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WizardStep getNextStep() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		// TODO Auto-generated method stub

	}

}
