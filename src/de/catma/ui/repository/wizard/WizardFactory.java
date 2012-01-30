package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.ui.Window;

public class WizardFactory {

	public Window createWizardWindow() {
		Wizard wizard = new Wizard();
		wizard.addStep(new LocationStep());
		Window wizardWindow = new Window("Add new SourceDocument");
		wizardWindow.setModal(true);
		wizardWindow.setContent(wizard);
		wizardWindow.setHeight("400px");
		wizardWindow.setWidth("500px");
		return wizardWindow;
	}
}
