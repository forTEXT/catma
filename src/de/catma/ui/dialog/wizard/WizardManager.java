package de.catma.ui.dialog.wizard;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.ui.Window;


public class WizardManager implements
		WizardProgressListener {
	private final Window wizardWindow;
	private DynamicWizardStep lastActiveStep;
	private int lastStepCount = 0;
	
	public WizardManager(Window wizardWindow) {
		this.wizardWindow = wizardWindow;
	}

	public void wizardCompleted(WizardCompletedEvent event) {
		if (lastActiveStep != null) {
			lastActiveStep.stepDeactivated(true);
		}
		wizardWindow.setVisible(false);
		wizardWindow.getParent().removeWindow(wizardWindow);
	}

	public void wizardCancelled(WizardCancelledEvent event) {
		wizardWindow.setVisible(false);
		wizardWindow.getParent().removeWindow(wizardWindow);
	}

	public void stepSetChanged(WizardStepSetChangedEvent event) {
		if (lastStepCount < event.getWizard().getSteps().size()) {
			DynamicWizardStep lastStep = 
				(DynamicWizardStep)event.getWizard().getSteps().get(
						event.getWizard().getSteps().size()-1);
			lastStep.stepAdded();
		}
		lastStepCount = event.getWizard().getSteps().size();
	}

	public void activeStepChanged(WizardStepActivationEvent event) {
		boolean forward = true;
		if (lastActiveStep != null) {
			forward = 
				(event.getWizard().getSteps().indexOf(lastActiveStep)
					< event.getWizard().getSteps().indexOf(event.getActivatedStep())); 
			lastActiveStep.stepDeactivated(forward);
		}
		lastActiveStep = (DynamicWizardStep)event.getActivatedStep();
		event.getWizard().getNextButton().setEnabled(
				!((DynamicWizardStep)event.getActivatedStep()).onFinishOnly() 
					&& event.getActivatedStep().onAdvance());
		event.getWizard().getBackButton().setEnabled(event.getActivatedStep().onBack());
		event.getWizard().getFinishButton().setEnabled(
				((DynamicWizardStep)event.getActivatedStep()).onFinish());
		((DynamicWizardStep)event.getActivatedStep()).stepActivated(forward);
	}
}