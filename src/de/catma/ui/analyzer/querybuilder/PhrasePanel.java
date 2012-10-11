package de.catma.ui.analyzer.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;

public class PhrasePanel extends AbstractSearchPanel {
	
	private Panel wordSequencePanel;
	private List<WordPanel> wordPanels;
	private Button btAddWordPanel;
	private ResultPanel resultPanel;

	public PhrasePanel(
			ToggleButtonStateListener toggleButtonStateListener,
			QueryTree queryTree,
			QueryOptions queryOptions) {
		super(toggleButtonStateListener, queryTree, queryOptions);
		wordPanels = new ArrayList<WordPanel>();
		initComponents();
		initActions();
	}

	private void initActions() {
		btAddWordPanel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				WordPanel wordPanel = 
						new WordPanel(true, wordPanels, new ValueChangeListener() {
							
							public void valueChange(ValueChangeEvent event) {
								showInPreviw();
							}
						});
				addWordPanel(wordPanel);
			}
		});
		resultPanel.addBtShowInPreviewListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				showInPreviw();
			}
		});
	}

	private void showInPreviw() {
		StringBuilder builder = new StringBuilder();
		String conc="";
		for (WordPanel panel : wordPanels) {
			builder.append(conc);
			builder.append(panel.getWildcardWord());
			conc = " ";
		}
		
		if (!builder.toString().trim().isEmpty()) {
			if (curQuery != null) {
				queryTree.removeLast();
			}
			curQuery = "wild=\"" + builder.toString() + "\"";
			resultPanel.setQuery(curQuery);
			queryTree.add(curQuery);
			onFinish = !isComplexQuery();
			onAdvance = true;
			toggleButtonStateListener.stepChanged(this);
		}
		else {
			onFinish = false;
			onAdvance = false;
		}
		
	}

	private void initComponents() {
		VerticalSplitPanel splitPanel = new VerticalSplitPanel();
		Component searchPanel = createSearchPanel();
		splitPanel.addComponent(searchPanel);
		resultPanel = new ResultPanel(queryOptions);
		splitPanel.addComponent(resultPanel);
		addComponent(splitPanel);
		super.initComponents(splitPanel);
	}

	private Component createSearchPanel() {
		VerticalLayout searchPanel = new VerticalLayout();
		searchPanel.setSpacing(true);
		
		wordSequencePanel = new Panel(new HorizontalLayout());
		searchPanel.addComponent(wordSequencePanel);
		
		WordPanel firstWordPanel = new WordPanel(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				showInPreviw();
			}
		});
		addWordPanel(firstWordPanel);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		
		Label addWordPanelLabel = 
				new Label("If your phrase contains more words you can");
		buttonPanel.addComponent(addWordPanelLabel);
		buttonPanel.setComponentAlignment(
				addWordPanelLabel, Alignment.MIDDLE_CENTER);
		
		btAddWordPanel = new Button("add another word!");
		buttonPanel.addComponent(btAddWordPanel);
		searchPanel.addComponent(buttonPanel);
		
		return searchPanel;
	}


	private void addWordPanel(WordPanel wordPanel) {
		wordSequencePanel.addComponent(wordPanel);
		wordPanels.add(wordPanel);
	}
	
	@Override
	public String getCaption() {
		return "How does your phrase look like?";
	}

	public Component getContent() {
		return this;
	}

	@Override
	public void attach() {
		super.attach();
		getParent().setHeight("100%");
	}
	
	
	@Override
	public String toString() {
		return "by word or phrase";
	}

}
