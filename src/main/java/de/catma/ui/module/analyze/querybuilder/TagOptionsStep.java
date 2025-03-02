package de.catma.ui.module.analyze.querybuilder;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.tag.TagDefinition;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.tags.TagSelectionPanel;

public class TagOptionsStep extends VerticalLayout implements WizardStep {
	
	private ProgressStep progressStep;
	private QueryTree queryTree;
	private TagSelectionPanel tagSelectionPanel;
	private String curQuery = null;
	private TextField curQueryField;
	private StepChangeListener stepChangeListener;
	private Project project;
	private WizardContext context;
	private ProgressStepFactory progressStepFactory;
	private int nextStepNo;

	public TagOptionsStep(
			int stepNo,
			Project project, WizardContext context, 
			ProgressStep progressStep, 
			ProgressStepFactory progressStepFactory) {
		this.nextStepNo = stepNo+1;
		this.project = project;
		this.context = context;
		this.progressStepFactory = progressStepFactory;

		this.queryTree = (QueryTree) context.get(QueryBuilder.ContextKey.QUERY_TREE);
		this.progressStep = progressStep;
				
		initComponents();
		initActions();
	}

	private void initActions() {
		tagSelectionPanel.addTagSelectionChangedListener(tag -> handleTagSelection(tag));
	}

	private void initComponents() {
		setSizeFull();
		setMargin(true);
		
		tagSelectionPanel = new TagSelectionPanel(project);
		addComponent(tagSelectionPanel);
		setExpandRatio(tagSelectionPanel, 1);
		
		curQueryField = new TextField("Your query so far: ");
		curQueryField.setReadOnly(true);
		curQueryField.setVisible(false);
		curQueryField.setWidth("100%");
		
		addComponent(curQueryField);
		setComponentAlignment(curQueryField, Alignment.BOTTOM_CENTER);
	}
	
	private void setCurQuery(String query) {
		curQueryField.setReadOnly(false);
		if (query == null) {
			query = "";
		}
		
		curQueryField.setValue(query);
		curQueryField.setReadOnly(true);
		curQueryField.setVisible(!query.isEmpty());
	}
	
	private void handleTagSelection(TagDefinition tag) {
		
		if (curQuery != null) {
			queryTree.removeLast();
		}

		if (tag != null) {
			StringBuilder builder = new StringBuilder("tag=\""); //$NON-NLS-1$
			builder.append(project.getTagManager().getTagLibrary().getTagPath(tag.getUuid()));
			builder.append("%\""); //$NON-NLS-1$
			
			curQuery = builder.toString();
			queryTree.add(curQuery);		
		}
		
		
		setCurQuery(queryTree.toString());
		
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return new ComplexTypeSelectionStep(nextStepNo, project, context, progressStepFactory);
	}

	@Override
	public boolean isValid() {
		return curQuery != null;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}

	@Override
	public String getStepDescription() {
		return "Select a Tag";
	}

	@Override
	public String toString() {
		return "by tag";
	}
	
	@Override
	public boolean canFinish() {
		return true;
	}
	
	@Override
	public void enter(boolean back) {
		if ((back) && (stepChangeListener != null)) {
			stepChangeListener.stepChanged(this);
		}
	}
	
	@Override
	public void exit(boolean back) {
		if (back) {
			queryTree.removeLast();
		}
	}
}
