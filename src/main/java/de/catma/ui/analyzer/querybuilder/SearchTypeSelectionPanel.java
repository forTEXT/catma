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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.OptionGroup;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.Corpus;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class SearchTypeSelectionPanel 
	extends VerticalLayout implements DynamicWizardStep {
	
	private ToggleButtonStateListener toggleButtonStateListener;
	private OptionGroup searchTypeSelect;
	private DynamicWizardStep nextStep;
	private PhrasePanel phrasePanel;
	private boolean onBack;

	public SearchTypeSelectionPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree,
			QueryOptions queryOptions, Corpus corpus) {
		this(toggleButtonStateListener, queryTree, queryOptions, corpus, false);
	}
	
	public SearchTypeSelectionPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree,
			QueryOptions queryOptions,
			Corpus corpus,
			boolean onBack) {
		this.toggleButtonStateListener = toggleButtonStateListener;
		this.onBack = onBack;
		initComponents(queryTree, queryOptions, corpus);
		initActions();
	}

	private void initActions() {
		
		searchTypeSelect.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				if (value != null) {
					setNextStep(
						(DynamicWizardStep)value);
				}
			}
		});
		
	}

	private void setNextStep(DynamicWizardStep step) {
		if (nextStep != null) {
			toggleButtonStateListener.getWizard().removeStep(nextStep);
		}
		nextStep = step;
		
		toggleButtonStateListener.getWizard().addStep(nextStep);
	}

	private void initComponents(
			QueryTree queryTree, QueryOptions queryOptions, Corpus corpus) {
		
		setSpacing(true);
		setWidth("100%"); //$NON-NLS-1$
		List<Component> nextSteps = new ArrayList<Component>();
		phrasePanel = 
			new PhrasePanel(
				toggleButtonStateListener,
				queryTree, queryOptions, corpus);
		nextSteps.add(phrasePanel);
		
		nextSteps.add(
			new SimilPanel(
				toggleButtonStateListener, 
				queryTree, queryOptions, corpus));
		
		nextSteps.add(
			new TagPanel(
				toggleButtonStateListener, queryTree, queryOptions, corpus));
		
		nextSteps.add(
			new CollocPanel(
				toggleButtonStateListener, 
				queryTree, queryOptions, corpus));
		
		nextSteps.add(
				new FreqPanel(
					toggleButtonStateListener, 
					queryTree, queryOptions, corpus));

		searchTypeSelect = new OptionGroup("",nextSteps); //$NON-NLS-1$
		
		searchTypeSelect.setImmediate(true);
		searchTypeSelect.setValue(phrasePanel);
		
		addComponent(searchTypeSelect);
		setComponentAlignment(searchTypeSelect, Alignment.MIDDLE_CENTER);
	}

	
	@Override
	public String getCaption() {
		return Messages.getString("SearchTypeSelectionPanel.HowDoYouWantToSearch"); //$NON-NLS-1$
	}
	
	public Component getContent() {
		return this;
	}
	
	public boolean onAdvance() {
		return true;
	}
	
	public boolean onBack() {
		return onBack;
	}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepAdded() {
	}
	
	public void stepActivated(boolean forward) {
		if(forward) {
			setNextStep(phrasePanel);
		}
	}
	
	public void stepDeactivated(boolean forward) {
		if (!forward) {
			toggleButtonStateListener.getWizard().removeStep(nextStep);
			nextStep = null;
		}
	}
}
