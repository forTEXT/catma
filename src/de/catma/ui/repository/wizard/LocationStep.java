package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;

public class LocationStep implements WizardStep {

	public String getCaption() {
		return "Source Document Location";
	}

	public Component getContent() {
		return new LocationPanel();
	}

	public boolean onAdvance() {
		return false;
	}

	public boolean onBack() {
		return false;
	}

}
