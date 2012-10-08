package de.catma.ui.dialog.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import com.vaadin.ui.Window;

public abstract class WizardFactory {
	
	private WizardProgressListener wizardProgressListener;
	
	public WizardFactory(WizardProgressListener wizardProgressListener) {
		this.wizardProgressListener = wizardProgressListener;
	}

	public Window createWizardWindow(
			String caption, String width, String height) {
		
		Wizard wizard = new Wizard();
		wizard.getFinishButton().setEnabled(false);
		Window wizardWindow = new Window(caption);
		wizardWindow.setModal(true);
		wizardWindow.setContent(wizard);
		wizardWindow.setWidth(width);
		wizardWindow.setHeight(height);
		
		wizard.addListener(new WizardManager(wizardWindow));
		wizard.addListener(wizardProgressListener);

		addSteps(wizard);
		
		return wizardWindow;
	}

	protected abstract void addSteps(Wizard wizard);
	

}
