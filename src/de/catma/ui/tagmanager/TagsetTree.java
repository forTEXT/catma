package de.catma.ui.tagmanager;

import java.text.MessageFormat;
import java.util.List;

import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.util.ColorConverter;

public class TagsetTree extends HorizontalLayout {

	private static final String COLORLABEL_HTML = 
			"<span style=\"background-color:#{0};margin-left:3px;\">" +
					"&nbsp;&nbsp;&nbsp;&nbsp;" +
			"</span>";
	
	private static enum TagTreePropertyName {
		caption,
		icon,
		color,
		;
		
		private String displayString;
		
		private TagTreePropertyName() {
			this.displayString = this.name();
		}
		
		public void setDisplayString(String displayString) {
			this.displayString = displayString;
		}
		
		@Override
		public String toString() {
			return displayString;
		}
	}
	
	private TreeTable tagTree;
	private Button btInsertTagset;
	private Button btRemoveTagset;
	private Button btEditTagset;
	private Button btInsertTag;
	private Button btRemoveTag;
	private Button btEditTag;
	private Button btInsertProperty;
	private Button btRemoveProperty;
	private Button btEditProperty;
	private boolean withTagsetButtons;

	public TagsetTree() {
		this(true);
	}
	
	public TagsetTree(boolean withTagsetButtons) {
		this.withTagsetButtons = withTagsetButtons;
	}
	
	@Override
	public void attach() {
		super.attach();
		initComponents();
	}
	
	private void initComponents() {
		setWidth("100%");
		
		tagTree = new TreeTable();
		tagTree.setSizeFull();
		tagTree.setSelectable(true);
		tagTree.setMultiSelect(false);
		
		tagTree.setContainerDataSource(new HierarchicalContainer());
		tagTree.addContainerProperty(
				TagTreePropertyName.caption, String.class, null);
		tagTree.addContainerProperty(
				TagTreePropertyName.icon, Resource.class, null);

		tagTree.setItemCaptionPropertyId(TagTreePropertyName.caption);
		tagTree.setItemIconPropertyId(TagTreePropertyName.icon);
		tagTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
	
		TagTreePropertyName.caption.setDisplayString("Tagsets");
		TagTreePropertyName.color.setDisplayString("Tag Color");
		
		tagTree.setVisibleColumns(
				new Object[] {
						TagTreePropertyName.caption});
		
		tagTree.addGeneratedColumn(
				TagTreePropertyName.color, new ColumnGenerator() {
			
			public Object generateCell(
					Table source, Object itemId, Object columnId) {
				if (itemId instanceof TagDefinition) {
					TagDefinition td = (TagDefinition)itemId;
					Label colorLabel = 
						new Label(
							MessageFormat.format(
								COLORLABEL_HTML, 
								new ColorConverter(
									Integer.valueOf(td.getColor())).toHex()));
					colorLabel.setContentMode(Label.CONTENT_XHTML);
					return colorLabel;
				}
				
				return new Label();
			}
		});
		
		addComponent(tagTree);
		setExpandRatio(tagTree, 2);
		
		GridLayout buttonGrid = new GridLayout(1, 19);
		buttonGrid.setMargin(true);
		buttonGrid.setSpacing(true);

		buttonGrid.addStyleName("taglibrary-action-grid");
		int buttonGridRowCount = 0;
		
		if (withTagsetButtons) {
			Label tagsetLabel = new Label();
			tagsetLabel.setIcon(
					new ClassResource(
							"ui/tagmanager/resources/grndiamd.gif", getApplication()));
			tagsetLabel.setCaption("Tagset");
			
			buttonGrid.addComponent(tagsetLabel);
			buttonGridRowCount++;
			
			btInsertTagset = new Button("Insert Tagset");
			btInsertTagset.setWidth("100%");
			buttonGrid.addComponent(btInsertTagset);
			buttonGridRowCount++;
			
			btRemoveTagset = new Button("Remove Tagset");
			btRemoveTagset.setWidth("100%");
			buttonGrid.addComponent(btRemoveTagset);
			buttonGridRowCount++;
			
			btEditTagset = new Button("Edit Tagset");
			btEditTagset.setWidth("100%");
			buttonGrid.addComponent(btEditTagset);
			buttonGridRowCount++;
		}
		
		Label tagLabel = new Label();
		tagLabel.setIcon(
				new ClassResource(
						"ui/tagmanager/resources/reddiamd.gif", getApplication()));
		tagLabel.setCaption("Tag");
		
		buttonGrid.addComponent(
				tagLabel, 0, buttonGridRowCount, 0, buttonGridRowCount+4 );
		buttonGridRowCount+=5;
		
		buttonGrid.setComponentAlignment(tagLabel, Alignment.BOTTOM_LEFT);
		
		btInsertTag = new Button("Insert Tag");
		btInsertTag.setWidth("100%");
		buttonGrid.addComponent(btInsertTag);
		buttonGridRowCount++;
		
		btRemoveTag = new Button("Remove Tag");
		btRemoveTag.setWidth("100%");
		buttonGrid.addComponent(btRemoveTag);
		buttonGridRowCount++;
		
		btEditTag = new Button("Edit Tag");
		btEditTag.setWidth("100%");
		buttonGrid.addComponent(btEditTag);
		buttonGridRowCount++;
		
		Label propertyLabel = new Label();
		propertyLabel.setIcon(
				new ClassResource(
						"ui/tagmanager/resources/ylwdiamd.gif", getApplication()));
		propertyLabel.setCaption("Property");
		
		
		buttonGrid.addComponent(
				propertyLabel, 0, buttonGridRowCount, 0, buttonGridRowCount+4);
		buttonGridRowCount+=5;
		
		buttonGrid.setComponentAlignment(propertyLabel, Alignment.BOTTOM_LEFT);
		
		btInsertProperty = new Button("Insert Property");
		btInsertProperty.setWidth("100%");
		buttonGrid.addComponent(btInsertProperty);
		buttonGridRowCount++;
		
		btRemoveProperty = new Button("Remove Property");
		// commented in on purpose: somehow this forces all the other buttons to 
		// show up in natural size...
//		btRemoveProperty.setWidth("100%");
		buttonGrid.addComponent(btRemoveProperty);
		buttonGridRowCount++;
		
		btEditProperty = new Button("Edit Property");
		btEditProperty.setWidth("100%");
		buttonGrid.addComponent(btEditProperty);
		buttonGridRowCount++;
		
		addComponent(buttonGrid);
		setExpandRatio(buttonGrid, 0);
	}

