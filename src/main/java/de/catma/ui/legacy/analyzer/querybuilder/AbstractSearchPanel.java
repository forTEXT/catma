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
package de.catma.ui.legacy.analyzer.querybuilder;


import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.corpus.Corpus;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public abstract class AbstractSearchPanel extends VerticalLayout implements DynamicWizardStep {

	private CheckBox cbComplexQuery;
	protected ToggleButtonStateListener toggleButtonStateListener;
	protected QueryTree queryTree;
	protected QueryOptions queryOptions;
	
	private ComplexTypeSelectionPanel complexTypeSelectionPanel;
	private SearchTypeSelectionPanel searchTypeSelectionPanel;
	
	protected boolean onFinish;
	protected boolean onFinishOnly;
	protected boolean onAdvance;
	protected String curQuery = null;
	private Corpus corpus;


	public AbstractSearchPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, QueryOptions queryOptions, Corpus corpus) {
		this.toggleButtonStateListener = toggleButtonStateListener;
		this.queryTree = queryTree;
		this.queryOptions = queryOptions;
		this.corpus = corpus;
		onFinish = false;
		onFinishOnly = true;
		onAdvance = false;
	}

	protected void initSearchPanelComponents(Component content) {
		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		cbComplexQuery = new CheckBox(Messages.getString("AbstractSearchPanel.ContinueToBuildAComplexQuery")); //$NON-NLS-1$
		cbComplexQuery.setImmediate(true);
		addComponent(cbComplexQuery);
		
		setExpandRatio(content, 0.95f);
		setExpandRatio(cbComplexQuery, 0.05f);
		setComponentAlignment(cbComplexQuery, Alignment.BOTTOM_RIGHT);
		
		initSearchPanelActions();
	}

	private void initSearchPanelActions() {
		cbComplexQuery.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (cbComplexQuery.getValue()) {
					if (complexTypeSelectionPanel == null) {
						complexTypeSelectionPanel = 
								new ComplexTypeSelectionPanel(queryTree);
						searchTypeSelectionPanel = new SearchTypeSelectionPanel(
								toggleButtonStateListener, queryTree, queryOptions, 
								corpus, true);
					}
					toggleButtonStateListener.getWizard().addStep(
							complexTypeSelectionPanel);
					toggleButtonStateListener.getWizard().addStep(
							searchTypeSelectionPanel);
				}
				else {
					if (complexTypeSelectionPanel != null) {
						toggleButtonStateListener.getWizard().removeStep(
								searchTypeSelectionPanel);
						toggleButtonStateListener.getWizard().removeStep(
								complexTypeSelectionPanel);
					}
				}
				
				onFinishOnly = !cbComplexQuery.getValue();
				onFinish = (!cbComplexQuery.getValue() 
							&& (curQuery != null) && !curQuery.isEmpty());
				
				toggleButtonStateListener.stepChanged(AbstractSearchPanel.this);
			}
			
		});
	}

	public void addCbComplexQueryListener(ValueChangeListener listener) {
		cbComplexQuery.addValueChangeListener(listener);
	}
	
	public void stepActivated(boolean forward) {
		if (forward) {
			if ((curQuery != null) && !curQuery.isEmpty()) {
				queryTree.add(curQuery);
			}
		}
	}
	
	public void stepDeactivated(boolean forward) {		
		if (!forward) {
			cbComplexQuery.setValue(Boolean.FALSE);
			if (curQuery != null) {
				queryTree.removeLast();
			}
		}
	}


	public void stepAdded() {/* noop */}
	
	public boolean onAdvance() {
		return onAdvance;
	}

	public boolean onBack() {
		return true;
	}

	public boolean onFinish() {
		return onFinish;
	}

	public boolean onFinishOnly() {
		return onFinishOnly;
	}

	public boolean isComplexQuery() {
		return cbComplexQuery.getValue();
	}
	
	@Override
	public void attach() {
		super.attach();
		getParent().setHeight("100%"); //$NON-NLS-1$
	}
	
	public Component getContent() {
		return this;
	}

	public Corpus getCorpus() {
		return corpus;
	}
}
