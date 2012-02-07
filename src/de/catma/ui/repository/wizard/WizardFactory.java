package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

public class WizardFactory {

	private static class WizardManager implements
			WizardProgressListener {
		private final WizardWindow wizardWindow;
		private DynamicWizardStep lastActiveStep;
		
		private WizardManager(WizardWindow wizardWindow) {
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

	public WizardWindow createWizardWindow(
			WizardProgressListener wizardProgressListener, WizardResult wizardResult) {
		
		Wizard wizard = new Wizard();
		wizard.getFinishButton().setEnabled(false);
		
		wizard.addStep(
				new LocationPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new FileTypePanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new IndexerOptionsPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new ContentInfoPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		WizardWindow wizardWindow = new WizardWindow("Add new Source Document", wizardResult);
		wizardWindow.setModal(true);
		wizardWindow.setContent(wizard);
		wizardWindow.setHeight("98%");
		wizardWindow.setWidth("85%");
		
		wizard.addListener(new WizardManager(wizardWindow));
		
		wizard.addListener(wizardProgressListener);

		return wizardWindow;
	}
}
