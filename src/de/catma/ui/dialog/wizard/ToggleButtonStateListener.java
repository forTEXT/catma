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

import org.apache.tools.ant.taskdefs.Get;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;


public class ToggleButtonStateListener implements WizardStepListener {

	private Wizard wizard;
	
	public ToggleButtonStateListener(Wizard wizard) {
		this.wizard = wizard;
	}

	public void stepChanged(WizardStep source) {
		wizard.getNextButton().setEnabled(!((DynamicWizardStep)source).onFinishOnly() && source.onAdvance());
		wizard.getBackButton().setEnabled(source.onBack());
		wizard.getFinishButton().setEnabled(((DynamicWizardStep)source).onFinish());
	}
	
	
	public Wizard getWizard() {
		return wizard;
	}
}
