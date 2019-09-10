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

import java.util.Arrays;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.OptionGroup;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.queryengine.MatchMode;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.DynamicWizardStep;

public class ComplexTypeSelectionPanel extends VerticalLayout implements
		DynamicWizardStep {
	
	private static class TagMatchModeItem {
		
		private String displayText;
		private MatchMode tagMatchMode;
		
		public TagMatchModeItem(String displayText, MatchMode tagMatchMode) {
			this.displayText = displayText;
			this.tagMatchMode = tagMatchMode;
		}
		
		public MatchMode getTagMatchMode() {
			return tagMatchMode;
		}
		
		@Override
		public String toString() {
			return displayText;
		}
	}
	
	static enum ComplexTypeOption {
		UNION(","), //$NON-NLS-1$
		EXCLUSION("-"), //$NON-NLS-1$
		REFINMENT("where"), //$NON-NLS-1$
		;
		String queryElement;
		String displayString;
		
		private ComplexTypeOption(String queryElement) {
			this.queryElement = queryElement;
		}
		
		public void setDisplayString(String displayString) {
			this.displayString = displayString;
		}
		
		public String getQueryElement() {
			return queryElement;
		}
		
		@Override
		public String toString() {
			return (displayString != null)?displayString:super.toString();
		}
	}
	
	private QueryTree queryTree;
	private OptionGroup complexTypeSelect;
	private boolean typeAdded = false;
	private ComboBox tagMatchModeCombo;

	public ComplexTypeSelectionPanel(QueryTree queryTree) {
		this.queryTree = queryTree;
		initComponents();
		initActions();
	}

	private void initActions() {
		complexTypeSelect.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (typeAdded) {
					queryTree.removeLast();
				}

				String postfix = ""; //$NON-NLS-1$
				if(!((ComplexTypeOption)complexTypeSelect.getValue()).equals(
					ComplexTypeOption.UNION)) {
					postfix = ((TagMatchModeItem)tagMatchModeCombo.getValue()).getTagMatchMode().name().toLowerCase();
				}
				
				queryTree.add(
					((ComplexTypeOption)complexTypeSelect.getValue()).getQueryElement(),
					postfix);

				typeAdded = true;
				
				tagMatchModeCombo.setVisible(!
					((ComplexTypeOption)complexTypeSelect.getValue()).equals(
						ComplexTypeOption.UNION));
			}
		});
		
		
		tagMatchModeCombo.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				if (typeAdded) {
					queryTree.removeLast();
				}

				queryTree.add(
					((ComplexTypeOption)complexTypeSelect.getValue()).getQueryElement(),
					((TagMatchModeItem)tagMatchModeCombo.getValue()).getTagMatchMode().name().toLowerCase());

				typeAdded = true;
			}
		});
	}

	private void initComponents() {
		
		setSpacing(true);
		setWidth("100%"); //$NON-NLS-1$
		ComplexTypeOption.UNION.setDisplayString(
				Messages.getString("ComplexTypeSelectionPanel.AddMoreResults")); //$NON-NLS-1$
		ComplexTypeOption.EXCLUSION.setDisplayString(
				Messages.getString("ComplexTypeSelectionPanel.ExcludeHitsFromPreviousResults")); //$NON-NLS-1$
		ComplexTypeOption.REFINMENT.setDisplayString(
				Messages.getString("ComplexTypeSelectionPanel.RefinePreviousResults")); //$NON-NLS-1$
		
		complexTypeSelect = 
				new OptionGroup("", Arrays.asList(ComplexTypeOption.values())); //$NON-NLS-1$
		
		complexTypeSelect.setImmediate(true);
		
		addComponent(complexTypeSelect);
		setComponentAlignment(complexTypeSelect, Alignment.MIDDLE_CENTER);
		
		
		tagMatchModeCombo = new ComboBox(Messages.getString("ComplexTypeSelectionPanel.ChooseMatch")); //$NON-NLS-1$
		tagMatchModeCombo.setImmediate(true);
		TagMatchModeItem exactMatchItem = 
				new TagMatchModeItem(Messages.getString("ComplexTypeSelectionPanel.ExactMatch"), MatchMode.EXACT); //$NON-NLS-1$
		tagMatchModeCombo.addItem(exactMatchItem);
		tagMatchModeCombo.addItem(
				new TagMatchModeItem(Messages.getString("ComplexTypeSelectionPanel.BoundaryMatch"),  //$NON-NLS-1$
						MatchMode.BOUNDARY));
		tagMatchModeCombo.addItem(
				new TagMatchModeItem(Messages.getString("ComplexTypeSelectionPanel.OverlapMatch"),  //$NON-NLS-1$
						MatchMode.OVERLAP));
		tagMatchModeCombo.setNullSelectionAllowed(false);
		tagMatchModeCombo.setNewItemsAllowed(false);
		
		tagMatchModeCombo.setDescription(
			Messages.getString("ComplexTypeSelectionPanel.MatchModeInfluence")); //$NON-NLS-1$
		tagMatchModeCombo.setValue(exactMatchItem);
		
		addComponent(tagMatchModeCombo);
		setComponentAlignment(tagMatchModeCombo, Alignment.MIDDLE_CENTER);
		

	}

	public Component getContent() {
		return this;
	}
	
	@Override
	public String getCaption() {
		return Messages.getString("ComplexTypeSelectionPanel.NextQuery"); //$NON-NLS-1$
	}

	public boolean onAdvance() {
		return true;
	}

	public boolean onBack() {
		return true;
	}

	public void stepActivated(boolean forward) {
		if (forward) {
			complexTypeSelect.setValue(ComplexTypeOption.UNION);
		}
	}

	public boolean onFinish() {
		return false;
	}

	public boolean onFinishOnly() {
		return false;
	}

	public void stepAdded() {/* noop */}
	
	public void stepDeactivated(boolean forward) {
		if (!forward && typeAdded) {
			queryTree.removeLast();
			typeAdded = false;
		}
	}
}
