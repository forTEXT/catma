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

import java.io.IOException;
import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;

public class CorpusContentSelectionDialog extends VerticalLayout {
	
	private enum DocumentTreeProperty {
		caption,
		include,
		;
	}

	private String userMarkupItemDisplayString = Messages.getString("CorpusContentSelectionDialog.annotations"); //$NON-NLS-1$
	private String windowCaption = "Window Caption"; //$NON-NLS-1$
	private String documentsTreeCaption = "Documents Tree Caption"; //$NON-NLS-1$
	private Repository repository;
	private SourceDocument sourceDocument;
	private TreeTable documentsTree;
	private HierarchicalContainer documentsContainer;
	private SaveCancelListener<Corpus> listener;
	private Button btCreateMarkupCollection;
	private Button btOk;
	private Button btCancel;
	private Window dialogWindow;
	private Corpus constrainingCorpus;
	private List<String> preselectUmcIds;
	private List<UserMarkupCollectionReference> umcRefList;

	public CorpusContentSelectionDialog(
			Repository repository,
			SourceDocument sd, 
			Corpus corpus, 
			SaveCancelListener<Corpus> listener,
			String windowCaption,
			String documentsTreeCaption) {
		this(repository, sd, corpus, listener, windowCaption, documentsTreeCaption, null);
	}
	
	public CorpusContentSelectionDialog(
			Repository repository,
			SourceDocument sd,
			Corpus corpus,
			SaveCancelListener<Corpus> listener,
			String windowCaption,
			String documentsTreeCaption,
			List<String> preselectUmcIds) {

		this.repository = repository;
		this.sourceDocument = sd;
		this.constrainingCorpus = corpus;
		this.listener = listener;
		this.windowCaption = windowCaption;
		this.documentsTreeCaption = documentsTreeCaption;
		this.preselectUmcIds = preselectUmcIds;
		
		initComponents();
		initActions();
	}

