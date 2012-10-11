package de.catma.ui.analyzer.querybuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
	
	private Map<String, TagsetDefinition> tagsetDefinitionsByUuid;
	private Component tagLibraryPanel;
	private Tree tagLibrariesTree;
	private Button btOpenTagLibrary;
	private TagsetTree tagsetTree;
	private boolean init = false;
	private VerticalSplitPanel splitPanel;
	private ResultPanel resultPanel;
	private ComboBox tagMatchModeCombo;
	private VerticalLayout contentPanel;

	public TagPanel(
			ToggleButtonStateListener toggleButtonStateListener, 
			QueryTree queryTree, QueryOptions queryOptions) {
		super(toggleButtonStateListener, queryTree, queryOptions);
		this.tagsetDefinitionsByUuid = new HashMap<String, TagsetDefinition>();
		try {
			initTagsets();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void attach() {
		super.attach();
		if (!init) {
			init = true;
			initComponents();
			initActions();
		}
//		getParent().setHeight("100%");
	}
	
	private void initActions() {
		if (tagsetDefinitionsByUuid.isEmpty()) {
			btOpenTagLibrary.addListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					Object value = tagLibrariesTree.getValue();
					if (value != null) {
						TagLibraryReference tlr = (TagLibraryReference)value;
						try {
							TagLibrary tl = 
									queryOptions.getRepository().getTagLibrary(tlr);
							addTagLibrary(tl);
							for (TagsetDefinition t : tagsetDefinitionsByUuid.values()) {
								System.out.println(t);
							}
							tagsetTree.addTagsetDefinition(tagsetDefinitionsByUuid.values());
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
			if (tagsetDefinitionsByUuid.containsKey(tagsetDefinition.getUuid())) {
				TagsetDefinition existingTSDef = 
						tagsetDefinitionsByUuid.get(tagsetDefinition.getUuid());
				
				if (tagsetDefinition.getVersion().isNewer(existingTSDef.getVersion())) {
					tagsetDefinitionsByUuid.remove(existingTSDef.getUuid());
					tagsetDefinitionsByUuid.put(
							tagsetDefinition.getUuid(), tagsetDefinition);
				}
				
			}
			else {
				tagsetDefinitionsByUuid.put(
						tagsetDefinition.getUuid(), tagsetDefinition);
			}
		}
		
		
	}

	protected void initComponents() {
		contentPanel = new VerticalLayout();
		contentPanel.setSizeFull();
		addComponent(contentPanel);
		
		if (tagsetDefinitionsByUuid.isEmpty()) {
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
		tagMatchModeCombo.setVisible(false);
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
		if (tagsetDefinitionsByUuid.isEmpty()) {
			splitPanel.setVisible(false);
		}
		else {
			tagsetTree.addTagsetDefinition(tagsetDefinitionsByUuid.values());
		}
		
		resultPanel = new ResultPanel(queryOptions);
		splitPanel.addComponent(resultPanel);
		
		initComponents(contentPanel);
		
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
	
	public Component getContent() {
		return this;
	}

	@Override
	public String getCaption() {
		return "Please choose a TagDefinition";
	}
	
	
	@Override
	public String toString() {
		return "by Tag";
	}
	
}
