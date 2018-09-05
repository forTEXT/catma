/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.repository.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import de.catma.document.repository.Repository;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.ui.dialog.wizard.WizardFactory;

public class AddSourceDocWizardFactory extends WizardFactory {
	
	private AddSourceDocWizardResult wizardResult;
	private Repository repository;

	public AddSourceDocWizardFactory(
			WizardProgressListener wizardProgressListener,
			AddSourceDocWizardResult wizardResult,
			Repository repository) {
		super(wizardProgressListener);
		this.wizardResult = wizardResult;
		this.repository = repository;
	}

	@Override
	protected void addSteps(Wizard wizard) {
		
		wizard.addStep(
				new LocationPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new FileTypePanel(
						new ToggleButtonStateListener(wizard), wizardResult, repository));
		
		wizard.addStep(
				new IndexerOptionsPanel(
						new ToggleButtonStateListener(wizard), wizardResult));
		
		wizard.addStep(
				new ContentInfoPanel(
						new ToggleButtonStateListener(wizard), wizardResult));	
	}
}