	private void initActions() {
		btCancel.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				dialogWindow.close();
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
				dialogWindow.close();
				listener.savePressed(corpus);
			}
		});
		
		btCreateMarkupCollection.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				final String userMarkupCollectionNameProperty = "name"; //$NON-NLS-1$
				
				SingleValueDialog singleValueDialog = new SingleValueDialog();
				
				singleValueDialog.getSingleValue(
						Messages.getString("CorpusContentSelectionDialog.createAnnotationCollectionTitle"), //$NON-NLS-1$
						Messages.getString("CorpusContentSelectionDialog.youHaveToEnterAName"), //$NON-NLS-1$
						new SaveCancelListener<PropertysetItem>() {
							public void cancelPressed() {}
							public void savePressed(PropertysetItem propertysetItem) {
									com.vaadin.data.Property<?> property = propertysetItem.getItemProperty(userMarkupCollectionNameProperty);
									String name = (String)property.getValue();
									try {
										repository.createUserMarkupCollection(name, sourceDocument);
										populateDocumentsTree();
									} catch (IOException e) {
										((CatmaApplication)UI.getCurrent()).showAndLogError(Messages.getString("CorpusContentSelectionDialog.errorCreatingCollection"), e); //$NON-NLS-1$
									}
								}
							}, userMarkupCollectionNameProperty);
			}
		});
	}

	private void initComponents() {
		setSizeFull();
		
		VerticalLayout documentsPanelContent = new VerticalLayout();
		documentsPanelContent.setMargin(true);
		documentsPanelContent.setSpacing(true);
		documentsPanelContent.setSizeFull();
		
		Panel documentsPanel = new Panel(documentsPanelContent);
//		documentsPanel.getContent().setSizeUndefined();
//		documentsPanel.getContent().setWidth("100%");
		documentsPanel.setSizeFull();
		
		documentsContainer = new HierarchicalContainer();

		documentsTree = new TreeTable(documentsTreeCaption, documentsContainer);

		documentsTree.setWidth("100%"); //$NON-NLS-1$
		documentsTree.setHeight("100%"); //$NON-NLS-1$
		
		documentsTree.addContainerProperty(
			DocumentTreeProperty.caption, String.class, null);
		documentsTree.addContainerProperty(
				DocumentTreeProperty.include, AbstractComponent.class, null);

		documentsTree.setColumnHeader(DocumentTreeProperty.caption, Messages.getString("CorpusContentSelectionDialog.docAnnotations")); //$NON-NLS-1$

		documentsTree.setColumnHeader(DocumentTreeProperty.include, Messages.getString("CorpusContentSelectionDialog.Include")); //$NON-NLS-1$
		
		populateDocumentsTree();
		documentsPanelContent.addComponent(documentsTree);
//		documentsPanelContent.setExpandRatio(documentsTree, 1.0f);

		addComponent(documentsPanel);
		setExpandRatio(documentsPanel, 1.0f);
		
		
			
		HorizontalLayout dialogButtonPanel = new HorizontalLayout();
		dialogButtonPanel.setSpacing(true);
		dialogButtonPanel.setWidth("100%"); //$NON-NLS-1$

		btCreateMarkupCollection = new Button(Messages.getString("CorpusContentSelectionDialog.createAnnotationCollection")); //$NON-NLS-1$
		btCreateMarkupCollection.addStyleName("secondary-button");		 //$NON-NLS-1$
		dialogButtonPanel.addComponent(btCreateMarkupCollection);
		dialogButtonPanel.setComponentAlignment(btCreateMarkupCollection, Alignment.MIDDLE_LEFT);

		btOk = new Button(Messages.getString("CorpusContentSelectionDialog.Ok")); //$NON-NLS-1$
		btOk.addStyleName("primary-button"); //$NON-NLS-1$
		btOk.setClickShortcut(KeyCode.ENTER);
		btOk.focus();
		
		dialogButtonPanel.addComponent(btOk);
		dialogButtonPanel.setComponentAlignment(btOk, Alignment.MIDDLE_RIGHT);
		dialogButtonPanel.setExpandRatio(btOk, 1.0f);
		btCancel = new Button(Messages.getString("CorpusContentSelectionDialog.Cancel")); //$NON-NLS-1$
		dialogButtonPanel.addComponent(btCancel);
		dialogButtonPanel.setComponentAlignment(btCancel, Alignment.MIDDLE_RIGHT);
		
		dialogButtonPanel.addStyleName("modal-button-container"); //$NON-NLS-1$
		addComponent(dialogButtonPanel);
		
		dialogWindow = new Window(windowCaption);
		dialogWindow.setContent(this);
	}
	
	private void populateDocumentsTree() {
		documentsTree.removeAllItems();
		
		documentsTree.addItem(
			new Object[] {sourceDocument.toString(), createCheckBox(false, true)},
			sourceDocument
		);
			
		documentsTree.setCollapsed(sourceDocument, false);

		MarkupCollectionItem userMarkupItem =
				new MarkupCollectionItem(
						sourceDocument, userMarkupItemDisplayString, true);
		documentsTree.addItem(
			new Object[] {userMarkupItemDisplayString, createToggleAllUmcCheckBox()},
			userMarkupItem);
		documentsTree.setParent(userMarkupItem, sourceDocument);		
		
		umcRefList = sourceDocument.getUserMarkupCollectionRefs();
		
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
		int pageLength = umcRefList.size() + 1;
		if (pageLength < 5) {
			pageLength = 5;
		}
		if (pageLength > 15) {
			pageLength = 15;
		}
		documentsTree.setPageLength(pageLength);
	}

	private CheckBox createCheckBox(final boolean editable, boolean initialState) {
		CheckBox cb = new CheckBox();
		
		cb.setValue(initialState);
		cb.setImmediate(true);
		cb.setEnabled(editable);

		return cb;
	}
	
	private CheckBox createToggleAllUmcCheckBox() {
		final CheckBox cbIncludeAll = new CheckBox();
		
		cbIncludeAll.setValue(true);
		cbIncludeAll.setImmediate(true);
		cbIncludeAll.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				boolean selected = cbIncludeAll.getValue();
				for (UserMarkupCollectionReference umcRef : umcRefList) {
					@SuppressWarnings("rawtypes")
					Property prop = documentsTree.getItem(umcRef).getItemProperty(
							DocumentTreeProperty.include);
					CheckBox cb = (CheckBox) prop.getValue();
					cb.setValue(selected);
				}
			}
		});
		return cbIncludeAll;
	}
	
	
	public void show(String dialogWidth) {
		dialogWindow.setModal(true);
		dialogWindow.setWidth(dialogWidth);
		dialogWindow.setHeight("60%"); //$NON-NLS-1$
		UI.getCurrent().addWindow(dialogWindow);
		dialogWindow.center();
	}
	
	public void show() {
		show("50%"); //$NON-NLS-1$
	}
	
	public void setCaptions(String windowCaption, String documentsTreeCaption){
		dialogWindow.setCaption(windowCaption);
		documentsTree.setCaption(documentsTreeCaption);
	}
}
