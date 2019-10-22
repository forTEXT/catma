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
package de.catma.ui.module.project.document;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.dialog.HelpWindow;
import de.catma.ui.legacy.wizard.DynamicWizardStep;
import de.catma.ui.legacy.wizard.WizardStepListener;

class ContentInfoPanel extends VerticalLayout implements
		DynamicWizardStep {
	
	private Grid<SourceDocumentResult> table;
	
	private AddSourceDocWizardResult wizardResult;
	@SuppressWarnings("unused")
	private WizardStepListener wizardStepListener; // not used
	
	public ContentInfoPanel(WizardStepListener wizardStepListener,
			AddSourceDocWizardResult wizardResult) {
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		
		initComponents();
	}

	private void initComponents() {
		setSpacing(false);
		setMargin(false);
		setSizeFull();
		
		
		HelpWindow helpWindow = new HelpWindow("Content details", "<p>This step allows you to set the Document's meta data.</p><p>Double click on a row to change title, author, publisher and description.</p>");
		Button btHelp = helpWindow.createHelpWindowButton();
		addComponent(btHelp);
		setComponentAlignment(btHelp, Alignment.TOP_RIGHT);
		
		table = new Grid<>("Documents", new ListDataProvider<>(Collections.emptyList()));
		table.setSizeFull();
		table.addStyleName("sourcedoc-wizard-content-info-documents-margins");
		
		TextField titleEditor = new TextField();
		TextField authorEditor = new TextField();
		TextField descEditor = new TextField();
		TextField publisherEditor = new TextField();
		
		table.addColumn(docResult -> docResult.getSourceDocumentInfo().getTechInfoSet().getFileName())
			.setCaption("File Name");
		
		table.addColumn(docResult -> docResult.getSourceDocumentInfo().getContentInfoSet().getTitle())
			.setCaption("Title")
			.setEditorComponent(
				titleEditor, 
				(docResult,title) -> docResult.getSourceDocumentInfo().getContentInfoSet().setTitle(title));
		table.addColumn(docResult -> docResult.getSourceDocumentInfo().getContentInfoSet().getAuthor())
			.setCaption("Author")
			.setEditorComponent(
				authorEditor, 
				(docResult,author) -> docResult.getSourceDocumentInfo().getContentInfoSet().setAuthor(author));
		table.addColumn(docResult -> docResult.getSourceDocumentInfo().getContentInfoSet().getDescription())
			.setCaption("Description")
			.setEditorComponent(
				descEditor, 
				(docResult,desc) -> docResult.getSourceDocumentInfo().getContentInfoSet().setDescription(desc));
		table.addColumn(docResult -> docResult.getSourceDocumentInfo().getContentInfoSet().getPublisher())
			.setCaption("Publisher")
			.setEditorComponent(
				publisherEditor, 
				(docResult,publisher) -> docResult.getSourceDocumentInfo().getContentInfoSet().setPublisher(publisher));
		
		table.setSelectionMode(SelectionMode.SINGLE);
		table.getEditor().setEnabled(true);
		
		addComponent(table);
		setExpandRatio(table, 1f);

	}

	public Component getContent() {
		return this;
	}
	
	@Override
	public String getCaption() {
		return "Content details";
	}

	public boolean onAdvance() {
		return true;
	}

	public boolean onBack() {
		return true;
	}
	
	private String makeTitleFromFileName(String fileName) {
		int indexOfLastFullStop = fileName.lastIndexOf('.');
		if(indexOfLastFullStop > 0){
			fileName = fileName.substring(0, indexOfLastFullStop);
		}
		
		return fileName.replace("_", " ").trim(); //$NON-NLS-1$ //$NON-NLS-2$
		
	}

	@SuppressWarnings("unchecked")
	public void stepActivated(boolean forward) {
		if (((ListDataProvider<SourceDocumentResult>)table.getDataProvider()).getItems().isEmpty()) {
			Collection<SourceDocumentResult> sourceDocumentResults = wizardResult.getSourceDocumentResults();
			
			for (SourceDocumentResult sdr : sourceDocumentResults) {
				String fileName = sdr.getSourceDocumentInfo().getTechInfoSet().getFileName();
				String title = makeTitleFromFileName(fileName);
				sdr.getSourceDocumentInfo().getContentInfoSet().setTitle(title);
			}
			
			ListDataProvider<SourceDocumentResult> dataProvider = new ListDataProvider<SourceDocumentResult>(sourceDocumentResults);
			table.setDataProvider(dataProvider);
			
			if(sourceDocumentResults.size() > 0){
				table.select(sourceDocumentResults.iterator().next());
			}
		}
	}

	public boolean onFinish() {
		return true;
	}
	
	public boolean onFinishOnly() {
		return true;
	}
	
	public void stepDeactivated(boolean forward) {
		for (SourceDocumentResult sdr : wizardResult.getSourceDocumentResults()) {
			if (!sdr.toString().isEmpty()){
				continue;
			}
			
			URI uri = sdr.getSourceDocumentInfo().getTechInfoSet().getURI();
			String title = uri.toString();
			if (uri.getScheme().equals("file")) { //$NON-NLS-1$
				title = new File(uri).getName();
			}
			sdr.getSourceDocument()
				.getSourceContentHandler().getSourceDocumentInfo()
				.getContentInfoSet().setTitle(title);			
		}	
	}
	
	public void stepAdded() {/* noop */}
}
