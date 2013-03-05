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

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.data.util.IntegerValueValidator;
import de.catma.ui.data.util.NonEmptySequenceValidator;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class CollocPanel extends AbstractSearchPanel {

	private TextField wordInput;
	private TextField collocInput;
	private TextField spanSizeInput;
	private ResultPanel resultPanel;

	public CollocPanel(ToggleButtonStateListener toggleButtonStateListener,
			QueryTree queryTree, QueryOptions queryOptions, 
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		super(toggleButtonStateListener, queryTree, queryOptions, tagsetDefinitionDictionary);
		initComponents();
		initActions();
	}

	private void initActions() {

		this.resultPanel.addBtShowInPreviewListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showInPreview();
			}
		});
		
	}

	private void showInPreview() {
		if (wordInput.isValid() && collocInput.isValid() && spanSizeInput.isValid()) {
			StringBuilder builder = new StringBuilder("\"");
			builder.append(wordInput.getValue());
			builder.append("\" & \"");
			builder.append(collocInput.getValue());
			builder.append("\" ");
			builder.append(spanSizeInput.getValue());
			
			if (curQuery != null) {
				queryTree.removeLast();
			}
			curQuery = builder.toString();
			resultPanel.setQuery(curQuery);
			
			queryTree.add(curQuery);
			onFinish = !isComplexQuery();
			onAdvance = true;
			toggleButtonStateListener.stepChanged(this);
		}
		else {
			onFinish = false;
			onAdvance = false;
		}		
	}
	
	private void initComponents() {
		VerticalSplitPanel splitPanel = new VerticalSplitPanel();
		
		Component searchPanel = createSearchPanel();
		splitPanel.addComponent(searchPanel);
		resultPanel = new ResultPanel(queryOptions);
		splitPanel.addComponent(resultPanel);
		addComponent(splitPanel);
		
		super.initSearchPanelComponents(splitPanel);
	}

	private Component createSearchPanel() {

		VerticalLayout searchPanel = new VerticalLayout();
		searchPanel.setSpacing(true);
		
		wordInput = new TextField();
		wordInput.addValidator(new NonEmptySequenceValidator("This value cannot be empty!"));
		wordInput.setRequired(true);
		wordInput.setInvalidAllowed(false);
		searchPanel.addComponent(wordInput);
		
		collocInput = new TextField("that appear near");
		collocInput.addValidator(new NonEmptySequenceValidator("This value cannot be empty!"));
		collocInput.setRequired(true);
		collocInput.setInvalidAllowed(false);
		
		searchPanel.addComponent(collocInput);
		
		spanSizeInput = new TextField("within a span of", "5");
		spanSizeInput.addValidator(new IntegerValueValidator(false, false));
		spanSizeInput.setRequired(true);
		spanSizeInput.setInvalidAllowed(false);
		
		searchPanel.addComponent(spanSizeInput);
		return searchPanel;
	}

	@Override
	public String getCaption() {
		return "Search for all occurrences of";
	}
	
	@Override
	public String toString() {
		return "by collocation";
	}
}
