package de.catma.ui.repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;


public class RepositoryView extends VerticalLayout {
	
	private static class SourceDocumentFilter implements Filter {
		
		private Set<String> corpusContent;
		
		public SourceDocumentFilter(Corpus corpus) {
			super();
			corpusContent = new HashSet<String>();
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				corpusContent.add(sd.toString());
			}

			for (UserMarkupCollectionReference ucr : corpus.getUserMarkupCollectionRefs()) {
				corpusContent.add(ucr.toString());
			}
			
			for (StructureMarkupCollectionReference scr : corpus.getStructureMarkupCollectionRefs()) {
				corpusContent.add(scr.toString());
			}
		}

		public boolean appliesToProperty(Object propertyId) {
			return true;
		}
		
		public boolean passesFilter(Object itemId, Item item)
				throws UnsupportedOperationException {
			
			return corpusContent.contains(itemId.toString());
		}
		
		
	}

	private Repository repository;
	private Tree documentsTree;
	private Tree corporaTree;
	private String allDocuments = "All documents";
	private HierarchicalContainer documentsContainer;
	private HashMap<String, SourceDocumentFilter> filters = 
			new HashMap<String, SourceDocumentFilter>();
	
	public RepositoryView(Repository repository) {
		super();
		this.repository = repository;
		initComponents();
		initActions();
	}

	private void initActions() {
		corporaTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				if (value != null) {
					documentsContainer.removeAllContainerFilters();
					
					if (!value.equals(allDocuments)) {
						Corpus corpus = (Corpus)value;
						
						if (!filters.containsKey(value)) {
							filters.put(
									corpus.toString(), new SourceDocumentFilter(corpus));
						}
						SourceDocumentFilter sdf = filters.get(corpus.toString());
						documentsContainer.addContainerFilter(sdf);
					}

				}
			}
		});
		
	}

	private void initComponents() {
		this.setMargin(true, false, false, true);
		this.setSpacing(true);
		Label documentsLabel = new Label("Document Manager");
		addComponent(documentsLabel);
		
		HorizontalLayout mainPanel = new HorizontalLayout();
		mainPanel.setSpacing(true);
		
		Panel corporaPanel = new Panel();
		corporaPanel.setHeight("200px");

		corporaTree = new Tree();
		corporaTree.setCaption("Corpora");
		corporaTree.addItem(allDocuments);
		corporaTree.setChildrenAllowed(allDocuments, false);
		corporaTree.setImmediate(true);
		
		for (Corpus c : repository.getCorpora()) {
			corporaTree.addItem(c);
			corporaTree.setChildrenAllowed(c, false);
		}
		
		corporaPanel.addComponent(corporaTree);
		mainPanel.addComponent(corporaPanel);
		mainPanel.setExpandRatio(corporaPanel, 1.0f);
		
		Panel documentsPanel = new Panel();
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption("Documents");
		
		documentsPanel.addComponent(documentsTree);
		documentsPanel.setHeight("200px");
		
		for (SourceDocument sd : repository.getSourceDocuments()) {
			documentsTree.addItem(sd);
			documentsTree.setChildrenAllowed(sd, true);
			
			for (StructureMarkupCollectionReference smcr : sd.getStructureMarkupCollectionRefs()) {
				documentsTree.addItem(smcr);
				documentsTree.setParent(smcr, sd);
			}
			
			for (UserMarkupCollectionReference ucr : sd.getUserMarkupCollectionRefs()) {
				documentsTree.addItem(ucr);
				documentsTree.setParent(ucr, sd);
			}
		}
		mainPanel.addComponent(documentsPanel);
		mainPanel.setExpandRatio(documentsPanel, 1.0f);
		
		addComponent(mainPanel);
		
		corporaTree.setValue(allDocuments);
	}

	public Repository getRepository() {
		return repository;
	}
}
