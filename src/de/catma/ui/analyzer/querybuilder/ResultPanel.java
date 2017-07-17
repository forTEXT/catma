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

import java.text.MessageFormat;

import com.vaadin.data.Validator;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.CatmaApplication;
import de.catma.ui.data.util.PropertyDependentItemSorter;
import de.catma.ui.data.util.PropertyToTrimmedStringCIComparator;

public class ResultPanel extends VerticalLayout {
	private static enum TreePropertyName {
		caption,
		frequency, 
		;
	}
	
	private TreeTable resultTable;
	private QueryOptions queryOptions;
	private Label queryLabel;
	private TextField maxTotalFrequencyField;
	private Button btShowInPreview;
	private ProgressBar searchProgress;

	public ResultPanel(QueryOptions queryOptions) {
		this.queryOptions = queryOptions;
		this.queryOptions.setLimit(50);
		initComponents();
	}

	private void initComponents() {
		setSpacing(true);
		setMargin(new MarginInfo(true, false, false, false));
		HorizontalLayout buttonPanel = new HorizontalLayout(); 
		buttonPanel.setSpacing(true);
		
		btShowInPreview = new Button(Messages.getString("ResultPanel.ShowInPreview")); //$NON-NLS-1$
		buttonPanel.addComponent(btShowInPreview);
		Label maxTotalFrequencyLabel = new Label(Messages.getString("ResultPanel.MaxTotalFrequency")); //$NON-NLS-1$
		buttonPanel.addComponent(maxTotalFrequencyLabel);
		buttonPanel.setComponentAlignment(
				maxTotalFrequencyLabel, Alignment.MIDDLE_CENTER);
		
		maxTotalFrequencyField = new TextField();
		maxTotalFrequencyField.setValue("50"); //$NON-NLS-1$
		maxTotalFrequencyField.addValidator(new Validator() {
			public void validate(Object value) throws InvalidValueException {
				try {
					Integer.valueOf((String)value);
				}
				catch (NumberFormatException nfe) {
					throw new InvalidValueException(Messages.getString("ResultPanel.ValueMustBeInteger")); //$NON-NLS-1$
				}
				
			}
		});
		maxTotalFrequencyField.setInvalidAllowed(false);
		buttonPanel.addComponent(maxTotalFrequencyField);
		addComponent(buttonPanel);
		
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setSpacing(true);
		headerPanel.setWidth("100%"); //$NON-NLS-1$
		addComponent(headerPanel);

		searchProgress = new ProgressBar();
		searchProgress.setIndeterminate(false);
		searchProgress.setVisible(false);
		headerPanel.addComponent(searchProgress);
		
		Label yourSearchLabel = new Label(Messages.getString("ResultPanel.YourSearch")); //$NON-NLS-1$
		headerPanel.addComponent(yourSearchLabel);
		headerPanel.setExpandRatio(yourSearchLabel, 0.1f);
		
		queryLabel = new Label(Messages.getString("ResultPanel.NothingEnteredYet")); //$NON-NLS-1$
		queryLabel.addStyleName("centered-bold-text"); //$NON-NLS-1$
		headerPanel.addComponent(queryLabel);
		headerPanel.setExpandRatio(queryLabel, 0.2f);
		
		Label willMatch = new Label(Messages.getString("ResultPanel.WillMatch")); //$NON-NLS-1$
		headerPanel.addComponent(willMatch);
		headerPanel.setExpandRatio(willMatch, 0.2f);
		
		
		resultTable = new TreeTable();
		resultTable.setSizeFull();
		resultTable.setSelectable(true);
		HierarchicalContainer container = new HierarchicalContainer();
		container.setItemSorter(
				new PropertyDependentItemSorter(
						TreePropertyName.caption, 
						new PropertyToTrimmedStringCIComparator()));
		
		resultTable.setContainerDataSource(container);

		resultTable.addContainerProperty(
				TreePropertyName.caption, String.class, null);
		resultTable.setColumnHeader(TreePropertyName.caption, Messages.getString("ResultPanel.Phrase")); //$NON-NLS-1$
		resultTable.addContainerProperty(
				TreePropertyName.frequency, Integer.class, null);
		resultTable.setColumnHeader(TreePropertyName.frequency, Messages.getString("ResultPanel.Frequency")); //$NON-NLS-1$
		addComponent(resultTable);
	}

	public void setQuery(String query) {
		int limit = Integer.valueOf(
				(String)maxTotalFrequencyField.getValue());
		
		queryOptions.setLimit(limit);
		
		queryLabel.setValue(query);

		searchProgress.setIndeterminate(true);
		searchProgress.setVisible(true);

		QueryJob job = new QueryJob(
				query,
				queryOptions);
		((BackgroundServiceProvider)UI.getCurrent()).getBackgroundService().submit(
				job, 
				new ExecutionListener<QueryResult>() {
				public void done(QueryResult result) {
					setQueryResult(result);
					searchProgress.setIndeterminate(false);
					searchProgress.setVisible(false);
				};
				public void error(Throwable t) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						Messages.getString("ResultPanel.ErrorDuringSearch"), t); //$NON-NLS-1$
				}
			}, 
			new LogProgressListener());
	}
	
	public void setQueryResult(QueryResult queryResult) {
		resultTable.removeAllItems();
		int totalCount = 0;
		int totalFreq = 0;
		
		for (GroupedQueryResult phraseResult : 
				queryResult.asGroupedSet()) {
			addPhraseResult(phraseResult);
			totalFreq+=phraseResult.getTotalFrequency();
			totalCount++;
		}
		
		resultTable.setFooterVisible(true);
		resultTable.setColumnFooter(
				TreePropertyName.caption, MessageFormat.format(Messages.getString("ResultPanel.TotalCount"), totalCount)); //$NON-NLS-1$
		resultTable.setColumnFooter(
				TreePropertyName.frequency, MessageFormat.format(Messages.getString("ResultPanel.TotalFrequency"), totalFreq)); //$NON-NLS-1$
	}

	private void addPhraseResult(GroupedQueryResult phraseResult) {
		resultTable.addItem(new Object[]{
				phraseResult.getGroup(), 
				phraseResult.getTotalFrequency()},
				phraseResult.getGroup());

		resultTable.getContainerProperty(
			phraseResult.getGroup(), TreePropertyName.caption).setValue(
					phraseResult.getGroup());
		
		for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
			SourceDocument sourceDocument = 
				queryOptions.getRepository().getSourceDocument(sourceDocumentID);
			String sourceDocumentItemID = 
					phraseResult.getGroup() + "@" + sourceDocument; //$NON-NLS-1$
			resultTable.addItem(sourceDocumentItemID);
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.frequency).setValue(
							phraseResult.getFrequency(sourceDocumentID));
			resultTable.getContainerProperty(
					sourceDocumentItemID, TreePropertyName.caption).setValue(
							sourceDocument.toString());
			resultTable.setParent(sourceDocumentItemID, phraseResult.getGroup());
			
			resultTable.setChildrenAllowed(sourceDocumentItemID, false);
		}
		
	}

	public void addBtShowInPreviewListener(ClickListener clickListener) {
		btShowInPreview.addClickListener(clickListener);
	}

	public void clear() {
		resultTable.removeAllItems();
	}

	
}
