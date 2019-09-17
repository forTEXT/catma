package de.catma.ui.module.analyze.querybuilder;

import java.util.ArrayList;
import java.util.Arrays;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.queryengine.MatchMode;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;


public class ComplexTypeSelectionStep extends VerticalLayout implements WizardStep {
	
	private static class TagMatchModeItem {
		
		private String displayText;
		private MatchMode tagMatchMode;
		
		public TagMatchModeItem(String displayText, MatchMode tagMatchMode) {
			this.displayText = displayText;
			this.tagMatchMode = tagMatchMode;
		}
		
		public MatchMode getTagMatchMode() {
			return tagMatchMode;
		}
		
		@Override
		public String toString() {
			return displayText;
		}
	}
	
	static enum ComplexTypeOption {
		UNION(","), //$NON-NLS-1$
		EXCLUSION("-"), //$NON-NLS-1$
		REFINMENT("where"), //$NON-NLS-1$
		;
		String queryElement;
		
		private ComplexTypeOption(String queryElement) {
			this.queryElement = queryElement;
		}
		
		public String getQueryElement() {
			return queryElement;
		}
	}

	private QueryTree queryTree;
	private ProgressStep progressStep;
	private TextField curQueryField;
	private RadioButtonGroup<ComplexTypeOption> complexTypeSelect;
	private ComboBox<TagMatchModeItem> tagMatchModeCombo;
	private int nextStepNo;
	private Project project;
	private WizardContext context;
	private ProgressStepFactory progressStepFactory;

	public ComplexTypeSelectionStep(
			int stepNo, 
			Project project, 
			WizardContext context, 
			ProgressStepFactory progressStepFactory) {
		this.queryTree = (QueryTree) context.get(QueryBuilder.ContextKey.QUERY_TREE);
		this.progressStep = progressStepFactory.create(stepNo, "Add, Exclude, Refine");
		this.project = project;
		this.context = context;
		this.progressStepFactory = progressStepFactory;		
		this.nextStepNo = stepNo+1;
		initComponents();
		initActions();
		queryTree.add(
				ComplexTypeOption.UNION.getQueryElement(),
				"");
		setCurQuery(queryTree.toString());
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

	private void initActions() {
		complexTypeSelect.addValueChangeListener(event -> handleComplexTypeChange());
		tagMatchModeCombo.addValueChangeListener(event -> handleTagMatchModeChange());
	}

	private void handleTagMatchModeChange() {
		queryTree.removeLast();

		queryTree.add(
			((ComplexTypeOption)complexTypeSelect.getValue()).getQueryElement(),
			((TagMatchModeItem)tagMatchModeCombo.getValue()).getTagMatchMode().name().toLowerCase());

		setCurQuery(queryTree.toString());
	}

	private void handleComplexTypeChange() {
		queryTree.removeLast();

		String postfix = ""; //$NON-NLS-1$
		if(!complexTypeSelect.getValue().equals(
			ComplexTypeOption.UNION)) {
			postfix = ((TagMatchModeItem)tagMatchModeCombo.getValue()).getTagMatchMode().name().toLowerCase();
		}
		
		queryTree.add(
			complexTypeSelect.getValue().getQueryElement(),
			postfix);

		tagMatchModeCombo.setVisible(!
			complexTypeSelect.getValue().equals(
				ComplexTypeOption.UNION));
		
		setCurQuery(queryTree.toString());
	}


	private void initComponents() {
		setSizeFull();
		setMargin(true);
		
		complexTypeSelect = 
				new RadioButtonGroup<>("", Arrays.<ComplexTypeOption>asList(ComplexTypeOption.values())); //$NON-NLS-1$
		
		complexTypeSelect.setItemCaptionGenerator(option -> {
			switch (option) {
			case UNION: return "add more results";
			case EXCLUSION: return "exclude hits from previous results";
			case REFINMENT: return "refine previous results";
			default: return option.name();
			}
			
		});
		
		addComponent(complexTypeSelect);
		setComponentAlignment(complexTypeSelect, Alignment.MIDDLE_CENTER);
		complexTypeSelect.setValue(ComplexTypeOption.UNION);
		
		ArrayList<TagMatchModeItem> tagMatchModeOptions = new ArrayList<>();
		TagMatchModeItem exactMatchItem = 
				new TagMatchModeItem("exact match", MatchMode.EXACT); 
		tagMatchModeOptions.add(exactMatchItem);
		tagMatchModeOptions.add(
				new TagMatchModeItem("boundary match",  
						MatchMode.BOUNDARY));
		tagMatchModeOptions.add(
				new TagMatchModeItem("overlap match",  
						MatchMode.OVERLAP));
		tagMatchModeCombo = new ComboBox<>("Please choose what you consider a match:", tagMatchModeOptions); 
		
		tagMatchModeCombo.setDescription(
			"The three different match modes influence the way tags refine your search results:<ul><li>exact match - the tag type boundaries have to match exactly to keep a result item in the result set</li><li>boundary match - result items that should be kept in the result set must start and end within the boundaries of the tag</li><li>overlap - the result items that should be kept in the result set must overlap with the range of the tag</li></ul>"); 
		tagMatchModeCombo.setValue(exactMatchItem);
		
		addComponent(tagMatchModeCombo);
		setComponentAlignment(tagMatchModeCombo, Alignment.MIDDLE_CENTER);		
		setExpandRatio(tagMatchModeCombo, 1);
		tagMatchModeCombo.setVisible(false);
		
		curQueryField = new TextField("Your query so far: ");
		curQueryField.setReadOnly(true);
		curQueryField.setVisible(false);
		curQueryField.setWidth("100%");
		
		addComponent(curQueryField);
		setComponentAlignment(curQueryField, Alignment.BOTTOM_CENTER);
	}

	@Override
	public ProgressStep getProgressStep() {
		return progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return new SearchTypeSelectionStep(
				nextStepNo,
				project, 
				context,
				progressStepFactory);
	}

	@Override
	public boolean isValid() {
		return true; // always true
	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
	
	
	@Override
	public void exit(boolean back) {
		if (back) {
			queryTree.removeLast();
		}
	}
	
	@Override
	public boolean canFinish() {
		return false;
	}
}
