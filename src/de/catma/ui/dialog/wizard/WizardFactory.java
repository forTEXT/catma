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
		wizardWindow.setStyleName("wizardry");
		
		wizard.addListener(new WizardManager(wizardWindow));
		wizard.addListener(wizardProgressListener);

		addSteps(wizard);
		
		return wizardWindow;
	}

	protected abstract void addSteps(Wizard wizard);
	

}
