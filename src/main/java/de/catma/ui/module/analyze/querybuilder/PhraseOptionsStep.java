package de.catma.ui.module.analyze.querybuilder;

import java.util.Iterator;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.Project;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;
import de.catma.ui.module.analyze.querybuilder.WordPanel.WordConfigurationChangedListener;

public class PhraseOptionsStep extends VerticalLayout implements WizardStep {

	private Button btAddWordPanel;
	private QueryTree queryTree;
	private String curQuery = null;
	private ProgressStep progressStep;
	private StepChangeListener stepChangeListener;
	private HorizontalLayout wordSequencePanelContent;
	private TextField curQueryField;
	
	public PhraseOptionsStep(
			Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		queryTree = (QueryTree) context.get(QueryBuilder.ContextKey.QUERY_TREE);
		this.progressStep = progressStepFactory.create(2, getProgressStepDescription());
		initComponents();
		initActions();
		setCurQuery(queryTree.toString());
	}

	private void initActions() {
		btAddWordPanel.addClickListener(
			event -> addWordPanel(new WordPanel(true, changedPanel -> handlePhraseChange())));
	}

	private void initComponents() {
		setSizeFull();
		setMargin(true);
		
		wordSequencePanelContent = new HorizontalLayout();
		wordSequencePanelContent.setMargin(false);
		
		Panel wordSequencePanel = new Panel(wordSequencePanelContent);
		wordSequencePanel.setSizeFull();
		wordSequencePanel.setStyleName(MaterialTheme.PANEL_BORDERLESS);
		
		WordPanel firstWordPanel = new WordPanel(new WordConfigurationChangedListener() {
			@Override
			public void wordConfigurationChanged(WordPanel wordPanel) {
				handlePhraseChange();
			}
		});
		
		addWordPanel(firstWordPanel);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.addComponent(new Label("If your phrase contains more words you can"));
		btAddWordPanel = new Button("add another word!");
		buttonPanel.addComponent(btAddWordPanel);
		
		addComponent(wordSequencePanel);
		setExpandRatio(wordSequencePanel, 0.7f);
		addComponent(buttonPanel);
		setExpandRatio(buttonPanel, 0.3f);
		
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
	
	private void addWordPanel(WordPanel wordPanel) {
		wordSequencePanelContent.addComponent(wordPanel);
	}

	private void handlePhraseChange() {
		
		StringBuilder builder = new StringBuilder();
		String conc=""; //$NON-NLS-1$
		
		for (Iterator<Component> iter = wordSequencePanelContent.iterator(); 
				iter.hasNext();) {
			WordPanel wordPanel = (WordPanel)iter.next();
			builder.append(conc);
			builder.append(wordPanel.getWildcardWord());
			conc = " "; //$NON-NLS-1$
		}
		
		if (curQuery != null) {
			queryTree.removeLast();
		}
		if (!builder.toString().trim().isEmpty()) {
			curQuery = "wild=\"" + builder.toString() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			queryTree.add(curQuery);
		}

		setCurQuery(queryTree.toString());
		
		if (stepChangeListener != null) {
			stepChangeListener.stepChanged(this);
		}
	}

	@Override
	public ProgressStep getProgressStep() {
		return this.progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		// TODO Auto-generated method stub
		return null;
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
	public void setFinished() {
		progressStep.setFinished();
	}

	@Override
	public void setCurrent() {
		progressStep.setCurrent();
	}
	
	@Override
	public String toString() {
		return "by word or phrase pattern";
	}

	@Override
	public String getProgressStepDescription() {
		return "How does your phrase look like?";
	}
}
