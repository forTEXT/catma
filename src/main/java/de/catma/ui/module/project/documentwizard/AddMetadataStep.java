package de.catma.ui.module.project.documentwizard;

import java.util.ArrayList;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.FileType;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

public class AddMetadataStep extends VerticalLayout implements WizardStep {
	
	private WizardContext wizardContext;
	private WizardStep nextStep;
	private ProgressStep progressStep;
	private ArrayList<UploadFile> fileList;
	private ListDataProvider<UploadFile> fileDataProvider;
	private Grid<UploadFile> fileGrid;
	private ActionGridComponent<Grid<UploadFile>> fileActionGridComponent;
	private ProgressStepFactory progressStepFactory;

	
	@SuppressWarnings("unchecked")
	public AddMetadataStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory) {
		this.progressStepFactory = progressStepFactory;
		this.wizardContext = wizardContext; 
		this.progressStep = progressStepFactory.create(3, "Add Some Metadata");
		
		this.fileList = (ArrayList<UploadFile>) wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
		this.fileDataProvider = new ListDataProvider<UploadFile>(this.fileList);
		initComponents();
		initActions();
	}
	

	private void initActions() {
		fileActionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem("Set Author", menuItem -> {
			if (fileGrid.getSelectedItems().isEmpty()) {
				Notification.show("Info", "Please select one or more entries first!", Type.HUMANIZED_MESSAGE);
			}
			else {
				SingleTextInputDialog authorInputDialog = new SingleTextInputDialog("Set the author", "Author", result -> {
					fileGrid.getSelectedItems().forEach(uploadFile -> uploadFile.setAuthor(result));
					fileDataProvider.refreshAll();
				});
				
				authorInputDialog.show();
			}
		});
		fileActionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem("Set Publisher", menuItem -> {
			if (fileGrid.getSelectedItems().isEmpty()) {
				Notification.show("Info", "Please select one or more entries first!", Type.HUMANIZED_MESSAGE);
			}
			else {
				SingleTextInputDialog publisherInputDialog = new SingleTextInputDialog("Set the publisher", "Publisher", result -> {
					fileGrid.getSelectedItems().forEach(uploadFile -> uploadFile.setPublisher(result));
					fileDataProvider.refreshAll();
				});
				
				publisherInputDialog.show();
			}
		});
		fileActionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem("Set Description", menuItem -> {
			if (fileGrid.getSelectedItems().isEmpty()) {
				Notification.show("Info", "Please select one or more entries first!", Type.HUMANIZED_MESSAGE);
			}
			else {
				SingleTextInputDialog descriptionInputDialog = new SingleTextInputDialog("Set the description", "Description", result -> {
					fileGrid.getSelectedItems().forEach(uploadFile -> uploadFile.setDescription(result));
					fileDataProvider.refreshAll();
				});
				
				descriptionInputDialog.show();
			}
		});
	}


	private void initComponents() {
		setSizeFull();
        Label infoLabel = new Label(
        		"Add some metadata by double clicking on a row");
        infoLabel.setContentMode(ContentMode.HTML);
		addComponent(infoLabel);
		
        fileGrid = new Grid<UploadFile>(fileDataProvider);
        fileGrid.setSizeFull();
        
		TextField titleEditor = new TextField();
		TextField authorEditor = new TextField();
		TextField descEditor = new TextField();
		TextField publisherEditor = new TextField();

        
        fileGrid.addColumn(UploadFile::getOriginalFilename)
        	.setCaption("File")
        	.setWidth(150)
        	.setDescriptionGenerator(UploadFile::getOriginalFilename);
        fileGrid.addColumn(UploadFile::getMimetype)
        	.setCaption("Type")
        	.setWidth(150)
        	.setDescriptionGenerator(UploadFile::getMimetype);

        fileGrid.addColumn(UploadFile::getTitle)
        	.setCaption("Title")
        	.setExpandRatio(1)
        	.setEditorComponent(titleEditor, UploadFile::setTitle);
        fileGrid.addColumn(UploadFile::getAuthor)
        	.setCaption("Author")
        	.setExpandRatio(1)
        	.setEditorComponent(authorEditor, UploadFile::setAuthor);
        fileGrid.addColumn(UploadFile::getPublisher)
        	.setCaption("Publisher")
        	.setExpandRatio(1)
        	.setEditorComponent(publisherEditor, UploadFile::setPublisher);
        fileGrid.addColumn(UploadFile::getDescription)
        	.setCaption("Description")
        	.setExpandRatio(1)
        	.setEditorComponent(descEditor, UploadFile::setDescription);
        
        fileGrid.getEditor().setEnabled(true).setBuffered(false);
   
        
        fileActionGridComponent = new ActionGridComponent<Grid<UploadFile>>(new Label("Metadata"), fileGrid); 
        fileActionGridComponent.setMargin(false);
        fileActionGridComponent.getActionGridBar().setAddBtnVisible(false);
        
        addComponent(fileActionGridComponent);
     
        setExpandRatio(fileActionGridComponent, 0.6f);
	}


	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return nextStep;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this); // always valid & can next
		}
	}
	
	@Override
	public void enter(boolean back) {
		if (back) {
			return;
		}
		boolean containsIntrinsicMarkup = false;
		for (UploadFile uploadFile : fileList) {
			if ((uploadFile.getTitle() == null) || uploadFile.getTitle().isEmpty()) {
				uploadFile.setTitle(makeTitleFromFileName(uploadFile.getOriginalFilename()));
			}
			
			if (uploadFile.getMimetype().equals(FileType.XML2.getMimeType())) {
				containsIntrinsicMarkup = true;
			}
		}
		fileDataProvider.refreshAll();
		
		if (containsIntrinsicMarkup) {
			nextStep = new ImportIntrinsicMarkupStep(wizardContext, progressStepFactory);
		}
		else {
			nextStep = new AddAnnotationCollectionsStep(wizardContext, progressStepFactory, 4);
		}
	}
	
	private String makeTitleFromFileName(String fileName) {
		int indexOfLastFullStop = fileName.lastIndexOf('.');
		if(indexOfLastFullStop > 0){
			fileName = fileName.substring(0, indexOfLastFullStop);
		}
		
		return fileName.replace("_", " ").trim(); //$NON-NLS-1$ //$NON-NLS-2$
		
	}

}
