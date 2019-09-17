package de.catma.ui.module.analyze.querybuilder;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.component.Slider;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

public class SimilOptionsStep extends VerticalLayout implements WizardStep {
	
	private ProgressStep progressStep;
	private QueryTree queryTree;
	private TextField exampleInput;
	private Slider similSlider;
	private String curQuery = null;
	private TextField curQueryField;
	private StepChangeListener stepChangeListener;
	private Project project;
	private WizardContext context;
	private ProgressStepFactory progressStepFactory;
	private int nextStepNo;

	public SimilOptionsStep(
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
		exampleInput.addValueChangeListener(event -> handleSimilChange());
		similSlider.addValueChangeListener(event -> handleSimilChange());
	}

	private void initComponents() {
		setSizeFull();
		setMargin(true);
		
		exampleInput = new TextField("The word is similar to");
		
		addComponent(exampleInput);
		
		similSlider = new Slider("grade of similarity", 0, 100, "%");
		
		similSlider.setValue(80.0);
		
		addComponent(similSlider);
		setExpandRatio(similSlider, 1);
		
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
	
	private void handleSimilChange() {
		
		if (curQuery != null) {
			queryTree.removeLast();
		}

		String example = exampleInput.getValue();
		if (example == null) {
			example = "";
		}
		
		double grade = similSlider.getValue();
		
		if (!example.isEmpty()) { 
			StringBuilder builder = new StringBuilder("simil=\""); //$NON-NLS-1$
			builder.append(example);
			builder.append("\" "); //$NON-NLS-1$
			builder.append(new Double(grade).intValue());
			builder.append("%"); //$NON-NLS-1$
			
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
		return "Give an example";
	}

	@Override
	public String toString() {
		return "by grade of similarity";
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
