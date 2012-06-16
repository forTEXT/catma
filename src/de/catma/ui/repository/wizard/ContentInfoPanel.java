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

	public void stepActivated() {
	
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
	
	public void stepDeactivated() {
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
}
