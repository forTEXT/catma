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
import de.catma.ui.legacy.wizard.DynamicWizardStep;

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
				"add more results"); 
		ComplexTypeOption.EXCLUSION.setDisplayString(
				"exclude hits from previous results"); 
		ComplexTypeOption.REFINMENT.setDisplayString(
				"refine previous results"); 
		
		complexTypeSelect = 
				new OptionGroup("", Arrays.asList(ComplexTypeOption.values())); //$NON-NLS-1$
		
		complexTypeSelect.setImmediate(true);
		
		addComponent(complexTypeSelect);
		setComponentAlignment(complexTypeSelect, Alignment.MIDDLE_CENTER);
		
		
		tagMatchModeCombo = new ComboBox("Please choose what you consider a match:"); 
		tagMatchModeCombo.setImmediate(true);
		TagMatchModeItem exactMatchItem = 
				new TagMatchModeItem("exact match", MatchMode.EXACT); 
		tagMatchModeCombo.addItem(exactMatchItem);
		tagMatchModeCombo.addItem(
				new TagMatchModeItem("boundary match",  
						MatchMode.BOUNDARY));
		tagMatchModeCombo.addItem(
				new TagMatchModeItem("overlap match",  
						MatchMode.OVERLAP));
		tagMatchModeCombo.setNullSelectionAllowed(false);
		tagMatchModeCombo.setNewItemsAllowed(false);
		
		tagMatchModeCombo.setDescription(
			"The three different match modes influence the way tags refine your search results:<ul><li>exact match - the tag type boundaries have to match exactly to keep a result item in the result set</li><li>boundary match - result items that should be kept in the result set must start and end within the boundaries of the tag</li><li>overlap - the result items that should be kept in the result set must overlap with the range of the tag</li></ul>"); 
		tagMatchModeCombo.setValue(exactMatchItem);
		
		addComponent(tagMatchModeCombo);
		setComponentAlignment(tagMatchModeCombo, Alignment.MIDDLE_CENTER);
		

	}

	public Component getContent() {
		return this;
	}
	
	@Override
	public String getCaption() {
		return "What do you want to do with the next query?"; 
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
