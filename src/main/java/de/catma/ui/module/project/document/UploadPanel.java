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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

import de.catma.ui.CatmaApplication;
import de.catma.util.IDGenerator;

class UploadPanel extends HorizontalLayout {

	private Upload upload;
	private ProgressBar pi;
	private Button btCancelUpload;
	private Label fileLabel;
	private URI uploadedFileUri;

	public UploadPanel() {
		initComponents();
		initActions();
	}

	private void initActions() {
		upload.addStartedListener(new StartedListener() {
			
			public void uploadStarted(StartedEvent event) {
				pi.setValue(0f);
				pi.setVisible(true);
				btCancelUpload.setVisible(true);
				fileLabel.setVisible(false);
			}
		});
		
		upload.addProgressListener(new ProgressListener() {
			
			public void updateProgress(long readBytes, long contentLength) {
				pi.setValue(Float.valueOf(readBytes)/Float.valueOf(contentLength));
			}
		});
		upload.addFailedListener(new FailedListener() {
			
			public void uploadFailed(FailedEvent event) {
				pi.setVisible(false);
				btCancelUpload.setVisible(false);
				fileLabel.setValue("Upload cancelled");
				fileLabel.setVisible(true);
			}
		});
		
		upload.addSucceededListener(new SucceededListener() {
			
			public void uploadSucceeded(SucceededEvent event) {
				pi.setVisible(false);
				btCancelUpload.setVisible(false);
				fileLabel.setValue(MessageFormat.format("{0} uploaded!", event.getFilename()));
				fileLabel.setVisible(true);
			}
		});
		
		upload.setReceiver(new Receiver() {
			
			public OutputStream receiveUpload(String filename, String mimeType) {

				try {
					String tempDir = 
							((CatmaApplication)UI.getCurrent()).accquirePersonalTempFolder();
					IDGenerator idGenerator = new IDGenerator();
					
					File uploadFile = new File(new File(tempDir), idGenerator.generateDocumentId());
					uploadedFileUri = uploadFile.toURI();
					
					if (uploadFile.exists()) {
						uploadFile.delete();
					}
			
					return new FileOutputStream(uploadFile);					
				} catch (IOException e) {
					e.printStackTrace(); // TODO: handle
					return null;
				}
			}
		});
		
		btCancelUpload.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				upload.interruptUpload();
				
			}
		});
				
	}

	private void initComponents() {
		setSpacing(true);
		upload = new Upload();
		upload.setButtonCaption("Upload local file");
		upload.setImmediateMode(true);
		addComponent(upload);

		pi = new ProgressBar();
		pi.setVisible(false);
		addComponent(pi);
		setComponentAlignment(pi, Alignment.MIDDLE_CENTER);
		
		btCancelUpload = new Button("Cancel");
		addComponent(btCancelUpload);
		btCancelUpload.setVisible(false);
		
		fileLabel = new Label();
		fileLabel.setVisible(false);
		addComponent(fileLabel);
		setComponentAlignment(fileLabel, Alignment.MIDDLE_CENTER);
		
	}

	public void addListener(StartedListener listener) {
		upload.addStartedListener(listener);
	}

	public void addListener(FailedListener listener) {
		upload.addFailedListener(listener);
	}

	public void addListener(SucceededListener listener) {
		upload.addSucceededListener(listener);
	}
	
	public URI getUploadedFileUri() {
		return uploadedFileUri;
	}
	
	
}
