package de.catma.ui.module.project.documentwizard;

import java.util.ArrayList;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

public class AddAnnotationCollectionsStep extends VerticalLayout implements WizardStep {
	
	private static interface EntryNameProvider {
		public String getName();
		public default UploadFile getUploadFile() {return null;};
	}
	
	private ProgressStep progressStep;
	private WizardContext wizardContext;
	private TreeData<EntryNameProvider> entries;
	private TreeDataProvider<EntryNameProvider> entryDataProvider;
	private TextField collectionNamePatternInput;
	private TreeGrid<EntryNameProvider> entryGrid;

	public AddAnnotationCollectionsStep(WizardContext wizardContext, ProgressStepFactory progressStepFactory, int stepNo) {
		this.wizardContext = wizardContext; 		
		this.progressStep = progressStepFactory.create(stepNo, "Create Annotation Collections");

		this.entries = new TreeData<AddAnnotationCollectionsStep.EntryNameProvider>();
		this.entryDataProvider = new TreeDataProvider<AddAnnotationCollectionsStep.EntryNameProvider>(entries);

		initComponents();
		initActions();
	}
	
	private void initActions() {
		this.collectionNamePatternInput.addValueChangeListener(event -> handleCollectionNamePatternChange(event));
		entries.clear();
	}

	private void handleCollectionNamePatternChange(ValueChangeEvent<String> event) {
		String pattern = event.getValue();
		
		wizardContext.put(DocumentWizard.WizardContextKey.COLLECTION_NAME_PATTERN, pattern);
		
		
		for (EntryNameProvider provider : new ArrayList<>(entries.getRootItems())) {
			for (EntryNameProvider child : new ArrayList<>(entries.getChildren(provider))) {
				entries.removeItem(child);
			}
		}

		for (EntryNameProvider provider : new ArrayList<>(entries.getRootItems())) {
		
			String name = pattern.replace("{{Title}}", provider.getUploadFile().getTitle());
			
			entries.addItem(provider, new EntryNameProvider() {
				@Override
				public String getName() {
					return name;
				}
			});
		
		}
		
		entryDataProvider.refreshAll();
		
		
	}

	private void initComponents() {
		setSizeFull();
		
		this.collectionNamePatternInput = new TextField("Enter a pattern for the generation of Collection names:");
		this.collectionNamePatternInput.setValueChangeMode(ValueChangeMode.LAZY);
		this.collectionNamePatternInput.setWidth("400px");
		addComponent(collectionNamePatternInput);
		
		this.entryGrid = new TreeGrid<AddAnnotationCollectionsStep.EntryNameProvider>(this.entryDataProvider);
		entryGrid.addColumn(entry -> entry.getName())
		.setCaption("Name");
		entryGrid.setSizeFull();
		
		addComponent(entryGrid);
		setExpandRatio(entryGrid, 1.0f);
	}

	
	@Override
	public void enter(boolean back) {
		@SuppressWarnings("unchecked")
		ArrayList<UploadFile> fileList = (ArrayList<UploadFile>) wizardContext.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);
		
		this.entries.clear();
		
		for (UploadFile uploadFile : fileList) {
			this.entries.addItem(null, new EntryNameProvider() {
				
				@Override
				public String getName() {
					return uploadFile.getTitle();
				}
				
				@Override
				public UploadFile getUploadFile() {
					return uploadFile;
				}
			});
		}
		
		String collectionNamePattern = (String) wizardContext.get(DocumentWizard.WizardContextKey.COLLECTION_NAME_PATTERN);
		if (collectionNamePattern == null) {
			collectionNamePattern = "{{Title}} Default Annotations";
		}
		this.collectionNamePatternInput.setValue(collectionNamePattern);
		
		entryGrid.expandRecursively(entries.getRootItems(), 1);
	}
	
	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return null; //intended
	}

	@Override
	public boolean canNext() {
		return false;
	}
	
	@Override
	public boolean canFinish() {
		return true;
	}
	
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}

}
