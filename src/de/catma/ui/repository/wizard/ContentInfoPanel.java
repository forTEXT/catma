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
package de.catma.ui.repository.wizard;

import java.io.File;
import java.net.URI;
import java.util.Collection;

import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;

import de.catma.document.source.ContentInfoSet;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.WizardStepListener;

class ContentInfoPanel extends HorizontalLayout implements
		DynamicWizardStep {
	
	private Table table;
	
	private AddSourceDocWizardResult wizardResult;
	@SuppressWarnings("unused")
	private WizardStepListener wizardStepListener; // not used
	
	public ContentInfoPanel(WizardStepListener wizardStepListener,
			AddSourceDocWizardResult wizardResult) {
		super();
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		
		initComponents();
	}

	private void initComponents() {
		setSpacing(true);
		setMargin(true);
		setSizeFull();
		
		BeanItemContainer<SourceDocumentResult> container = new BeanItemContainer<SourceDocumentResult>(SourceDocumentResult.class);
		container.addNestedContainerProperty("sourceDocumentInfo.techInfoSet.fileName");
		container.addNestedContainerProperty("sourceDocumentInfo.contentInfoSet.title");
		container.addNestedContainerProperty("sourceDocumentInfo.contentInfoSet.author");
		container.addNestedContainerProperty("sourceDocumentInfo.contentInfoSet.description");
		container.addNestedContainerProperty("sourceDocumentInfo.contentInfoSet.publisher");
		
		table = new Table("Documents", container);
		
		table.setVisibleColumns(new Object[]{
				"sourceDocumentInfo.techInfoSet.fileName",
				"sourceDocumentInfo.contentInfoSet.title",
				"sourceDocumentInfo.contentInfoSet.author",
				"sourceDocumentInfo.contentInfoSet.description",
				"sourceDocumentInfo.contentInfoSet.publisher"
		});
		table.setColumnHeaders(new String[]{"File Name", "Title", "Author", "Description", "Publisher"});
		
		table.setSelectable(true);
		table.setNullSelectionAllowed(false);
		table.setImmediate(true);
		table.setEditable(true);
		
		addComponent(table);
		
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
		
		return fileName.replace("_", " ").trim();
		
	}

	public void stepActivated(boolean forward) {
		
		BeanItemContainer<SourceDocumentResult> container = (BeanItemContainer<SourceDocumentResult>)table.getContainerDataSource();
		if(container.size() > 0) {
			return;
		}
	
		Collection<SourceDocumentResult> sourceDocumentResults = wizardResult.getSourceDocumentResults();
		
		for (SourceDocumentResult sdr : sourceDocumentResults) {
			String fileName = sdr.getSourceDocumentInfo().getTechInfoSet().getFileName();
			String title = makeTitleFromFileName(fileName);
			sdr.getSourceDocumentInfo().getContentInfoSet().setTitle(title);
		}
		
		container.addAll(sourceDocumentResults);
		
		if(sourceDocumentResults.size() > 0){
			table.select(sourceDocumentResults.toArray()[0]);
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
			if (uri.getScheme().equals("file")) {
				title = new File(uri).getName();
			}
			sdr.getSourceDocument()
				.getSourceContentHandler().getSourceDocumentInfo()
				.getContentInfoSet().setTitle(title);			
		}	
	}
	
	public void stepAdded() {/* noop */}
}
