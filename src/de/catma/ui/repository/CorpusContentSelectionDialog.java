/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.repository;

import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
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
	private Corpus constrainingCorpus;

	public CorpusContentSelectionDialog(
			SourceDocument sd, Corpus corpus, String treeTitle, SaveCancelListener<Corpus> listener) {
		this(sd, corpus, treeTitle, listener, null);
	}
	
	public CorpusContentSelectionDialog(
			SourceDocument sd, Corpus corpus, String treeTitle, SaveCancelListener<Corpus> listener,
			List<String> preselectUmcIds) {
		this.sourceDocument = sd;
		this.listener = listener;
		this.constrainingCorpus = corpus;
		
		initComponents(treeTitle, preselectUmcIds);
		initActions();
	}

	private void initActions() {
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().removeWindow(dialogWindow);
				listener.cancelPressed();
				listener = null;
			}
		});
		
		btOk.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				Corpus corpus = new Corpus(sourceDocument.toString());
				corpus.addSourceDocument(sourceDocument);
				
				List<UserMarkupCollectionReference> umcRefList = sourceDocument.getUserMarkupCollectionRefs();
				
				if (constrainingCorpus != null) {
					umcRefList = constrainingCorpus.getUserMarkupCollectionRefs(sourceDocument);
				}
				
				for (UserMarkupCollectionReference umcRef : umcRefList) {
					
					Property prop = documentsTree.getItem(umcRef).getItemProperty(
							DocumentTreeProperty.include);
					CheckBox cb = (CheckBox) prop.getValue();
					if (cb.getValue()) {
						corpus.addUserMarkupCollectionReference(umcRef);
					}
				}
				UI.getCurrent().removeWindow(dialogWindow);
				listener.savePressed(corpus);
			}
		});
	}

	private void initComponents(String treeTitle, List<String> preselectUmcIds) {
		setSizeFull();
		VerticalLayout documentsPanelContent = new VerticalLayout();
		documentsPanelContent.setMargin(true);
		
		Panel documentsPanel = new Panel(documentsPanelContent);
		documentsPanel.getContent().setSizeUndefined();
		documentsPanel.getContent().setWidth("100%");
		documentsPanel.setSizeFull();
		
		documentsContainer = new HierarchicalContainer();
		documentsTree = new TreeTable(
				treeTitle, documentsContainer);
		documentsTree.setWidth("100%");
		
		documentsTree.addContainerProperty(
			DocumentTreeProperty.caption, String.class, null);
		documentsTree.addContainerProperty(
				DocumentTreeProperty.include, AbstractComponent.class, null);
		documentsTree.setColumnHeader(DocumentTreeProperty.caption, "Document/Collection");
		documentsTree.setColumnHeader(DocumentTreeProperty.include, "Include");
		
		documentsTree.addItem(
			new Object[] {sourceDocument.toString(), createCheckBox(false, true)},
			sourceDocument);
		
		documentsTree.setCollapsed(sourceDocument, false);

		MarkupCollectionItem userMarkupItem =
				new MarkupCollectionItem(
						sourceDocument, userMarkupItemDisplayString, true);
		documentsTree.addItem(
			new Object[] {userMarkupItemDisplayString, new Label()},
			userMarkupItem);
		documentsTree.setParent(userMarkupItem, sourceDocument);
		
		List<UserMarkupCollectionReference> umcRefList = sourceDocument.getUserMarkupCollectionRefs();
		
		if (constrainingCorpus != null) {
			umcRefList = constrainingCorpus.getUserMarkupCollectionRefs(sourceDocument);
		}

		for (UserMarkupCollectionReference umcRef : umcRefList) {
			documentsTree.addItem(
				new Object[] {
					umcRef.getName(), 
					createCheckBox(
						true,
						(preselectUmcIds==null)||preselectUmcIds.contains(umcRef.getId()))
				}, 
				umcRef);
			documentsTree.setParent(umcRef, userMarkupItem);
			documentsTree.setChildrenAllowed(umcRef, false);
		}
		documentsTree.setCollapsed(userMarkupItem, false);
		int pageLength = sourceDocument.getUserMarkupCollectionRefs().size() + 1;
		if (pageLength < 5) {
			pageLength = 5;
		}
		if (pageLength > 15) {
			pageLength = 15;
		}
		documentsTree.setPageLength(pageLength);
		documentsPanelContent.addComponent(documentsTree);
		
		addComponent(documentsPanel);
		setExpandRatio(documentsPanel, 1.0f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		btOk = new Button("Ok");
		btOk.setClickShortcut(KeyCode.ENTER);
		btOk.focus();
		
		buttonPanel.addComponent(btOk);
		buttonPanel.setComponentAlignment(btOk, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(btOk, 1.0f);
		btCancel = new Button("Cancel");
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		addComponent(buttonPanel);
		
		dialogWindow = new Window("Selection of relevant documents");
		dialogWindow.setContent(this);
	}

	private CheckBox createCheckBox(final boolean editable, boolean initialState) {
		CheckBox cb = new CheckBox();
		
		cb.setValue(initialState);
		cb.setImmediate(true);

		cb.addValidator(new Validator() {
			
			public void validate(Object value) throws InvalidValueException {
				if (!editable && !(Boolean)value) {
					throw new InvalidValueException(
							"Source Document has to be included!");
				}
			}
		});

		cb.setInvalidAllowed(false);
		
		return cb;
	}
	
	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		dialogWindow.setHeight("80%");
		UI.getCurrent().addWindow(dialogWindow);
		dialogWindow.center();
	}
	
	public void show() {
		show("45%");
	}
}
