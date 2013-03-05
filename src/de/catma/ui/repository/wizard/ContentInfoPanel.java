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

import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;

import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.WizardStepListener;
import de.catma.util.ContentInfoSet;

class ContentInfoPanel extends HorizontalLayout implements
		DynamicWizardStep {
	
	
	private AddSourceDocWizardResult wizardResult;
	@SuppressWarnings("unused")
	private WizardStepListener wizardStepListener; // not used
	private Form contentInfoForm;
	
	public ContentInfoPanel(WizardStepListener wizardStepListener,
			AddSourceDocWizardResult wizardResult) {
		super();
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		
		initComponents();
	}

	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		
		contentInfoForm = new Form();
		addComponent(contentInfoForm);
		contentInfoForm.setReadOnly(false);
		contentInfoForm.setWriteThrough(true);
		contentInfoForm.setImmediate(true);
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

	public void stepActivated(boolean forward) {
	
		ContentInfoSet contentInfoSet = wizardResult.getSourceDocumentInfo().getContentInfoSet();
		
		BeanItem<ContentInfoSet> biContentInfoSet = new BeanItem<ContentInfoSet>(contentInfoSet);
		
		contentInfoForm.setItemDataSource(biContentInfoSet);
		contentInfoForm.setVisibleItemProperties(new String[] {
				"title", "author", "description", "publisher"
		});
	}

	public boolean onFinish() {
		return true;
	}
	
	public boolean onFinishOnly() {
		return true;
	}
	
	public void stepDeactivated(boolean forward) {
		if (wizardResult.getSourceDocument().toString().isEmpty()) {
			URI uri = wizardResult.getSourceDocumentInfo().getTechInfoSet().getURI();
			String title = uri.toString();
			if (uri.getScheme().equals("file")) {
				title = new File(uri).getName();
			}
			wizardResult.getSourceDocument()
				.getSourceContentHandler().getSourceDocumentInfo()
				.getContentInfoSet().setTitle(title);
		}
	}
	
	public void stepAdded() {/* noop */}
}
