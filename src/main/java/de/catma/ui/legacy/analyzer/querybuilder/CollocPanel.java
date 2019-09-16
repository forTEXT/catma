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

import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.corpus.Corpus;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.legacy.wizard.ToggleButtonStateListener;

public class CollocPanel extends AbstractSearchPanel {

	private TextField wordInput;
	private TextField collocInput;
	private TextField spanSizeInput;
	private ResultPanel resultPanel;

	public CollocPanel(ToggleButtonStateListener toggleButtonStateListener,
			QueryTree queryTree, QueryOptions queryOptions,
			Corpus corpus) {
		super(toggleButtonStateListener, queryTree, queryOptions, corpus);
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
			StringBuilder builder = new StringBuilder("\""); //$NON-NLS-1$
			builder.append(wordInput.getValue());
			builder.append("\" & \""); //$NON-NLS-1$
			builder.append(collocInput.getValue());
			builder.append("\" "); //$NON-NLS-1$
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
		wordInput.addValidator(new NonEmptySequenceValidator(Messages.getString("CollocPanel.ThisValueCannotBeEmpty"))); //$NON-NLS-1$
		wordInput.setRequired(true);
		wordInput.setInvalidAllowed(false);
		searchPanel.addComponent(wordInput);
		
		collocInput = new TextField(Messages.getString("CollocPanel.ThatAppearNear")); //$NON-NLS-1$
		collocInput.addValidator(new NonEmptySequenceValidator(Messages.getString("CollocPanel.ThisValueCannotBeEmpty"))); //$NON-NLS-1$
		collocInput.setRequired(true);
		collocInput.setInvalidAllowed(false);
		
		searchPanel.addComponent(collocInput);
		
		spanSizeInput = new TextField(Messages.getString("CollocPanel.WithinASpanOf"), "5"); //$NON-NLS-1$ //$NON-NLS-2$
		spanSizeInput.addValidator(new IntegerValueValidator(false, false));
		spanSizeInput.setRequired(true);
		spanSizeInput.setInvalidAllowed(false);
		
		searchPanel.addComponent(spanSizeInput);
		Label infoLabel = new Label(
			Messages.getString("CollocPanel.PreviewFrequencyLimitInfo"));  //$NON-NLS-1$
		infoLabel.setContentMode(ContentMode.HTML);
		searchPanel.addComponent(infoLabel);
		
		return searchPanel;
	}

	@Override
	public String getCaption() {
		return Messages.getString("CollocPanel.SearchForAllOccurrencesOf"); //$NON-NLS-1$
	}
	
	@Override
	public String toString() {
		return Messages.getString("CollocPanel.ByCollocation"); //$NON-NLS-1$
	}
}
