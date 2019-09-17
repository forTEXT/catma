package de.catma.ui.module.analyze.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

public class SearchTypeSelectionStep extends VerticalLayout implements WizardStep {
	
	private ProgressStep progressStep;
	private WizardStep nextStep;
	private RadioButtonGroup<WizardStep> rgSteps;
	private ProgressStep nextProgressStep;
	private int nextStepNo;

	public SearchTypeSelectionStep(
			int stepNo,
			Project project, 
			WizardContext context, 
			ProgressStepFactory progressStepFactory) {
		this.progressStep = progressStepFactory.create(stepNo, "How do you want to search?");
		this.nextStepNo = stepNo+1;
		initComponents(project, context, progressStepFactory);
		initActions();
	}

	private void initActions() {
		rgSteps.addValueChangeListener(event -> handleNextStepChange(event));
	}

	private void handleNextStepChange(ValueChangeEvent<WizardStep> event) {
		nextStep = event.getValue();
		nextProgressStep.setStepDescription(nextStep.getStepDescription());
	}

	private void initComponents(
			Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		setSizeFull();
		setMargin(true);
		
		List<WizardStep> nextSteps = new ArrayList<WizardStep>();
		
		PhraseOptionsStep phraseOptionsStep = new PhraseOptionsStep(this.nextStepNo, project, context, progressStepFactory);  
		nextProgressStep = phraseOptionsStep.getProgressStep();
		
		nextSteps.add(phraseOptionsStep);
		
		nextSteps.add(new SimilOptionsStep(this.nextStepNo, project, context, nextProgressStep, progressStepFactory));
		nextSteps.add(new FreqOptionsStep(this.nextStepNo, project, context, nextProgressStep, progressStepFactory));
		
		rgSteps = new RadioButtonGroup<>("", nextSteps);
		rgSteps.setValue(nextSteps.get(0));
		nextStep = nextSteps.get(0);
		
		addComponent(rgSteps);
		setComponentAlignment(rgSteps, Alignment.MIDDLE_CENTER);
		
	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return nextStep;
	}

	@Override
	public boolean isValid() {
		return true; //always valid
	}
	
	@Override
	public boolean canFinish() {
		return false;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this); //always valid
		}
	}

}
