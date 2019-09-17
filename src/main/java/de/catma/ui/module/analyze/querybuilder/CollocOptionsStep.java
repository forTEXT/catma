package de.catma.ui.module.analyze.querybuilder;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

public class CollocOptionsStep extends VerticalLayout implements WizardStep {
	
	private ProgressStep progressStep;
	private QueryTree queryTree;
	private String curQuery = null;
	private TextField curQueryField;
	private StepChangeListener stepChangeListener;
	private Project project;
	private WizardContext context;
	private ProgressStepFactory progressStepFactory;
	private int nextStepNo;

	private TextField wordInput;
	private TextField collocInput;
	private TextField spanSizeInput;
	
	
	public CollocOptionsStep(
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
		wordInput.addValueChangeListener(event -> handleCollocChange());
		collocInput.addValueChangeListener(event -> handleCollocChange());
		spanSizeInput.addValueChangeListener(event -> handleCollocChange());
	}

	private void handleCollocChange() {
		if (curQuery != null) {
			queryTree.removeLast();
		}
		
		StringBuilder builder = new StringBuilder("\""); //$NON-NLS-1$
		builder.append(wordInput.getValue());
		builder.append("\" & \""); //$NON-NLS-1$
		builder.append(collocInput.getValue());
		builder.append("\" "); //$NON-NLS-1$
		builder.append(spanSizeInput.getValue());
		
		curQuery = builder.toString();
		queryTree.add(curQuery);		
		
		setCurQuery(queryTree.toString());
		
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}

	private void initComponents() {
		setSizeFull();
		setMargin(true);
		
		HorizontalLayout searchPanel = new HorizontalLayout();
		searchPanel.setMargin(new MarginInfo(true, false, false, false));
		
		searchPanel.setSpacing(true);
		searchPanel.addStyleName("query-builder-colloc-options-step-search-panel");
	
		wordInput = new TextField("Search for all occurrences of");
		searchPanel.addComponent(wordInput);
		
		collocInput = new TextField("that appear near"); 
		searchPanel.addComponent(collocInput);
		
		spanSizeInput = new TextField("within a token span of", "5");  //$NON-NLS-2$
		searchPanel.addComponent(spanSizeInput);
		
		addComponent(searchPanel);
		setExpandRatio(searchPanel, 1);
		
		
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
		if ((wordInput.getValue() == null) || wordInput.getValue().isEmpty()) {
			Notification.show("Info", "Please enter a word!", Type.HUMANIZED_MESSAGE);
			return false;
		}
		if ((collocInput.getValue() == null) || collocInput.getValue().isEmpty()) {
			Notification.show("Info", "Please enter a collocate!", Type.HUMANIZED_MESSAGE);
			return false;
		}
		
		if ((spanSizeInput.getValue() != null) && !spanSizeInput.getValue().isEmpty()) {
			try {
				Integer.valueOf(spanSizeInput.getValue());
			}
			catch (NumberFormatException nfe) {
				Notification.show("Info", "Please enter a valid integer for the token span!", Type.HUMANIZED_MESSAGE);
				return false;
			}
		}
		else {
			Notification.show("Info", "Please enter a token span!", Type.HUMANIZED_MESSAGE);
			return false;
		}			
		
		return curQuery != null;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}

	@Override
	public String getStepDescription() {
		return "Give the word and its collocate";
	}

	@Override
	public String toString() {
		return "by collocation";
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
