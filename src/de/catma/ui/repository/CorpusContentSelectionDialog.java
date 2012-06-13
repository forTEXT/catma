package de.catma.ui.repository;

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.ui.dialog.SaveCancelListener;

public class CorpusContentSelectionDialog extends VerticalLayout {
	
	private enum DocumentTreeProperty {
		caption,
		include,
		;
	}

	private String userMarkupItemDisplayString = "User Markup Collections";
	private String staticMarkupItemDisplayString = "Static Markup Collections";
	private SourceDocument sourceDocument;
	private TreeTable documentsTree;
	private HierarchicalContainer documentsContainer;
	private SaveCancelListener<Corpus> listener;
	private Button btOk;
	private Button btCancel;
	private Window dialogWindow;

	public CorpusContentSelectionDialog(
			SourceDocument sd, SaveCancelListener<Corpus> listener) {
		this.sourceDocument = sd;
		this.listener = listener;
		
		initComponents();
		initActions();
	}

	private void initActions() {
		btCancel.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				dialogWindow.getParent().removeWindow(dialogWindow);
				listener.cancelPressed();
				listener = null;
			}
		});
		
		btOk.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Corpus corpus = new Corpus(sourceDocument.toString());
				corpus.addSourceDocument(sourceDocument);
				for (UserMarkupCollectionReference umcRef : 
					sourceDocument.getUserMarkupCollectionRefs()) {
					Property prop = documentsTree.getItem(umcRef).getItemProperty(
							DocumentTreeProperty.include);
					CheckBox cb = (CheckBox) prop.getValue();
					if (cb.booleanValue()) {
						corpus.addUserMarkupCollectionReference(umcRef);
					}
				}
				dialogWindow.getParent().removeWindow(dialogWindow);
				listener.savePressed(corpus);
			}
		});
	}

	private void initComponents() {
		setSizeFull();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new TreeTable(
				"Documents for the analysis", documentsContainer);
		documentsTree.setWidth("100%");
		
		documentsTree.addContainerProperty(
			DocumentTreeProperty.caption, String.class, null);
		documentsTree.addContainerProperty(
				DocumentTreeProperty.include, AbstractComponent.class, null);
		
		documentsTree.addItem(
			new Object[] {sourceDocument.toString(), createCheckBox(false)},
			sourceDocument);
		
		documentsTree.setCollapsed(sourceDocument, false);

		MarkupCollectionItem userMarkupItem =
				new MarkupCollectionItem(userMarkupItemDisplayString, true);
		documentsTree.setParent(userMarkupItem, sourceDocument);
		documentsTree.addItem(
			new Object[] {userMarkupItemDisplayString, new Label()},
			userMarkupItem);
		
		for (UserMarkupCollectionReference umcRef : 
			sourceDocument.getUserMarkupCollectionRefs()) {
			documentsTree.addItem(
				new Object[] {umcRef.getName(), createCheckBox(true)}, umcRef);
			documentsTree.setParent(umcRef, userMarkupItem);
			documentsTree.setChildrenAllowed(umcRef, false);
		}
		documentsTree.setCollapsed(userMarkupItem, false);
		
		addComponent(documentsTree);
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		btOk = new Button("Ok");
		buttonPanel.addComponent(btOk);
		buttonPanel.setComponentAlignment(btOk, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(btOk, 1.0f);
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		addComponent(buttonPanel);
		
		dialogWindow = new Window();
		dialogWindow.setContent(this);
	}

	private CheckBox createCheckBox(final boolean editable) {
		CheckBox cb = new CheckBox();
		
		cb.setValue(true);
		cb.setEnabled(editable);
		//TODO: try validation
//		cb.addValidator(new Validator() {
//
//			public boolean isValid(Object value) {
//				return editable || ((Boolean)value); 
//			}
//			
//			public void validate(Object value) throws InvalidValueException {
//				if (!editable && !(Boolean)value) {
//					throw new InvalidValueException("Source Document must be selected!");
//				}
//			}
//		});
//		cb.setValidationVisible(true);
		return cb;
	}
	
	public void show(Window parent, String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		parent.addWindow(dialogWindow);
	}
	
	public void show(Window parent) {
		show(parent, "25%");
	}
}
