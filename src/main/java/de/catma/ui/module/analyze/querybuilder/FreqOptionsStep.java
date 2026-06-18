package de.catma.ui.module.analyze.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
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

public class FreqOptionsStep extends VerticalLayout implements WizardStep {
	
	private ProgressStep progressStep;
	private QueryTree queryTree;
	private String curQuery = null;
	private TextField curQueryField;
	private StepChangeListener stepChangeListener;
	private Project project;
	private WizardContext context;
	private ProgressStepFactory progressStepFactory;
	private int nextStepNo;

	private ComboBox<FreqComparator> freqComparatorCombo;
	private TextField freq1Input;
	private TextField freq2Input;
	private Label andLabel;

	public FreqOptionsStep(
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
		freq1Input.addValueChangeListener(event -> handleFreqChange());
		freq2Input.addValueChangeListener(event -> handleFreqChange());
		freqComparatorCombo.addValueChangeListener(event -> handleFreqChange());
		freqComparatorCombo.addValueChangeListener(event -> {
			freq2Input.setVisible(event.getValue().isRange());
			andLabel.setVisible(event.getValue().isRange());
		});
	}

	private void handleFreqChange() {
		if (curQuery != null) {
			queryTree.removeLast();
		}
		
		StringBuilder builder = new StringBuilder("freq ");
		FreqComparator curComparator = freqComparatorCombo.getValue(); 
		
		builder.append(curComparator.getComparator());
		builder.append(" ");
		builder.append(freq1Input.getValue());
		
		if (curComparator.isRange()) {
			builder.append("-");
			builder.append(freq2Input.getValue());
		}
		
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
	
		List<FreqComparator> freqComparators = new ArrayList<FreqComparator>();
		FreqComparator exactlyFreqComp = new FreqComparator("exactly", "=");
		freqComparators.add(exactlyFreqComp);
		
		freqComparators.add(
                new FreqComparator("more than", ">"));
		freqComparators.add(
                new FreqComparator("less than", "<"));
		freqComparators.add(
                new FreqComparator("more or equal than", ">="));
		freqComparators.add(
                new FreqComparator("less or equal than", "<="));
		freqComparators.add(
                new FreqComparator("between", "=", true));
		
		searchPanel.addComponent(new Label("The word shall appear")); 
		
		freqComparatorCombo = new ComboBox<FreqComparator>(null, freqComparators);
		freqComparatorCombo.setEmptySelectionAllowed(false);
		freqComparatorCombo.setValue(exactlyFreqComp);

		searchPanel.addComponent(freqComparatorCombo);
		
		freq1Input = new TextField();
		
		searchPanel.addComponent(freq1Input);
		searchPanel.setExpandRatio(freq1Input, 0.5f);
		
		andLabel = new Label("and");
		andLabel.setVisible(false);
		searchPanel.addComponent(andLabel);

		freq2Input = new TextField();
		freq2Input.setVisible(false);

		searchPanel.addComponent(freq2Input);
		searchPanel.setExpandRatio(freq2Input, 0.5f);
		
		searchPanel.addComponent(new Label("times.")); 

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
		if ((freq1Input.getValue() != null) && !freq1Input.getValue().isEmpty()) {
			try {
				Integer.valueOf(freq1Input.getValue());
			}
			catch (NumberFormatException nfe) {
				Notification.show("Info", "Please enter a valid integer for the frequency!", Type.HUMANIZED_MESSAGE);
				return false;
			}
		}
		else {
			Notification.show("Info", "Please enter a frequency!", Type.HUMANIZED_MESSAGE);
			return false;
		}
		
		if (freqComparatorCombo.getValue().isRange()) {
			if ((freq2Input.getValue() != null) && !freq2Input.getValue().isEmpty()) {
				try {
					Integer.valueOf(freq2Input.getValue());
				}
				catch (NumberFormatException nfe) {
					Notification.show("Info", "Please enter a valid integer for the frequency!", Type.HUMANIZED_MESSAGE);
					return false;
				}
			}
			else {
				Notification.show("Info", "Please enter a frequency!", Type.HUMANIZED_MESSAGE);
				return false;
			}			
		}
		
		return curQuery != null;
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}

	@Override
	public String getStepDescription() {
		return "How often should the word appear?";
	}

	@Override
	public String toString() {
		return "by frequency";
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
