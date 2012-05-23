package de.catma.ui.repository.wizard;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.source.TechInfoSet;

public class LocationPanel extends VerticalLayout implements DynamicWizardStep {
	
	private TextField remoteURIInput;
	private UploadPanel uploadPanel;
	private boolean onAdvance = false;
	private WizardResult wizardResult;
	private Panel remoteURIInputPanel;

	public LocationPanel(WizardStepListener listener, WizardResult wizardResult) {
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
								event.getMIMEType(), 
								uploadPanel.getUploadedFileUri());
				
				wizardResult.getSourceDocumentInfo().setTechInfoSet(ti);
				onAdvance = true;
				wizardStepListener.stepChanged(LocationPanel.this);
			}
		});
		
		remoteURIInput.addListener(new TextChangeListener() {
			
			public void textChange(TextChangeEvent event) {
				
				try {
					String urlText = event.getText();
					if (urlText.toLowerCase().startsWith("www")) { //TODO: better scheme detection
						urlText = "http://" + urlText;
					}
					URL url = new URL(urlText);
					
					TechInfoSet ti = new TechInfoSet(null, url.toURI()); //TODO: mime type detection?
					wizardResult.getSourceDocumentInfo().setTechInfoSet(ti);
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
		setMargin(true, false, false, false);
		
		setSizeFull();
		HorizontalLayout remoteLayout = new HorizontalLayout();
		remoteLayout.setMargin(true);
		remoteLayout.setSpacing(true);
		remoteLayout.setSizeFull();
		
		remoteURIInputPanel = new Panel(remoteLayout);
		
		remoteURIInput = new TextField();
		remoteURIInput.setCaption("Enter an URI that is accessible over the internet:");
		remoteURIInput.setWidth("100%");
		remoteURIInput.setTextChangeEventMode(TextChangeEventMode.EAGER);
		remoteURIInputPanel.addComponent(remoteURIInput);
		remoteLayout.setExpandRatio(remoteURIInput, 2);
		
		addComponent(remoteURIInputPanel);
		
		Label localFileLabel = new Label("or upload a local file from your computer:");
		addComponent(localFileLabel);
		
		uploadPanel = new UploadPanel();
		
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
	
	public void stepActivated(){ /*not needed*/}
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}

	public void stepDeactivated() { /*not needed */}
}
