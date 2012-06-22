package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.PropertyCollection;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.StringProperty;
import de.catma.ui.dialog.TagDefinitionFieldFactory;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TagsetTree extends HorizontalLayout {
	
	private static enum TagTreePropertyName {
		caption,
		icon,
		color,
		;
	}

	private boolean init = true;
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
	private ColorButtonListener colorButtonListener;
	private TagManager tagManager;
	private TagLibrary tagLibrary;
	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private boolean withButtonPanel;

	public TagsetTree(TagManager tagManager, TagLibrary tagLibrary) {
		this(tagManager, tagLibrary, true, null);
	}
	
	public TagsetTree(
			TagManager tagManager, final TagLibrary tagLibrary, 
			boolean withTagsetButtons, 
			ColorButtonListener colorButtonListener) {
		this(tagManager, tagLibrary, withTagsetButtons, true, colorButtonListener);
	}
	
	public TagsetTree(
			TagManager tagManager, final TagLibrary tagLibrary, 
			boolean withTagsetButtons, 
			boolean withButtonPanel,
			ColorButtonListener colorButtonListener) {
		this.tagManager = tagManager;
		this.tagLibrary = tagLibrary;
		if (withTagsetButtons) {
			tagManager.addTagLibrary(tagLibrary);
		}
		this.withTagsetButtons = withTagsetButtons;
		this.withButtonPanel = withButtonPanel;
		this.colorButtonListener = colorButtonListener;
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init){
			initComponents();
			initActions();
			init = false;
		}
	}
	
	private void initActions() {
		if (withTagsetButtons) {

			tagsetDefinitionChangedListener = 
					new PropertyChangeListener() {
				
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getOldValue()==null) {
						@SuppressWarnings("unchecked")
						Pair<TagLibrary, TagsetDefinition> addOperationResult = 
							(Pair<TagLibrary,TagsetDefinition>)evt.getNewValue();
						
						if (tagLibrary.equals(addOperationResult.getFirst())) {
							addTagsetDefinition(addOperationResult.getSecond());
						}
					}
					else if (evt.getNewValue() == null) {
						@SuppressWarnings("unchecked")
						Pair<TagLibrary, TagsetDefinition> removeOperationResult = 
							(Pair<TagLibrary,TagsetDefinition>)evt.getOldValue();
						
						if (tagLibrary.equals(removeOperationResult.getFirst())) {
							TagsetDefinition tagsetDef = 
									removeOperationResult.getSecond();
							removeTagsetDefinitionFromTree(tagsetDef);
						}
					}
					else {
						TagsetDefinition tagsetDefinition = 
								(TagsetDefinition)evt.getNewValue();
						if (tagTree.containsId(tagsetDefinition)) {
							tagTree.getContainerProperty(
								tagsetDefinition, TagTreePropertyName.caption).setValue(
										tagsetDefinition.getName());
						}
					}
				}
			};
			
			this.tagManager.addPropertyChangeListener(
					TagManagerEvent.tagsetDefinitionChanged,
					tagsetDefinitionChangedListener);
			
			this.btInsertTagset.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleInsertTagsetDefinitionRequest();
				}
			});
			
			this.btEditTagset.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleEditTagsetDefinitionRequest();
				}
			});

			btRemoveTagset.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleRemoveTagsetDefinitionRequest();
				}
			});
		}
		
		tagDefinitionChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();
				if ((oldValue == null) && (newValue == null)) {
					return;
				}
				
				if (oldValue == null) {
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> addOperationResult =
							(Pair<TagsetDefinition, TagDefinition>)evt.getNewValue();
					TagsetDefinition tagsetDefinition = 
							addOperationResult.getFirst();
					TagDefinition tagDefinition = 
							addOperationResult.getSecond();
					if (tagTree.containsId(tagsetDefinition)) {
						addTagDefinition(tagDefinition);
						establishHierarchy(tagsetDefinition, tagDefinition);
						configureChildren(tagDefinition);
					}
				}
				else if (newValue == null) {
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> removeOperationResult = 
						(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
					TagDefinition td = removeOperationResult.getSecond();
					if (tagTree.containsId(td)) {
						removeTagDefinitionFromTree(td, removeOperationResult.getFirst());
					}
				}
				else {
					TagDefinition tagDefinition = (TagDefinition)evt.getNewValue();
					Property prop = tagTree.getContainerProperty(
							tagDefinition, 
							TagTreePropertyName.caption);
					if (prop != null) {
						prop.setValue(tagDefinition.getName());
					}
				}
			}
		};
		
		this.tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);
		
		btInsertTag.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleInsertTagDefinitionRequest();
			}
		});
		
		btRemoveTag.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleRemoveTagDefinitionRequest();
			}

		});

		btEditTag.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleEditTagDefinitionRequest();
			}
		});
		
		tagTree.addListener(
				new ButtonStateManager(
						withTagsetButtons,
						btRemoveTagset, btEditTagset, 
						btInsertTag, btRemoveTag, btEditTag, 
						btInsertProperty, btRemoveProperty, btEditProperty));
	}
	
	private void handleEditTagDefinitionRequest() {
		Object selValue = tagTree.getValue();
		
		if ((selValue != null) 
			&& (selValue instanceof TagDefinition)) {
			final TagDefinition selTagDefinition = (TagDefinition)selValue;
			final String tagDefNameProp = "name";
			final String tagDefColorProp = "color";
			
			PropertyCollection propertyCollection = 
					new PropertyCollection(tagDefNameProp, tagDefColorProp);
			
			propertyCollection.getItemProperty(tagDefNameProp).setValue(
					selTagDefinition.getName());
			propertyCollection.getItemProperty(tagDefColorProp).setValue(
					ColorConverter.toHex(selTagDefinition.getColor()));
			
			FormDialog tagFormDialog = new FormDialog(
				"Edit Tag",
				propertyCollection,
				new TagDefinitionFieldFactory(tagDefColorProp),
				new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						
						Property nameProperty =
							propertysetItem.getItemProperty(
									tagDefNameProp);
						
						Property colorProperty =
							propertysetItem.getItemProperty(
									tagDefColorProp);

						tagManager.setTagDefinitionTypeAndColor(
								selTagDefinition, 
								(String)nameProperty.getValue(),
								ColorConverter.toRGBIntAsString(
										(String)colorProperty.getValue()));
					}
				});
			tagFormDialog.show(getApplication().getMainWindow(), "50%");
		}
		
	}

	private void removeTagDefinitionFromTree(
			TagDefinition td, TagsetDefinition tagsetDefinition) {
		
		for (TagDefinition child : tagsetDefinition.getChildren(td)) {
			removeTagDefinitionFromTree(child, tagsetDefinition);
		}
		
		for (PropertyDefinition pd : 
			td.getSystemPropertyDefinitions()) {
			tagTree.removeItem(pd);
		}
		for (PropertyDefinition pd :
			td.getUserDefinedPropertyDefinitions()) {
			tagTree.removeItem(pd);
		}
		Object parentId = tagTree.getParent(td);
		tagTree.removeItem(td);
		if ((parentId != null) && (!tagTree.hasChildren(parentId))) {
			tagTree.setChildrenAllowed(parentId, false);
		}
	}

	private void handleRemoveTagDefinitionRequest() {
		Object selValue = tagTree.getValue();
		
		if ((selValue != null)
				&& (selValue instanceof TagDefinition)) {
			final TagDefinition td = (TagDefinition)selValue;
			
			ConfirmDialog.show(
				getApplication().getMainWindow(),
				"Remove Tag", 
				"Do you really want to delete this Tag " +
				"with all its properties?", "Yes", "No", 
				new ConfirmDialog.Listener() {
					
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {
							tagManager.removeTagDefinition(
									getTagsetDefinition(td), td);
						}
					}
				});
		}
	}
	
	private void handleInsertTagDefinitionRequest() {
		final String tagDefNameProp = "name";
		final String tagDefColorProp = "color";
		
		PropertyCollection propertyCollection = 
				new PropertyCollection(tagDefNameProp, tagDefColorProp);
		propertyCollection.getItemProperty(tagDefColorProp).setValue(
				ColorConverter.randomHex());

		final Object selectedParent = 
				tagTree.getValue();
		
		if (selectedParent == null) {
			return;
		}
		
		FormDialog tagFormDialog =
			new FormDialog(
				"Create new Tag",
				propertyCollection,
				new TagDefinitionFieldFactory(
					tagDefColorProp),
				new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						
						Property nameProperty =
							propertysetItem.getItemProperty(
									tagDefNameProp);
						
						Property colorProperty =
							propertysetItem.getItemProperty(
									tagDefColorProp);
						
						String baseID = null;
						TagsetDefinition tagsetDefinition = null;

						if (selectedParent instanceof TagsetDefinition) {
							baseID = "";
							tagsetDefinition = 
									(TagsetDefinition)selectedParent;
						}
						else if (selectedParent instanceof TagDefinition) {
							baseID = 
								((TagDefinition)selectedParent).getUuid();
							tagsetDefinition = 
									getTagsetDefinition(
										(TagDefinition)selectedParent);
						}
						else {
							throw new IllegalStateException(
								"a parent of a TagDefinition has to be either a"
								+ "TagDefinition or a TagsetDefinition and not a " 
								+ selectedParent.getClass().getName());
						}
						IDGenerator idGenerator = new IDGenerator();
						TagDefinition tagDefinition = 
								new TagDefinition(
									null,
									idGenerator.generate(),
									(String)nameProperty.getValue(),
									new Version(), 
									(baseID.isEmpty()? null : 
										((TagDefinition)selectedParent).getId()),
									baseID);
						PropertyDefinition colorPropertyDef =
								new PropertyDefinition(
									null,
									idGenerator.generate(),
									PropertyDefinition.SystemPropertyName.
										catma_displaycolor.name(),
									new PropertyPossibleValueList(
										ColorConverter.toRGBIntAsString(
											(String)colorProperty.getValue())));
						tagDefinition.addSystemPropertyDefinition(
								colorPropertyDef);
						tagManager.addTagDefintion(
								tagsetDefinition, 
								tagDefinition);
					}
				});
		tagFormDialog.show(getApplication().getMainWindow(), "50%");
	}

	public TagsetDefinition getTagsetDefinition(TagDefinition tagDefinition) {
		Object parent = tagTree.getParent(tagDefinition);
		if (parent instanceof TagsetDefinition) {
			return (TagsetDefinition)parent;
		}
		else {
			return getTagsetDefinition((TagDefinition)parent);
		}
	}

	private void handleRemoveTagsetDefinitionRequest() {
		Object selValue = tagTree.getValue();
		
		if ((selValue != null)
				&& (selValue instanceof TagsetDefinition)) {
			final TagsetDefinition td = (TagsetDefinition)selValue;
			
			ConfirmDialog.show(
				getApplication().getMainWindow(),
				"Remove Tagset", 
				"Do you really want to delete this Tagset " +
				"with all its Tags?", "Yes", "No", 
				new ConfirmDialog.Listener() {
					
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {
							tagManager.removeTagsetDefinition(
									tagLibrary, td);
						}
					}
				});
		}
	}

	private void handleInsertTagsetDefinitionRequest() {
		final String tagsetdefinitionnameProperty = "name";
		
		PropertyCollection propertyCollection = 
				new PropertyCollection(tagsetdefinitionnameProperty);

		FormDialog tagsetFormDialog =
			new FormDialog(
				"Create new Tagset",
				propertyCollection,
				new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						Property property = 
								propertysetItem.getItemProperty(
										tagsetdefinitionnameProperty);
						TagsetDefinition td = 
								new TagsetDefinition(
									null,
									new IDGenerator().generate(), 
									(String)property.getValue(), 
									new Version());
						
						tagManager.addTagsetDefinition(
								tagLibrary, td);
					}
				});
		configureTagsetFormDialog(
				tagsetFormDialog, tagsetdefinitionnameProperty);
		tagsetFormDialog.show(getApplication().getMainWindow());
	}
	
	private void handleEditTagsetDefinitionRequest() {
		final String tagsetdefinitionnameProperty = "name";
		
		Object selValue = tagTree.getValue();
		
		if ((selValue != null)
				&& (selValue instanceof TagsetDefinition)) {
			
			final TagsetDefinition curSelTagsetDefinition =
					(TagsetDefinition)selValue;
			
			PropertyCollection propertyCollection = 
					new PropertyCollection();
			propertyCollection.addItemProperty(
							tagsetdefinitionnameProperty,
							new StringProperty(
								curSelTagsetDefinition.getName()));
			
			FormDialog tagsetFormDialog =
				new FormDialog(
					"Edit Tagset",
					propertyCollection,
					new SaveCancelListener<PropertysetItem>() {
						public void cancelPressed() {}
						public void savePressed(
								PropertysetItem propertysetItem) {
							Property property = 
									propertysetItem.getItemProperty(
										tagsetdefinitionnameProperty);
							
							tagManager.setTagsetDefinitionName(
									curSelTagsetDefinition,
									(String)property.getValue());
						}
					});
			configureTagsetFormDialog(
					tagsetFormDialog, tagsetdefinitionnameProperty);

			tagsetFormDialog.show(getApplication().getMainWindow());
		}
	}

	private void configureTagsetFormDialog(
			FormDialog formDialog, String propertyId) {
		formDialog.getField(
				propertyId).setRequired(true);
		formDialog.getField(
				propertyId).setRequiredError(
						"You have to enter a name!");
	}

	private void initComponents() {
		setWidth("100%");
	
		WebApplicationContext context = 
				((WebApplicationContext) getApplication().getContext());
		WebBrowser wb = context.getBrowser();
		
		setHeight(wb.getScreenHeight()*0.42f, UNITS_PIXELS);
		
		tagTree = new TreeTable();
		tagTree.setImmediate(true);
		tagTree.setSizeFull();
		tagTree.setSelectable(true);
		tagTree.setMultiSelect(false);
		
		tagTree.setContainerDataSource(new HierarchicalContainer());
		tagTree.addContainerProperty(
				TagTreePropertyName.caption, String.class, null);
		tagTree.setColumnHeader(TagTreePropertyName.caption, "Tagsets");
		
		tagTree.addContainerProperty(
				TagTreePropertyName.icon, Resource.class, null);

		tagTree.setItemCaptionPropertyId(TagTreePropertyName.caption);
		tagTree.setItemIconPropertyId(TagTreePropertyName.icon);
		tagTree.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
	
		tagTree.setVisibleColumns(
				new Object[] {
						TagTreePropertyName.caption});
		
		if (colorButtonListener != null) {
			tagTree.addGeneratedColumn(
				TagTreePropertyName.color,
				new ColorButtonColumnGenerator(colorButtonListener));
			
		}
		else {
			tagTree.addGeneratedColumn(
					TagTreePropertyName.color, new ColorLabelColumnGenerator());
		}
		tagTree.setColumnHeader(TagTreePropertyName.color, "Tag Color");
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
							"ui/tagmanager/resources/grndiamd.gif", 
							getApplication()));
			tagsetLabel.setCaption("Tagset");
			
			buttonGrid.addComponent(tagsetLabel);
			buttonGridRowCount++;
			
			btInsertTagset = new Button("Insert Tagset");
			btInsertTagset.setEnabled(true);
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
						"ui/tagmanager/resources/reddiamd.gif", 
						getApplication()));
		tagLabel.setCaption("Tag");
		
		buttonGrid.addComponent(
				tagLabel, 0, buttonGridRowCount, 0, buttonGridRowCount+4 );
		buttonGridRowCount+=5;
		
		buttonGrid.setComponentAlignment(tagLabel, Alignment.BOTTOM_LEFT);
		
		btInsertTag = new Button("Insert Tag");
		btInsertTag.setWidth("100%");
		if (withTagsetButtons) {
			btInsertTag.setEnabled(true);
		}
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
						"ui/tagmanager/resources/ylwdiamd.gif", 
						getApplication()));
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
		// commented out on purpose: somehow this forces all the other buttons to 
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
		
		if (!withButtonPanel) {
			buttonGrid.setVisible(false);
		}
	}

	public void addTagsetDefinition(Collection<TagsetDefinition> tagsetDefinitions) {
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
			addTagDefinition(tagDefinition);
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			establishHierarchy(tagsetDefinition, tagDefinition);
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			configureChildren(tagDefinition);
		}
	}
	

	
	private void configureChildren(TagDefinition tagDefinition) {
		if (!tagTree.hasChildren(tagDefinition)) {
			tagTree.setChildrenAllowed(tagDefinition, false);
		}
	}

	private void establishHierarchy(
			TagsetDefinition tagsetDefinition, TagDefinition tagDefinition) {
		String baseID = tagDefinition.getParentUuid();
		if (baseID.isEmpty()) {
			tagTree.setChildrenAllowed(tagsetDefinition, true);
			tagTree.setParent(tagDefinition, tagsetDefinition);
		}
		else {
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			tagTree.setChildrenAllowed(parent, true);
			tagTree.setParent(tagDefinition, parent);
		}		
	}

	private void addTagDefinition(TagDefinition tagDefinition) {
		ClassResource tagIcon = 
			new ClassResource(
				"ui/tagmanager/resources/reddiamd.gif", getApplication());

		tagTree.addItem(tagDefinition);
		tagTree.getContainerProperty(
				tagDefinition, 
				TagTreePropertyName.caption).setValue(
						tagDefinition.getName());
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

	public TreeTable getTagTree() {
		return tagTree;
	}
	
	public void close() {
		if (withTagsetButtons) {
			tagManager.removePropertyChangeListener(
					TagManagerEvent.tagsetDefinitionChanged,
					tagsetDefinitionChangedListener);
			tagManager.removeTagLibrary(tagLibrary);
		}
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);
	}
	
	public TagManager getTagManager() {
		return tagManager;
	}
	
	public TagDefinition getTagDefinition(String tagDefinitionID) {
		for (Object item : tagTree.getItemIds()) {
			if ((item instanceof TagDefinition) 
					&& ((TagDefinition)item).getUuid().equals(tagDefinitionID)) {
				return (TagDefinition)item;
			}
		}
		return null;
	}
	
	public TagsetDefinition getTagsetDefinition(String tagDefinitionID) {
		return getTagsetDefinition(getTagDefinition(tagDefinitionID));
	}

	public void removeTagsetDefinition(TagsetDefinition tagsetDefinition) {
		removeTagsetDefinitionFromTree(tagsetDefinition);
	}

	private void removeTagsetDefinitionFromTree(TagsetDefinition tagsetDef) {
		for (TagDefinition td : tagsetDef) {
			removeTagDefinitionFromTree(td, tagsetDef);
		}
		tagTree.removeItem(tagsetDef);
	}
	
	public void addValueChangeListener(ValueChangeListener valueChangeListener) {
		tagTree.addListener(valueChangeListener);
	}
	
	public void removeValueChangeListener(ValueChangeListener valueChangeListener) {
		tagTree.removeListener(valueChangeListener);
	}
}