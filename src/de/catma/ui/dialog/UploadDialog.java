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
package de.catma.ui.dialog;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class UploadDialog extends VerticalLayout {
	
	private static class DataReceiver implements Receiver {
		private ByteArrayOutputStream data;
		
		public DataReceiver(ByteArrayOutputStream data) {
			this.data = data;
		}

		public OutputStream receiveUpload(String filename, String mimeType) {
			return data;
		}
	}
	
	private Window dialogWindow;
	private String caption;
	private SaveCancelListener<byte[]> saveCancelListener;
	private Upload upload;
	private ProgressIndicator pi;
	private ByteArrayOutputStream data;
	private Button btCancel;
	
	public UploadDialog(String caption, SaveCancelListener<byte[]> saveCancelListener) {
		this.caption = caption;
		this.saveCancelListener = saveCancelListener;
		this.data = new ByteArrayOutputStream();

		initComponents();
		initActions();
	}
	
	private void initActions() {
		 upload.addListener(new Upload.StartedListener() {
            public void uploadStarted(StartedEvent event) {
//	                upload.setVisible(false);
            	pi.setVisible(true);
                pi.setValue(0f);
                pi.setPollingInterval(500);
                upload.setCaption("Uploading file \"" + event.getFilename()
                        + "\"");
            }
        });

        upload.addListener(new Upload.ProgressListener() {
            public void updateProgress(long readBytes, long contentLength) {
                pi.setValue(new Float(readBytes / (float) contentLength));
            }

        });

        upload.addListener(new Upload.SucceededListener() {
            public void uploadSucceeded(SucceededEvent event) {
            	upload.setCaption("Uploading file \"" + event.getFilename()
                        + "\" succeeded");
            	saveCancelListener.savePressed(data.toByteArray());
            	upload.removeListener(this);
            	UploadDialog.this.close();
            }
        });

        upload.addListener(new Upload.FailedListener() {
            public void uploadFailed(FailedEvent event) {
            	upload.setCaption("Uploading interrupted");
            }
        });
        btCancel.addListener(new ClickListener() {
        	
        	public void buttonClick(ClickEvent event) {
        		upload.interruptUpload();
        		dialogWindow.getParent().removeWindow(dialogWindow);
        		saveCancelListener.cancelPressed();
        	}
        });
	}

	private void close() {
		getApplication().getMainWindow().removeWindow(dialogWindow);
	}

	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		
		dialogWindow = new Window(caption);
		dialogWindow.setModal(true);
		
		upload = new Upload(
				"Please select a file to upload",
				new DataReceiver(data));
		upload.setButtonCaption("Select file");
		upload.setImmediate(true);
		
		addComponent(upload);
		pi = new ProgressIndicator();
		pi.setVisible(false);
		addComponent(pi);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);

		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		
		addComponent(buttonPanel);
		this.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		dialogWindow.addComponent(this);
		
		upload.focus();

	}
	
	public void show(Window parent, String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		parent.addWindow(dialogWindow);
	}
	
	public void show(Window parent) {
		show(parent, "25%");
	}
	
	
}
