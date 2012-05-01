package de.catma.ui.analyzer;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

public class AnalyzerView extends VerticalLayout {
	
	private TextField searchInput;
	private Button btExecSearch;
	private Button btQueryBuilder;
	private Button btWordList;
	private HierarchicalContainer documentsContainer;
	private Tree documentsTree;
	private TabSheet resultTabSheet;
	
	public AnalyzerView() {
		initComponents();
	}

	private void initComponents() {
		setSizeFull();
		Component searchPanel = createSearchPanel();
		Component convenienceButtonPanel = createConvenienceButtonPanel();
		VerticalLayout searchAndConveniencePanel = new VerticalLayout();
		searchAndConveniencePanel.setSizeFull();
		searchAndConveniencePanel.setSpacing(true);
		searchAndConveniencePanel.setMargin(true);
		searchAndConveniencePanel.addComponent(searchPanel);
		searchAndConveniencePanel.addComponent(convenienceButtonPanel);
		
		Component documentsPanel = createDocumentsPanel();

		HorizontalSplitPanel topPanel = new HorizontalSplitPanel();
		topPanel.setSplitPosition(70);
		topPanel.addComponent(searchAndConveniencePanel);
		topPanel.addComponent(documentsPanel);

		addComponent(topPanel);
////		setExpandRatio(topPanel, 1.0f);
//		
		Component resultPanel = createResultPanel();
		resultPanel.setSizeFull();
//		
		addComponent(resultPanel);
		setExpandRatio(resultPanel, 1.0f);
	}

	private Component createResultPanel() {
		
		resultTabSheet = new TabSheet();
		
		Component resultByPhraseView = createResultByPhraseView();
		resultTabSheet.addTab(resultByPhraseView, "Result by phrase");
		
		Component resultByMarkupView = createResultByMarkupView();
		resultTabSheet.addTab(resultByMarkupView, "Result by markup");
		
		Component kwicResultView = createKwicResultView();
		resultTabSheet.addTab(kwicResultView, "KWIC result");
		
		return resultTabSheet;
	}

	private Component createKwicResultView() {
		// TODO Auto-generated method stub
		
		return new HorizontalLayout();
	}

	private Component createResultByMarkupView() {
		// TODO Auto-generated method stub
		return new HorizontalLayout();
	}

	private Component createResultByPhraseView() {
		// TODO Auto-generated method stub
		return new HorizontalLayout();
	}

	private Component createDocumentsPanel() {
		Panel documentsPanel = new Panel();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption("Documents considered for this search");
		
		
		documentsPanel.addComponent(documentsTree);
		return documentsPanel;
	}

	private Component createConvenienceButtonPanel() {
		HorizontalLayout convenienceButtonPanel = new HorizontalLayout();
		convenienceButtonPanel.setSpacing(true);
		
		btQueryBuilder = new Button("Query Builder");
		convenienceButtonPanel.addComponent(btQueryBuilder);
		
		btWordList = new Button("Wordlist");
		convenienceButtonPanel.addComponent(btWordList);
	
		return convenienceButtonPanel;
	}

	private Component createSearchPanel() {
		HorizontalLayout searchPanel = new HorizontalLayout();
		searchPanel.setSpacing(true);
		searchPanel.setWidth("100%");
		
		searchInput = new TextField();
		searchInput.setCaption("Query");
		searchInput.setWidth("100%");
		
		searchPanel.addComponent(searchInput);
		searchPanel.setExpandRatio(searchInput, 1.0f);
		
		btExecSearch = new Button("Execute Query");
		searchPanel.addComponent(btExecSearch);
		searchPanel.setComponentAlignment(btExecSearch, Alignment.BOTTOM_CENTER);
		
		return searchPanel;
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

}
