package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Property;
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

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.PropertyPossibleValueList;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.core.tag.TagManager.TagManagerEvent;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.tag.Version;
import de.catma.core.util.ColorConverter;
import de.catma.core.util.IDGenerator;
import de.catma.core.util.Pair;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.PropertyCollection;
import de.catma.ui.dialog.StringProperty;
import de.catma.ui.dialog.TagDefinitionFieldFactory;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;

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
	private PropertyChangeListener tagsetDefAddedListener;
	private PropertyChangeListener tagsetDefNameChangedListener;
	private PropertyChangeListener tagsetDefRemovedListener;
	private PropertyChangeListener tagDefAddedListener;
	private PropertyChangeListener tagDefRemovedListener;
	private PropertyChangeListener tagDefChangedListener;

	public TagsetTree(TagManager tagManager, TagLibrary tagLibrary) {
		this(tagManager, tagLibrary, true, null);
	}
	
	public TagsetTree(
			TagManager tagManager, final TagLibrary tagLibrary, 
			boolean withTagsetButtons, 
			ColorButtonListener colorButtonListener) {
		this.tagManager = tagManager;
		this.tagLibrary = tagLibrary;
		this.withTagsetButtons = withTagsetButtons;
		this.colorButtonListener = colorButtonListener;
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init){
			initComponents();
			initAction();
			init = false;
		}
	}
	
	private void initAction() {
		if (withTagsetButtons) {
			tagsetDefAddedListener = 
					new PropertyChangeListener() {
				
				public void propertyChange(PropertyChangeEvent evt) {
					
					@SuppressWarnings("unchecked")
					Pair<TagLibrary, TagsetDefinition> addOperationResult = 
						(Pair<TagLibrary,TagsetDefinition>)evt.getNewValue();
					
					if (tagLibrary.equals(addOperationResult.getFirst())) {
						addTagsetDefinition(addOperationResult.getSecond());
					}
				}
			};
					
			this.tagManager.addPropertyChangeListener(
					TagManagerEvent.tagsetDefinitionAdded, 
					tagsetDefAddedListener);
			
			tagsetDefNameChangedListener = 
					new PropertyChangeListener() {
				
				public void propertyChange(PropertyChangeEvent evt) {
					TagsetDefinition tagsetDefinition = 
							(TagsetDefinition)evt.getNewValue();
					if (tagTree.containsId(tagsetDefinition)) {
						tagTree.getContainerProperty(
							tagsetDefinition, TagTreePropertyName.caption).setValue(
									tagsetDefinition.getName());
					}
				}
			};
			
			this.tagManager.addPropertyChangeListener(
					TagManagerEvent.tagsetDefinitionNameChanged,
					tagsetDefNameChangedListener);
			
			tagsetDefRemovedListener =
					new PropertyChangeListener() {
						
				public void propertyChange(PropertyChangeEvent evt) {
					@SuppressWarnings("unchecked")
					Pair<TagLibrary, TagsetDefinition> removeOperationResult = 
						(Pair<TagLibrary,TagsetDefinition>)evt.getOldValue();
					
					if (tagLibrary.equals(removeOperationResult.getFirst())) {
						TagsetDefinition tagsetDef = 
								removeOperationResult.getSecond();
						for (TagDefinition td : tagsetDef) {
							removeTagDefinition(td);
						}
						tagTree.removeItem(tagsetDef);
					}
					
				}
			};
			
			this.tagManager.addPropertyChangeListener(
					TagManagerEvent.tagsetDefinitionRemoved,
					tagsetDefRemovedListener);
			
			this.btInsertTagset.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleInsertTagsetDefinition();
				}
			});
			
			this.btEditTagset.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleEditTagsetDefinition();
				}
			});

			btRemoveTagset.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleRemoveTagsetDefinition();
				}
			});
		}
		
		tagDefAddedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
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
		};
		
		this.tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionAdded,
				tagDefAddedListener);
		
		tagDefRemovedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				@SuppressWarnings("unchecked")
				Pair<TagsetDefinition, TagDefinition> removeOperationResult = 
					(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
				TagDefinition td = removeOperationResult.getSecond();
				if (tagTree.containsId(td)) {
					removeTagDefinition(td);
				}
			}
		};
		
		this.tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionRemoved,
				tagDefRemovedListener);
		
		tagDefChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				TagDefinition tagDefinition = (TagDefinition)evt.getNewValue();
				Property prop = tagTree.getContainerProperty(
						tagDefinition, 
						TagTreePropertyName.caption);
				if (prop != null) {
					prop.setValue(tagDefinition.getType());
				}
			}
		};
		
		this.tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefChangedListener);
		
		btInsertTag.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleInsertTagDefinition();
			}
		});
		
		btRemoveTag.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleRemoveTagDefinition();
			}

		});
		
		// hier gehts weiter: 
		// Problem: 
		/*
		 * Es existieren verschiedene Instanzen der selben TagDefinition, z. B. 
		 * einmal aus independent TagLib und einmal aus dem UserMarkupDoc
		 * Bei Aenderungen sollten alle TagDefinition geaendert werden.
		 * Der TagManager sollte irgendwie Buch über die verschiedenen Intanzen  
		 * fuehren oder alles auf eine Instanz runterbrechen. 
		 * Evtl. muss equals/hashcode ueberschrieben werden, aber mit Version, 
		 * die dafuer immutable sein muss.
		 * Schon beim Laden muessen Versionsunterschiede gemeldet werden, damit 
		 * spaeter klar ist wie der TagManager damit umgehen soll. Entweder alte  
		 * Versionen werden gleich hochgezogen oder sie werden spaeter bei  
		 * Aenderungen an der neureren Version auch nicht angefasst!
		 * 
		 * Wie werden Versionen verglichen (date vs. int+uid)?
		 * Wie wirkt sich Versionierung auf den Index aus?
		 * Brauchen Propeties eine Version oder läuft das so wie bisher über die Tag Version?
		 * Bei Tagsets ändert sich die Version nicht wenn die enthaltenden Tags 
		 * sich ändern! Also inkonsistentes Verhalten.
		 * Gut wäre wahrscheinlich wenn Versionsänderungen von PropertyVersionen
		 * bis zur Tagset Version durchpropagiert werden.
		 * 
		 * Idee:
		 * der TagManager wird beim Laden von TagLibs benutzt und er erzeugt nur
		 * neue Instanzen wenn es sich um bisher unbekannte TagDefs handelt oder
		 * um TagDefs in einer anderen Version. Hier lässt sich dann beim Laden auch
		 * gleich entscheiden wie verfahren werden soll! Die unterschiedlichen Versionen sollen
		 * später dann ja auch beim Taggen nicht gemischt werden!
		 *  
		 */
		// - implement editTag
		// -propertyActions
		// -deserialize
		btEditTag.addListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				handleEditTagDefinition();
			}
		});
		
		tagTree.addListener(
				new ButtonStateMachine(
						withTagsetButtons,
						btRemoveTagset, btEditTagset, 
						btInsertTag, btRemoveTag, btEditTag, 
						btInsertProperty, btRemoveProperty, btEditProperty));
	}
	
	private void handleEditTagDefinition() {
		Object selValue = tagTree.getValue();
		
		if ((selValue != null) 
			&& (selValue instanceof TagDefinition)) {
			final TagDefinition selTagDefinition = (TagDefinition)selValue;
			final String tagDefNameProp = "name";
			final String tagDefColorProp = "color";
			
			PropertyCollection propertyCollection = 
					new PropertyCollection(tagDefNameProp, tagDefColorProp);
			
			propertyCollection.getItemProperty(tagDefNameProp).setValue(
					selTagDefinition.getType());
			propertyCollection.getItemProperty(tagDefColorProp).setValue(
					selTagDefinition.getColor());
			
			FormDialog tagFormDialog = new FormDialog(
				"Edit Tag",
				propertyCollection,
				new TagDefinitionFieldFactory(tagDefColorProp),
				new FormDialog.SaveCancelListener() {
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

	private void removeTagDefinition(TagDefinition td) {
		for (PropertyDefinition pd : 
			td.getSystemPropertyDefinitions()) {
			tagTree.removeItem(pd);
		}
		for (PropertyDefinition pd :
			td.getUserDefinedPropertyDefinitions()) {
			tagTree.removeItem(pd);
		}
		tagTree.removeItem(td);
	}

	private void handleRemoveTagDefinition() {
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
	
	private void handleInsertTagDefinition() {
		final String tagDefNameProp = "name";
		final String tagDefColorProp = "color";
		
		PropertyCollection propertyCollection = 
				new PropertyCollection(tagDefNameProp, tagDefColorProp);
		final Object selectedParent = 
				tagTree.getValue();
		
		if (selectedParent == null) {
			return;
		}
		
		FormDialog tagFormDialog =
			new FormDialog(
				"Create new Tag",
				propertyCollection,
				new TagDefinitionFieldFactory(tagDefColorProp),
				new FormDialog.SaveCancelListener() {
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
							baseID = 
								TagDefinition.CATMA_BASE_TAG.getID();
							tagsetDefinition = 
									(TagsetDefinition)selectedParent;
						}
						else if (selectedParent instanceof TagDefinition) {
							baseID = 
								((TagDefinition)selectedParent).getID();
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
						
						TagDefinition tagDefinition = 
								new TagDefinition(
									new IDGenerator().generate(),
									(String)nameProperty.getValue(),
									new Version(), 
									baseID);
						PropertyDefinition colorPropertyDef =
								new PropertyDefinition(
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

	private TagsetDefinition getTagsetDefinition(TagDefinition tagDefinition) {
		Object parent = tagTree.getParent(tagDefinition);
		if (parent instanceof TagsetDefinition) {
			return (TagsetDefinition)parent;
		}
		else {
			return getTagsetDefinition((TagDefinition)parent);
		}
	}

	private void handleRemoveTagsetDefinition() {
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

	private void handleInsertTagsetDefinition() {
		final String tagsetdefinitionnameProperty = "name";
		
		PropertyCollection propertyCollection = 
				new PropertyCollection(tagsetdefinitionnameProperty);

		FormDialog tagsetFormDialog =
			new FormDialog(
				"Create new Tagset",
				propertyCollection,
				new FormDialog.SaveCancelListener() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						Property property = 
								propertysetItem.getItemProperty(
										tagsetdefinitionnameProperty);
						TagsetDefinition td = 
								new TagsetDefinition(
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
	
	private void handleEditTagsetDefinition() {
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
					new FormDialog.SaveCancelListener() {
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

	private void addTagDefinition(TagDefinition tagDefinition) {
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

	public TreeTable getTagTree() {
		return tagTree;
	}
	
	public void close() {
		if (withTagsetButtons) {
			tagManager.removePropertyChangeListener(
					TagManagerEvent.tagsetDefinitionAdded, 
					tagsetDefAddedListener);
			tagManager.removePropertyChangeListener(
					TagManagerEvent.tagsetDefinitionNameChanged,
					tagsetDefNameChangedListener);
		}
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionAdded,
				tagDefAddedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionRemoved,
				tagDefRemovedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefChangedListener);
	}
}
