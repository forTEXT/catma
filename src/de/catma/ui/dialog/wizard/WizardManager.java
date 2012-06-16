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
	
	public WizardManager(Window wizardWindow) {
		this.wizardWindow = wizardWindow;
	}

	public void wizardCompleted(WizardCompletedEvent event) {
		if (lastActiveStep != null) {
			lastActiveStep.stepDeactivated();
		}
		wizardWindow.setVisible(false);
		wizardWindow.getParent().removeWindow(wizardWindow);
	}

	public void wizardCancelled(WizardCancelledEvent event) {
		wizardWindow.setVisible(false);
		wizardWindow.getParent().removeWindow(wizardWindow);
	}

	public void stepSetChanged(WizardStepSetChangedEvent event) {/*not needed*/}

	public void activeStepChanged(WizardStepActivationEvent event) {
		if (lastActiveStep != null) {
			lastActiveStep.stepDeactivated();
		}
		lastActiveStep = (DynamicWizardStep)event.getActivatedStep();
		event.getWizard().getNextButton().setEnabled(
				!((DynamicWizardStep)event.getActivatedStep()).onFinishOnly() 
					&& event.getActivatedStep().onAdvance());
		event.getWizard().getBackButton().setEnabled(event.getActivatedStep().onBack());
		event.getWizard().getFinishButton().setEnabled(
				((DynamicWizardStep)event.getActivatedStep()).onFinish());
		((DynamicWizardStep)event.getActivatedStep()).stepActivated();
	}
}