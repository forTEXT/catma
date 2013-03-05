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
package de.catma.ui.analyzer.querybuilder;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardProgressListener;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.ui.dialog.wizard.WizardFactory;

public class QueryBuilderWizardFactory extends WizardFactory {
	
	private QueryTree queryTree;
	private QueryOptions queryOptions;
	private TagsetDefinitionDictionary tagsetDefinitionDictionary;

	public QueryBuilderWizardFactory(
			WizardProgressListener wizardProgressListener, QueryTree queryTree, 
			QueryOptions queryOptions,
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		super(wizardProgressListener);
		this.queryTree = queryTree;
		this.queryOptions = queryOptions;
		this.tagsetDefinitionDictionary = tagsetDefinitionDictionary;
	}

	@Override
	protected void addSteps(Wizard wizard) {
		SearchTypeSelectionPanel searchTypeSelectionPanel = 
				new SearchTypeSelectionPanel(
					new ToggleButtonStateListener(wizard), 
					queryTree, queryOptions,
					tagsetDefinitionDictionary);
		
		wizard.addStep(searchTypeSelectionPanel);
	}
	
	
	
}