	public void addTagsetDefinition(List<TagsetDefinition> tagsetDefinitions) {
		for (TagsetDefinition tagsetDefinition : tagsetDefinitions) {
			addTagsetDefinition(tagsetDefinition);
		}
	}
	
	public void addTagsetDefinition(TagsetDefinition tagsetDefinition) {
		
		ClassResource tagsetIcon = 
				new ClassResource(
					"ui/tagmanager/resources/grndiamd.gif", getApplication());
		tagTree.addItem(tagsetDefinition);
		tagTree.getContainerProperty(
				tagsetDefinition, TagTreePropertyName.caption).setValue(
						tagsetDefinition.getName());
		tagTree.getContainerProperty(
				tagsetDefinition, TagTreePropertyName.icon).setValue(tagsetIcon);
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!tagDefinition.getID().equals
					(TagDefinition.CATMA_BASE_TAG.getID())) {
				
				ClassResource tagIcon = 
					new ClassResource(
						"ui/tagmanager/resources/reddiamd.gif", getApplication());

				tagTree.addItem(tagDefinition);
				tagTree.getContainerProperty(
						tagDefinition, 
						TagTreePropertyName.caption).setValue(
								tagDefinition.getType());
				tagTree.getContainerProperty(
						tagDefinition, 
						TagTreePropertyName.icon).setValue(tagIcon);
				
				for (PropertyDefinition propertyDefinition : 
						tagDefinition.getUserDefinedPropertyDefinitions()) {
					
					ClassResource propertyIcon = 
							new ClassResource(
								"ui/tagmanager/resources/ylwdiamd.gif", 
								getApplication());
					
					tagTree.addItem(propertyDefinition);
					tagTree.setParent(propertyDefinition, tagDefinition);
					tagTree.getContainerProperty(
							propertyDefinition, 
							TagTreePropertyName.caption).setValue(
									propertyDefinition.getName());
					tagTree.getContainerProperty(
							propertyDefinition, 
							TagTreePropertyName.icon).setValue(
									propertyIcon);
					tagTree.setChildrenAllowed(propertyDefinition, false);
				}
			}
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			String baseID = tagDefinition.getBaseID();
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			if ((parent==null)
					||(parent.getID().equals(
							TagDefinition.CATMA_BASE_TAG.getID()))) {
				tagTree.setParent(tagDefinition, tagsetDefinition);
			}
			else {
				tagTree.setParent(tagDefinition, parent);
			}
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!tagTree.hasChildren(tagDefinition)) {
				tagTree.setChildrenAllowed(tagDefinition, false);
			}
		}
	}
	
	public TreeTable getTagTree() {
		return tagTree;
	}
}
