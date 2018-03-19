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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import com.vaadin.v7.event.FieldEvents.TextChangeEvent;
import com.vaadin.v7.event.FieldEvents.TextChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.v7.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.Upload.FailedEvent;
import com.vaadin.v7.ui.Upload.FailedListener;
import com.vaadin.v7.ui.Upload.StartedEvent;
import com.vaadin.v7.ui.Upload.StartedListener;
import com.vaadin.v7.ui.Upload.SucceededEvent;
import com.vaadin.v7.ui.Upload.SucceededListener;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.TechInfoSet;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.WizardStepListener;

class LocationPanel extends VerticalLayout implements DynamicWizardStep {
	
	private TextField remoteURIInput;
	private UploadPanel uploadPanel;
	private boolean onAdvance = false;
	private AddSourceDocWizardResult wizardResult;
	private Panel remoteURIInputPanel;

	public LocationPanel(WizardStepListener listener, AddSourceDocWizardResult wizardResult) {
		this.wizardResult = wizardResult;
		initComponents();
		initActions(listener);
	}

	private void initActions(final WizardStepListener wizardStepListener) {
		
		uploadPanel.addListener(new StartedListener() {
			public void uploadStarted(StartedEvent event) {
				remoteURIInputPanel.setEnabled(false);
			}
		});
		

		uploadPanel.addListener(new FailedListener() {
			public void uploadFailed(FailedEvent event) {
				remoteURIInputPanel.setEnabled(true);
			}
		});
		
		uploadPanel.addListener(new SucceededListener() {
			
			public void uploadSucceeded(SucceededEvent event) {

				TechInfoSet ti = 
						new TechInfoSet(
								event.getFilename(),
								new SourceDocumentHandler().getMimeType(
									event.getFilename(), event.getMIMEType()), // the event's mimetype can be wrong (eg. RTF) 
								uploadPanel.getUploadedFileUri());
				
				wizardResult.setInputTechInfoSet(ti);
				onAdvance = true;				
				wizardStepListener.stepChanged(LocationPanel.this);
			}
		});
		
		remoteURIInput.addTextChangeListener(new TextChangeListener() {
			
			public void textChange(TextChangeEvent event) {
				
				try {
					String urlText = event.getText();
					if (urlText.toLowerCase().startsWith("www")) { //TODO: better scheme detection //$NON-NLS-1$
						urlText = "http://" + urlText; //$NON-NLS-1$
					}
					URL url = new URL(urlText);
					
					String fileName = url.getFile();
					fileName = fileName.substring(fileName.lastIndexOf('/') + 1).replace("%20", " "); //$NON-NLS-1$ //$NON-NLS-2$
					
					TechInfoSet ti = new TechInfoSet(fileName, null, url.toURI()); //TODO: mime type detection?
					wizardResult.setInputTechInfoSet(ti);
					onAdvance = true;
				}
				catch(MalformedURLException exc) {
					onAdvance = false;
					exc.printStackTrace(); //TODO: ignore
				}
				catch(URISyntaxException use) {
					onAdvance = false;
					use.printStackTrace(); //TODO: ignore
				}
				
				wizardStepListener.stepChanged(LocationPanel.this);
			}
		});
	}

	private void initComponents() {
		setSpacing(true);
		setMargin(true);
		
//		setSizeFull();
		VerticalLayout remoteLayout = new VerticalLayout();
		remoteLayout.setMargin(true);
		remoteLayout.setSpacing(true);
		remoteLayout.setSizeFull();
		
		remoteURIInputPanel = new Panel(remoteLayout);
		
		remoteURIInput = new TextField();
		remoteURIInput.setCaption(Messages.getString("LocationPanel.enterURL")); //$NON-NLS-1$
		remoteURIInput.setWidth("100%"); //$NON-NLS-1$
		remoteURIInput.setTextChangeEventMode(TextChangeEventMode.EAGER);
		remoteLayout.addComponent(remoteURIInput);
		remoteLayout.setExpandRatio(remoteURIInput, 2);
		
		addComponent(remoteURIInputPanel);
		remoteLayout.addComponent(new Label(Messages.getString("LocationPanel.contentProviderHint"))); //$NON-NLS-1$
		
		Label localFileLabel = new Label("or upload a local file from your computer:"); //$NON-NLS-1$
		addComponent(localFileLabel);
		
		uploadPanel = new UploadPanel();
		
		addComponent(uploadPanel);
	}
	
	public String getCaption() {
		return Messages.getString("LocationPanel.SourceDocLocation"); //$NON-NLS-1$
	}

	public Component getContent() {
		return this;
	}

	public boolean onAdvance() {
		return onAdvance;
	}

	public boolean onBack() {
		return false;
	}
	
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}

	public void stepActivated(boolean forward){ /* noop */}
	public void stepDeactivated(boolean forward) { /* noop */}
	public void stepAdded() {/* noop */}
}

