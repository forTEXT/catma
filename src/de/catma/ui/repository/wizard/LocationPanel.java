package de.catma.ui.repository.wizard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

import de.catma.CleaApplication;

public class LocationPanel extends VerticalLayout 
	implements Upload.SucceededListener, Upload.FailedListener, Upload.Receiver {
	
	
	private Upload upload;
	private ProgressIndicator pi;
	private TextField remoteURIInput;
	private Button btCancelUpload;
	private Label fileLabel;

	public LocationPanel() {
		initComponents();
		initActions();
	}

	private void initActions() {
		
		upload.addListener(new StartedListener() {
			
			public void uploadStarted(StartedEvent event) {
				pi.setValue(0f);
				pi.setVisible(true);
				pi.setPollingInterval(500);
				btCancelUpload.setVisible(true);
				remoteURIInput.setEnabled(false);
				fileLabel.setVisible(false);
			}
		});
		
		upload.addListener(new ProgressListener() {
			
			public void updateProgress(long readBytes, long contentLength) {
				pi.setValue(Float.valueOf(readBytes)/Float.valueOf(contentLength));
			}
		});
		upload.addListener(new FailedListener() {
			
			public void uploadFailed(FailedEvent event) {
				pi.setVisible(false);
				btCancelUpload.setVisible(false);
				fileLabel.setValue("Upload cancelled!");
				fileLabel.setVisible(true);
				remoteURIInput.setEnabled(true);
			}
		});
		
		upload.addListener(new SucceededListener() {
			
			public void uploadSucceeded(SucceededEvent event) {
				pi.setVisible(false);
				btCancelUpload.setVisible(false);
				fileLabel.setValue(event.getFilename() + " uploaded!");
				fileLabel.setVisible(true);
			}
		});
		
		btCancelUpload.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				upload.interruptUpload();
				
			}
		});
	}

	private void initComponents() {
		setMargin(true);
		setSpacing(true);
		
		remoteURIInput = new TextField();
		remoteURIInput.setCaption("Enter an URI that is accessible over the internet:");
		remoteURIInput.setWidth("100%");
		addComponent(remoteURIInput);
		
		Label localFileLabel = new Label("or upload a local file from your computer:");
		addComponent(localFileLabel);
		
		pi = new ProgressIndicator();
		pi.setVisible(false);
		
		HorizontalLayout uploadPanel = new HorizontalLayout();
		uploadPanel.setSpacing(true);
		
		upload = new Upload(null,this);
		upload.setButtonCaption("Upload local file");
		upload.setImmediate(true);
		uploadPanel.addComponent(upload);

		uploadPanel.addComponent(pi);
		uploadPanel.setComponentAlignment(pi, Alignment.MIDDLE_CENTER);
		
		btCancelUpload = new Button("Cancel");
		uploadPanel.addComponent(btCancelUpload);
		btCancelUpload.setVisible(false);
		
		fileLabel = new Label();
		fileLabel.setVisible(false);
		uploadPanel.addComponent(fileLabel);
		uploadPanel.setComponentAlignment(fileLabel, Alignment.MIDDLE_CENTER);
		
		addComponent(uploadPanel);
		
	}
	
	public OutputStream receiveUpload(String filename, String mimeType) {
		try {
			String tempDir = ((CleaApplication)getApplication()).getTempDirectory();
			File uploadFile = new File(new File(tempDir), filename);
			if (uploadFile.exists()) {
				uploadFile.delete();
			}
			
	
			return new FileOutputStream(uploadFile) {
				int counter=0;

				@Override
				public void write(byte[] b, int off, int len)
						throws IOException {
					counter++;
					if ((counter%100)==0){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
					}
					super.write(b, off, len);
				}
			};
			
		} catch (FileNotFoundException e) {
			e.printStackTrace(); // TODO: handle
			return null;
		}
	}
	
	public void uploadFailed(FailedEvent event) {
		System.out.println("uploadFailed: " + event);
		
	}
	
	public void uploadSucceeded(SucceededEvent event) {
		System.out.println("uploadSucceeded: " + event);
		
	}

}
