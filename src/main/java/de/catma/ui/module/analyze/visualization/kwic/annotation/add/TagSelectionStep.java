package de.catma.ui.module.analyze.visualization.kwic.annotation.add;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.tag.TagDefinition;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.tags.TagSelectionPanel;
import de.catma.ui.module.tags.TagSelectionPanel.TagSelectionChangedListener;

class TagSelectionStep extends VerticalLayout implements WizardStep {
	
	private TagSelectionPanel tagSelectionPanel;
	private TagDefinition selectedTag;
	private Project project;
	private ProgressStep progressStep;
	private StepChangeListener stepChangeListener;
	private PropertySelectionStep nextStep;
	private WizardContext context;

	public TagSelectionStep(EventBus eventBus, Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		this.project = project;
		this.context = context;
		this.progressStep = progressStepFactory.create(1, "Select a Tag");
		this.nextStep = new PropertySelectionStep(eventBus, project, context, progressStepFactory);
		initComponents();
		initActions();
	}

	private void initActions() {
		tagSelectionPanel.addTagSelectionChangedListener(tag -> handleTagSelection(tag));
	}

	private void handleTagSelection(TagDefinition tag) {
		this.selectedTag = tag;

		if (tag != null) {
			progressStep.setCompleted(
				project.getTagManager().getTagLibrary().getTagPath(tag.getUuid()));
			
			if (tag.getUserDefinedPropertyDefinitions().isEmpty()) {
				nextStep.getProgressStep().setCompleted("no properties available");
				nextStep.setSkipped(true);
			}
			else {
				nextStep.getProgressStep().resetCurrent();
				nextStep.setSkipped(false);
			}
		}
		else {
			progressStep.resetCurrent();
			nextStep.getProgressStep().resetCurrent();
			nextStep.setSkipped(false);
		}
		
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}
	private void initComponents() {
		setSizeFull();
		setMargin(false);
		
		tagSelectionPanel = new TagSelectionPanel(project);
		addComponent(tagSelectionPanel);
	}

	public void addTagSelectionChangedListener(TagSelectionChangedListener tagSelectionChangedListener) {
		tagSelectionPanel.addTagSelectionChangedListener(tagSelectionChangedListener);
	}
	
	public ProgressStep getProgressStep() {
		return progressStep;
	}
	
	@Override
	public boolean isValid() {
		return selectedTag != null;
	}
	
	@Override
	public WizardStep getNextStep() {
		if (selectedTag != null && selectedTag.getUserDefinedPropertyDefinitions().isEmpty()) {
			nextStep.getProgressStep().setFinished();
			return nextStep.getNextStep();
		}
		else {
			nextStep.setTag(selectedTag);
			
			return nextStep;
		}
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}
	
	@Override
	public void exit(boolean back) {
		if (back) {
			context.put(AnnotationWizardContextKey.TAG, null);
		}
		else {
			context.put(AnnotationWizardContextKey.TAG, selectedTag);			
		}
	}
}
