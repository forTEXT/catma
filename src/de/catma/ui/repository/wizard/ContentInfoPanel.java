package de.catma.ui.repository.wizard;

import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;

import de.catma.core.document.source.ContentInfoSet;

public class ContentInfoPanel extends HorizontalLayout implements
		DynamicWizardStep {
	
	
	private WizardResult wizardResult;
	private WizardStepListener wizardStepListener;
	private Form contentInfoForm;
	private String incomingTitle;
	
	public ContentInfoPanel(WizardStepListener wizardStepListener,
			WizardResult wizardResult) {
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
		
		incomingTitle = contentInfoSet.getTitle();
	}

	public boolean onFinish() {
		return true;
	}
	
	public boolean onFinishOnly() {
		return true;
	}
	
	public void stepDeactivated() {
		if (!incomingTitle.equals(wizardResult.getSourceDocumentInfo().getContentInfoSet().getTitle())) {
			wizardResult.getSourceDocument().setTitle(
					wizardResult.getSourceDocumentInfo().getContentInfoSet().getTitle());
		}
	}
}
