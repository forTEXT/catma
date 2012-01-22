package de.catma.ui.repository;

import java.util.HashMap;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Form;
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
import de.catma.ui.repository.entry.ContentInfo;
import de.catma.ui.repository.entry.DocumentEntry;
import de.catma.ui.repository.entry.MarkupCollectionEntry;
import de.catma.ui.repository.entry.MarkupCollectionsEntry;
import de.catma.ui.repository.entry.SourceDocumentEntry;
import de.catma.ui.repository.entry.StandardContentInfo;


public class RepositoryView extends VerticalLayout {
	
	private Repository repository;
	private Tree documentsTree;
	private Tree corporaTree;
	private Form contentInfoForm;
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
						if(documentsContainer.size() > 0) {
							documentsTree.setValue(documentsContainer.getIdByIndex(0));
						}
					}

				}
			}
		});
		
		documentsTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				Object value = event.getProperty().getValue();
				if (value != null) {
					DocumentEntry entry = (DocumentEntry)value;
					contentInfoForm.setItemDataSource(entry.getContentInfo());
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
		mainPanel.setSizeFull();
		mainPanel.setMargin(false, false, true, false);
		
		Panel corporaPanel = new Panel();
		corporaPanel.getContent().setSizeUndefined();
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
		mainPanel.setExpandRatio(corporaPanel, 1);
		
		Panel documentsPanel = new Panel();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new Tree();
		documentsTree.setContainerDataSource(documentsContainer);
		documentsTree.setCaption("Documents");
		documentsTree.setImmediate(true);
		
		documentsPanel.addComponent(documentsTree);
		documentsPanel.getContent().setSizeUndefined();
		documentsPanel.setHeight("200px");
		
		for (SourceDocument sd : repository.getSourceDocuments()) {
			DocumentEntry sourceDocEntry = new SourceDocumentEntry(sd);
			documentsTree.addItem(sourceDocEntry);
			documentsTree.setChildrenAllowed(sourceDocEntry, true);
			MarkupCollectionsEntry structureMarkupCollEntry = 
					new MarkupCollectionsEntry( 
							new MarkupCollectionsNode("Structure Markup Collections"));
			
			documentsTree.addItem(structureMarkupCollEntry);
			documentsTree.setParent(structureMarkupCollEntry, sourceDocEntry);
			
			for (StructureMarkupCollectionReference smcr : sd.getStructureMarkupCollectionRefs()) {
				DocumentEntry structureMarkupCollRefEntry = new MarkupCollectionEntry(smcr);
				documentsTree.addItem(structureMarkupCollRefEntry);
				documentsTree.setParent(structureMarkupCollRefEntry, structureMarkupCollEntry);
				documentsTree.setChildrenAllowed(structureMarkupCollRefEntry, false);
			}
			
			MarkupCollectionsEntry userMarkupCollEntry =
					new MarkupCollectionsEntry(
							new MarkupCollectionsNode("User Markup Collections"));
			documentsTree.addItem(userMarkupCollEntry);
			documentsTree.setParent(userMarkupCollEntry, sourceDocEntry);
		
			for (UserMarkupCollectionReference ucr : sd.getUserMarkupCollectionRefs()) {
				DocumentEntry userMarkupCollRefEntry = new MarkupCollectionEntry(ucr);
				documentsTree.addItem(userMarkupCollRefEntry);
				documentsTree.setParent(userMarkupCollRefEntry, userMarkupCollEntry);
				documentsTree.setChildrenAllowed(userMarkupCollRefEntry, false);

			}
		}
		mainPanel.addComponent(documentsPanel);
		mainPanel.setExpandRatio(documentsPanel, 2);

		contentInfoForm = new Form();
		contentInfoForm.setCaption("Information");
		
		BeanItem<ContentInfo> contentInfoItem = new BeanItem<ContentInfo>(new StandardContentInfo());
		contentInfoForm.setItemDataSource(contentInfoItem);
		
		mainPanel.addComponent(contentInfoForm);
		mainPanel.setExpandRatio(contentInfoForm, 1);
		
		addComponent(mainPanel);
		corporaTree.setValue(allDocuments);
	}

	public Repository getRepository() {
		return repository;
	}
}
