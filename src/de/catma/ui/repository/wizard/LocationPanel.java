package de.catma.ui.repository.wizard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

import de.catma.CleaApplication;
import de.catma.core.document.source.SourceDocumentInfo;

public class LocationPanel extends VerticalLayout implements DynamicWizardStep {
	
	private Upload upload;
	private ProgressIndicator pi;
	private TextField remoteURIInput;
	private Button btCancelUpload;
	private Label fileLabel;
	private SourceDocumentInfo sourceDocumentInfo;
	private boolean onAdvance = false;

	public LocationPanel(WizardStepListener listener, SourceDocumentInfo sourceDocumentInfo) {
		this.sourceDocumentInfo = sourceDocumentInfo;
		initComponents();
		initActions(listener);
	}

	private void initActions(final WizardStepListener wizardStepListener) {
		
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
				File uploadedFile = 
						new File(((CleaApplication)getApplication()).getTempDirectory(), event.getFilename());
				sourceDocumentInfo.setURI(uploadedFile.toURI());
				sourceDocumentInfo.setMimeType(event.getMIMEType());
				wizardStepListener.stepChanged(LocationPanel.this);
			}
		});
		
		upload.setReceiver(new Receiver() {
			
			public OutputStream receiveUpload(String filename, String mimeType) {
				try {
					String tempDir = ((CleaApplication)getApplication()).getTempDirectory();
					File uploadFile = new File(new File(tempDir), filename);
					if (uploadFile.exists()) {
						uploadFile.delete();
					}
			
					return new FileOutputStream(uploadFile);					
				} catch (FileNotFoundException e) {
					e.printStackTrace(); // TODO: handle
					return null;
				}
			}
		});
		
		btCancelUpload.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				upload.interruptUpload();
				
			}
		});
		
		remoteURIInput.addListener(new TextChangeListener() {
			
			public void textChange(TextChangeEvent event) {
				System.out.println("EVENT: " + event.getText());
				try {
					URL url = new URL(event.getText());
					System.out.println(url);
					sourceDocumentInfo.setURI(url.toURI());
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
		setMargin(true);
		setSpacing(true);
		
		remoteURIInput = new TextField();
		remoteURIInput.setCaption("Enter an URI that is accessible over the internet:");
		remoteURIInput.setWidth("100%");
		remoteURIInput.setTextChangeEventMode(TextChangeEventMode.EAGER);
		addComponent(remoteURIInput);
		
		Label localFileLabel = new Label("or upload a local file from your computer:");
		addComponent(localFileLabel);
		
		pi = new ProgressIndicator();
		pi.setVisible(false);
		
		HorizontalLayout uploadPanel = new HorizontalLayout();
		uploadPanel.setSpacing(true);
		
		upload = new Upload();
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
	
	public String getCaption() {
		return "Source Document location";
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
	
	public void stepActivated() {
		this.sourceDocumentInfo.setMimeType(null);
		this.sourceDocumentInfo.setURI(null);
	}
}
