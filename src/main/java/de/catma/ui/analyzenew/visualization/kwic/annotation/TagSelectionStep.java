package de.catma.ui.analyzenew.visualization.kwic.annotation;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.tag.TagDefinition;
import de.catma.ui.modules.tags.TagSelectionPanel;
import de.catma.ui.modules.tags.TagSelectionPanel.TagSelectionChangedListener;

public class TagSelectionStep extends VerticalLayout implements WizardStep {
	
	private TagSelectionPanel tagSelectionPanel;
	private TagDefinition selectedTag;
	private Repository project;
	private ProgressStep progressStep;
	private StepChangeListener stepChangeListener;
	private PropertySelectionStep nextStep;
	private WizardContext context;

	public TagSelectionStep(EventBus eventBus, Repository project, WizardContext context, ProgressStepFactory progressStepFactory) {
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
				nextStep.getProgressStep().setCompleted("no Properties available");
			}
			else {
				nextStep.getProgressStep().reset();
			}
		}
		else {
			progressStep.reset();
			nextStep.getProgressStep().reset();
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
			nextStep.setFinished();
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
	public void setFinished() {
		progressStep.setFadedOut();
	}
}
