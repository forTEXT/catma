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

import static de.catma.ui.tagmanager.ColorLabelColumnGenerator.COLORLABEL_HTML;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.CatmaApplication;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.TagMatchMode;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.ui.EndorsedTreeTable;
import de.catma.ui.dialog.wizard.ToggleButtonStateListener;
import de.catma.util.ColorConverter;

public class TagPanel extends AbstractSearchPanel {
	
	private static enum TagTreePropertyName {
		caption,
		icon,
		color,
		colorValue
		;
	}
	
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
	
	private EndorsedTreeTable tagsetTree;
	private boolean init = false;
	private VerticalSplitPanel splitPanel;
	private ResultPanel resultPanel;
	private ComboBox tagMatchModeCombo;
	private VerticalLayout contentPanel;
	private boolean inRefinement;
	
	public TagPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, QueryOptions queryOptions) {
		super(toggleButtonStateListener, queryTree, queryOptions);
	}

	@Override
	public void attach() {
		super.attach();
		if (!init) {
			init = true;
			try {
				initComponents();
				initActions();
				initData();
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError("DB error", e);
			}
		}
	}
	
	private void initData() throws IOException {
		List<String> userMarkupCollectionIDs = null;
		
		Repository repository = queryOptions.getRepository();

		if (!queryOptions.getRelevantUserMarkupCollIDs().isEmpty()) {
			userMarkupCollectionIDs = queryOptions.getRelevantUserMarkupCollIDs();
		}
		else if (!queryOptions.getRelevantSourceDocumentIDs().isEmpty()) {
			userMarkupCollectionIDs = new ArrayList<String>();
			for (String sourceDocId : queryOptions.getRelevantSourceDocumentIDs()) {
				SourceDocument sd = repository.getSourceDocument(sourceDocId);
				for (UserMarkupCollectionReference umcRef : sd.getUserMarkupCollectionRefs()) {
					userMarkupCollectionIDs.add(umcRef.getId());
				}
			}
		}
		
		List<TagDefinitionPathInfo> tagDefinitionPathInfos = 
				queryOptions.getIndexer().getTagDefinitionPathInfos(userMarkupCollectionIDs);
		
		for (TagDefinitionPathInfo tagDefinitionPathInfo : tagDefinitionPathInfos) {
			addTagDefinitionPathInfo(tagDefinitionPathInfo);
		}
	}

	private void initActions() {
		resultPanel.addBtShowInPreviewListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				showInPreview();
			}
		});
		
		tagMatchModeCombo.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				showInPreview();
			}
		});
	}

	private void showInPreview() {
		Object value = tagsetTree.getValue();
		
		if ((value != null) && (tagsetTree.getParent(value) != null)) {
			String path = ((String)value).substring(((String)value).indexOf('/')+1);
			
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

	@SuppressWarnings("unchecked")
	private void addTagDefinitionPathInfo(
			TagDefinitionPathInfo tagDefinitionPathInfo) {
		String tagsetDefinitionName = tagDefinitionPathInfo.getTagsetDefinitionName();
		
		if (!(tagsetTree).containsId(tagsetDefinitionName)) {
			ClassResource tagsetIcon = 
					new ClassResource("ui/tagmanager/resources/grndiamd.gif");
			tagsetTree.addItem(tagsetDefinitionName);
			tagsetTree.getContainerProperty(
				tagsetDefinitionName, TagTreePropertyName.caption).setValue(
						tagsetDefinitionName);
			tagsetTree.getContainerProperty(
					tagsetDefinitionName, TagTreePropertyName.icon).setValue(
							tagsetIcon);
			tagsetTree.setChildrenAllowed(tagsetDefinitionName, true);
		}
		
		Object parent = tagsetDefinitionName;
		
		String[] tagDefinitions = 
				tagDefinitionPathInfo.getTagDefinitionPath().substring(1).split("/"); //TODO: handle escapes
		StringBuilder pathBuilder = new StringBuilder();
		for (String tagDefinitionName : tagDefinitions) {
			pathBuilder.append("/");
			pathBuilder.append(tagDefinitionName);
			String curPath = pathBuilder.toString();
				
			String curId = tagsetDefinitionName + curPath; 
			if (!tagsetTree.containsId(curId)) {
				ClassResource tagIcon = new ClassResource(
						"ui/tagmanager/resources/reddiamd.gif");
				tagsetTree.addItem(curId);
				tagsetTree.getContainerProperty(
						curId, TagTreePropertyName.caption).setValue(
							tagDefinitionName);
				tagsetTree.getContainerProperty(
						curId, TagTreePropertyName.icon).setValue(
							tagIcon);
				tagsetTree.setChildrenAllowed(parent, true);
				tagsetTree.setParent(curId, parent);
				tagsetTree.setChildrenAllowed(curId, false);
			}
			
			if (curPath.equals(tagDefinitionPathInfo.getTagDefinitionPath())) {
				Label colorLabel = 
						new Label(
							MessageFormat.format(
								COLORLABEL_HTML, 
								ColorConverter.toHex((
										tagDefinitionPathInfo.getColor()))));
				colorLabel.setContentMode(ContentMode.HTML);
				tagsetTree.getContainerProperty(
						curId, TagTreePropertyName.color).setValue(
							colorLabel);
			}
			parent = curId;
		}
	}

	private void initComponents() {
		contentPanel = new VerticalLayout();
		contentPanel.setSizeFull();
		addComponent(contentPanel);
		
		HorizontalLayout tagSearchPanel = new HorizontalLayout();
		tagSearchPanel.setSizeFull();
		tagSearchPanel.setSpacing(true);
		
		tagsetTree = new EndorsedTreeTable();
		tagsetTree.setSizeFull();
		tagsetTree.setSelectable(true);
		tagsetTree.setMultiSelect(false);
		
		tagsetTree.setContainerDataSource(new HierarchicalContainer());
		
		tagsetTree.addContainerProperty(
				TagTreePropertyName.caption, String.class, null);
		tagsetTree.setColumnHeader(TagTreePropertyName.caption, "Tagsets");
		
		tagsetTree.addContainerProperty(
				TagTreePropertyName.icon, Resource.class, null);

		tagsetTree.addContainerProperty(
				TagTreePropertyName.color,
				Component.class, null);
		
		tagsetTree.setItemCaptionPropertyId(TagTreePropertyName.caption);
		tagsetTree.setItemIconPropertyId(TagTreePropertyName.icon);
		tagsetTree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
	
		tagsetTree.setVisibleColumns(
				new Object[] {
						TagTreePropertyName.caption,
						TagTreePropertyName.color});
		

		tagsetTree.setColumnHeader(TagTreePropertyName.color, "Tag Color");
		
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
		
		resultPanel = new ResultPanel(queryOptions);
		splitPanel.addComponent(resultPanel);
		
		initSearchPanelComponents(contentPanel);
		
	}

	@Override
	public String getCaption() {
		return "Please choose a TagDefinition " +
				"(only TagDefinitions that will provide results are listed)";
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
