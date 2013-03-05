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
package de.catma.ui.analyzer.querybuilder;

import java.io.IOException;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.TagMatchMode;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.ui.tagmanager.TagsetTree;
import de.catma.util.ContentInfoSet;

public class TagPanel extends AbstractSearchPanel {
	
	private static class TagMatchModeItem {
		
		private String displayText;
		private TagMatchMode tagMatchMode;
		
		public TagMatchModeItem(String displayText, TagMatchMode tagMatchMode) {
			this.displayText = displayText;
			this.tagMatchMode = tagMatchMode;
		}
		
		public TagMatchMode getTagMatchMode() {
			return tagMatchMode;
		}
		
		@Override
		public String toString() {
			return displayText;
		}
	}
	
	private Component tagLibraryPanel;
	private Tree tagLibrariesTree;
	private Button btOpenTagLibrary;
	private TagsetTree tagsetTree;
	private boolean init = false;
	private VerticalSplitPanel splitPanel;
	private ResultPanel resultPanel;
	private ComboBox tagMatchModeCombo;
	private VerticalLayout contentPanel;
	private boolean inRefinement;
	
	public TagPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, QueryOptions queryOptions, 
			TagsetDefinitionDictionary tagsetDefinitionDictionary) {
		super(toggleButtonStateListener, queryTree, queryOptions, 
				tagsetDefinitionDictionary);
	}

	@Override
	public void attach() {
		super.attach();
		if (!init) {
			init = true;
			if (this.tagsetDefinitionDictionary.isEmpty()) {
				try {
					initTagsets();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			initComponents();
			initActions();
		}
	}
	
	private void initActions() {
		if (tagsetDefinitionDictionary.isEmpty()) {
			btOpenTagLibrary.addListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					Object value = tagLibrariesTree.getValue();
					if (value != null) {
						tagsetDefinitionDictionary.clear();
						
						TagLibraryReference tlr = (TagLibraryReference)value;
						try {
							TagLibrary tl = 
									queryOptions.getRepository().getTagLibrary(tlr);
							addTagLibrary(tl);
							tagsetTree.addTagsetDefinition(
									tagsetDefinitionDictionary.values());
							splitPanel.setVisible(true);
							tagLibraryPanel.setVisible(false);
							contentPanel.removeComponent(tagLibraryPanel);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
			});
		}
		tagsetTree.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				showInPreview();
			}
		});
		
		resultPanel.addBtShowInPreviewListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showInPreview();
			}
		});
		
		tagMatchModeCombo.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				showInPreview();
			}
		});
	}

	private void showInPreview() {
		Object value = tagsetTree.getTagTree().getValue();
		
		if ((value != null) && (value instanceof TagDefinition)) {
			TagDefinition td = (TagDefinition)value;
			String path = tagsetTree.getTagsetDefinition(td).getTagPath(td);
			if (curQuery != null) {
				queryTree.removeLast();
			}
			curQuery = "tag=\""+path+"%\"";
			
			if (inRefinement) {
				curQuery += 
					" " + ((TagMatchModeItem)tagMatchModeCombo.getValue()).getTagMatchMode().name().toLowerCase();
			}
			resultPanel.setQuery(curQuery);
			
			queryTree.add(curQuery);
			onFinish = !isComplexQuery();
			onAdvance = true;
		}
		else {
			onFinish = false;
			onAdvance = false;
		}
		toggleButtonStateListener.stepChanged(TagPanel.this);
	}

	private void initTagsets() throws IOException {
		Repository repository = queryOptions.getRepository();

		if (!queryOptions.getRelevantUserMarkupCollIDs().isEmpty()) {
			for (String userMarkupCollId : 
				queryOptions.getRelevantUserMarkupCollIDs()) {
				addUserMarkupCollection(
					repository, 
					new UserMarkupCollectionReference(
							userMarkupCollId, new ContentInfoSet()));
			}
		}
		else if (!queryOptions.getRelevantSourceDocumentIDs().isEmpty()) {
			for (String sourceDocId : queryOptions.getRelevantSourceDocumentIDs()) {
				SourceDocument sd = repository.getSourceDocument(sourceDocId);
				for (UserMarkupCollectionReference umcRef : sd.getUserMarkupCollectionRefs()) {
					addUserMarkupCollection(repository, umcRef);
				}
			}
		}
	}

	private void addUserMarkupCollection(Repository repository,
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		
		UserMarkupCollection umc =
				repository.getUserMarkupCollection(userMarkupCollectionReference);
		addTagLibrary(umc.getTagLibrary());
		
	}

	private void addTagLibrary(TagLibrary tagLibrary) {
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			addTagsetDefinition(tagsetDefinition);
		}
	}

	private void addTagsetDefinition(TagsetDefinition tagsetDefinition) {
		if (tagsetDefinitionDictionary.containsKey(tagsetDefinition.getUuid())) {
			TagsetDefinition existingTSDef = 
					tagsetDefinitionDictionary.get(tagsetDefinition.getUuid());
			
			if (tagsetDefinition.getVersion().isNewer(existingTSDef.getVersion())) {
				tagsetDefinitionDictionary.remove(existingTSDef.getUuid());
				tagsetDefinitionDictionary.put(
						tagsetDefinition.getUuid(), tagsetDefinition);
			}
			
		}
		else {
			tagsetDefinitionDictionary.put(
					tagsetDefinition.getUuid(), tagsetDefinition);
		}
	}

	private void initComponents() {
		contentPanel = new VerticalLayout();
		contentPanel.setSizeFull();
		addComponent(contentPanel);
		
		if (tagsetDefinitionDictionary.isEmpty()) {
			tagLibraryPanel = createTagLibraryPanel();
			contentPanel.addComponent(tagLibraryPanel);
		}
		
		HorizontalLayout tagSearchPanel = new HorizontalLayout();
		tagSearchPanel.setSizeFull();
		tagSearchPanel.setSpacing(true);
		
		tagsetTree = new TagsetTree(
			queryOptions.getRepository().getTagManager(), 
			null, false, false, null);
		tagSearchPanel.addComponent(tagsetTree);
		tagSearchPanel.setExpandRatio(tagsetTree, 0.8f);
		
		
		tagMatchModeCombo = new ComboBox("Please choose what you consider a match:");
		tagMatchModeCombo.setImmediate(true);
		TagMatchModeItem exactMatchItem = 
				new TagMatchModeItem("exact match", TagMatchMode.EXACT);
		tagMatchModeCombo.addItem(exactMatchItem);
		tagMatchModeCombo.addItem(
				new TagMatchModeItem("boundary match", 
						TagMatchMode.BOUNDARY));
		tagMatchModeCombo.addItem(
				new TagMatchModeItem("overlap match", 
						TagMatchMode.OVERLAP));
		tagMatchModeCombo.setNullSelectionAllowed(false);
		tagMatchModeCombo.setNewItemsAllowed(false);
		
		tagMatchModeCombo.setDescription(
			"The three different match modes influence the way tags refine" +
			" your search results:" +
			"<ul>"+
			"<li>exact match - the tag boundaries have to match exactly to " +
			"keep a result item in the result set</li>" +
			"<li>boundary match - result items that should be kept in the " +
			"result set must start and end within the boundaries of the tag</li>"+
			"<li>overlap - the result items that should be kept in the result " +
			"set must overlap with the range of the tag</li>" +
			"</ul>");
		tagMatchModeCombo.setValue(exactMatchItem);
		
		tagSearchPanel.addComponent(tagMatchModeCombo);
		tagSearchPanel.setExpandRatio(tagMatchModeCombo, 0.2f);
		
		splitPanel = new VerticalSplitPanel();
		contentPanel.addComponent(splitPanel);
		
		splitPanel.addComponent(tagSearchPanel);
		if (tagsetDefinitionDictionary.isEmpty()) {
			splitPanel.setVisible(false);
		}
		else {
			tagsetTree.addTagsetDefinition(tagsetDefinitionDictionary.values());
		}
		
		resultPanel = new ResultPanel(queryOptions);
		splitPanel.addComponent(resultPanel);
		
		initSearchPanelComponents(contentPanel);
		
	}

	private Component createTagLibraryPanel() {
		VerticalLayout tagLibraryPanel = new VerticalLayout();
		tagLibraryPanel.setMargin(true, false, false, false);
		tagLibraryPanel.setSpacing(true);
		Label infoLabel = 
			new Label(
				"Since you did not specify any Source Documents " +
				"or User Markup Collections to constrain your search, " +
				"CATMA has nowhere to look for Tags. So please open a Tag Library first: ");
		tagLibraryPanel.addComponent(infoLabel);
		Component tagLibraryTreePanel = createTagLibraryTreePanel();
		tagLibraryPanel.addComponent(tagLibraryTreePanel);
		btOpenTagLibrary = new Button("Open Tag Library");
		tagLibraryPanel.addComponent(btOpenTagLibrary);
		return tagLibraryPanel;
	}

	private Component createTagLibraryTreePanel() {

		Panel tagLibraryPanel = new Panel();
		tagLibraryPanel.getContent().setSizeUndefined();
		tagLibraryPanel.setSizeFull();
		
		tagLibrariesTree = new Tree();
		tagLibrariesTree.setCaption("Tag Libraries");
		tagLibrariesTree.addStyleName("bold-label-caption");
		tagLibrariesTree.setImmediate(true);
		tagLibrariesTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_ID);
		
		for (TagLibraryReference tlr :
				queryOptions.getRepository().getTagLibraryReferences()) {
			addTagLibraryReferenceToTree(tlr);
		}
		
		tagLibraryPanel.addComponent(tagLibrariesTree);
		
		return tagLibraryPanel;
	}
	
	private void addTagLibraryReferenceToTree(TagLibraryReference tlr) {
		tagLibrariesTree.addItem(tlr);
		tagLibrariesTree.setChildrenAllowed(tlr, false);
	}
	
	@Override
	public String getCaption() {
		return "Please choose a TagDefinition";
	}
	
	
	@Override
	public String toString() {
		return "by Tag";
	}

	@Override
	public void stepActivated(boolean forward) {
		super.stepActivated(forward);
		
		if (forward) {
			String last = queryTree.getLast();
			inRefinement =
				(last != null) 
					&& (last.trim().equals(
						ComplexTypeSelectionPanel.ComplexTypeOption.REFINMENT.getQueryElement())); 
			this.tagMatchModeCombo.setEnabled(inRefinement);
				
		}
	}
}
