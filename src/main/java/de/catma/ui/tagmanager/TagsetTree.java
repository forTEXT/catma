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
package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.event.Action;
import com.vaadin.server.ClassResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.HierarchicalContainer;
import com.vaadin.v7.data.util.PropertysetItem;
import com.vaadin.v7.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.TreeTable;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.EndorsedTreeTable;
import de.catma.ui.dialog.FormDialog;
import de.catma.ui.dialog.PropertyCollection;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.StringProperty;
import de.catma.ui.dialog.TagDefinitionFieldFactory;
import de.catma.ui.tagger.CurrentWritableUserMarkupCollectionProvider;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class TagsetTree extends HorizontalLayout {

	private static enum TagTreePropertyName {
		caption, icon, color,;
	}

	private TreeTable tagTree;
	private Button btLoadIntoDocument;
	private Button btInsertTagset;
	private Button btRemoveTagset;
	private Button btEditTagset;
	private Button btInsertTag;
	private Button btRemoveTag;
	private Button btEditTag;
	private Button btInsertProperty;
	private Button btRemoveProperty;
	private Button btEditProperty;
	private boolean withReloadButton;
	private boolean withTagsetButtons;
	private boolean withTagButtons;
	private boolean withPropertyButtons;
	private boolean withDocumentButtons;
	private VerticalLayout propertyPanel;
	private ColorButtonListener colorButtonListener;
	private TagManager tagManager;
	private TagLibrary tagLibrary;
	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener userPropertyDefinitionChangedListener;
	private Button btReload;
	private TagsetSelectionListener tagsetSelectionListener;
	private Repository repository;
	private CurrentWritableUserMarkupCollectionProvider currentWritableUserMarkupCollectionProvider;


	public TagsetTree(TagManager tagManager, final TagLibrary tagLibrary, boolean withReloadButton,
			boolean withTagsetButtons, boolean withTagButtons, boolean withPropertyButtons, boolean withDocumentButtons,
			ColorButtonListener colorButtonListener, 
			 TagsetSelectionListener tagsetSelectionListener,
			Repository repository,CurrentWritableUserMarkupCollectionProvider currentWritableUserMarkupCollectionProvider ) {
		this.tagManager = tagManager;
		this.tagLibrary = tagLibrary;
		this.withReloadButton = withReloadButton;
		this.withTagsetButtons = withTagsetButtons;
		this.withTagButtons = withTagButtons;
		this.withPropertyButtons = withPropertyButtons;
		this.withDocumentButtons = withDocumentButtons;
		this.colorButtonListener = colorButtonListener;
		this.tagsetSelectionListener = tagsetSelectionListener;
		this.repository = repository;
		this.currentWritableUserMarkupCollectionProvider= currentWritableUserMarkupCollectionProvider;

		initComponents();
		initActions();
	}

		public TagsetTree(TagManager tagManager, TagLibrary tagLibrary) {
			this(tagManager, tagLibrary, true, true, true, true, true, null,null,null,null);
		}

	public TagsetTree(TagManager tagManager, final TagLibrary tagLibrary, boolean withReloadButton,
			boolean withTagsetButtons, boolean withTagButtons, boolean withPropertyButtons, boolean withDocumentButtons,
			ColorButtonListener colorButtonListener, 
			TagsetSelectionListener tagsetSelectionListener,
			Repository repository ) {
		this(tagManager,tagLibrary,withReloadButton,withTagsetButtons,withTagButtons,withPropertyButtons,withDocumentButtons,colorButtonListener,tagsetSelectionListener,repository,null);
	}

	public TagsetTree(TagManager tagManager, final TagLibrary tagLibrary, boolean withReloadButton,
			boolean withTagsetButtons, boolean withTagButtons, boolean withPropertyButtons, boolean withDocumentButtons,
			ColorButtonListener colorButtonListener, 
			Repository repository,CurrentWritableUserMarkupCollectionProvider currentWritableUserMarkupCollectionProvider ) {
		this(tagManager,tagLibrary,withReloadButton,withTagsetButtons,withTagButtons,withPropertyButtons,withDocumentButtons,colorButtonListener,null,repository, currentWritableUserMarkupCollectionProvider);
		
	}

	public TagsetTree(TagManager tagManager, final TagLibrary tagLibrary, boolean withReloadButton,
			boolean withTagsetButtons, boolean withTagButtons, boolean withPropertyButtons, boolean withDocumentButtons,
			ColorButtonListener colorButtonListener) {
		this(tagManager,tagLibrary,withReloadButton,withTagsetButtons,withTagButtons,withPropertyButtons,withDocumentButtons,colorButtonListener,null,null, null);

	}

	private void initActions() {
		tagsetDefinitionChangedListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) {
					if (withTagsetButtons) {
						TagsetDefinition addOperationResult = (TagsetDefinition) evt
								.getNewValue();

						addTagsetDefinition(addOperationResult);
					}
				} else if (evt.getNewValue() == null) {
					TagsetDefinition removeOperationResult = (TagsetDefinition) evt
							.getOldValue();

					removeTagsetDefinitionFromTree(removeOperationResult);
				} else {
					TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getNewValue();
					if (tagTree.containsId(tagsetDefinition)) {
						tagTree.getContainerProperty(tagsetDefinition, TagTreePropertyName.caption)
								.setValue(tagsetDefinition.getName());
					}
				}
			}
		};

		this.tagManager.addPropertyChangeListener(TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);

		if (withDocumentButtons) {
			btLoadIntoDocument.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleLoadIntoDocumentRequest(tagsetSelectionListener); 
				}
			});
		}

		if (withTagsetButtons) {
			this.btInsertTagset.addClickListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleInsertTagsetDefinitionRequest();
				}
			});

			this.btEditTagset.addClickListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					handleEditTagsetDefinitionRequest();
				}
			});

			btRemoveTagset.addClickListener(new ClickListener() {
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
					Pair<TagsetDefinition, TagDefinition> addOperationResult = (Pair<TagsetDefinition, TagDefinition>) evt
							.getNewValue();
					TagsetDefinition tagsetDefinition = addOperationResult.getFirst();
					TagDefinition tagDefinition = addOperationResult.getSecond();
					if (tagTree.containsId(tagsetDefinition)) {
						addTagDefinition(tagDefinition);
						establishHierarchy(tagsetDefinition, tagDefinition);
						configureChildren(tagDefinition);
					}
				} else if (newValue == null) {
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> removeOperationResult = (Pair<TagsetDefinition, TagDefinition>) evt
							.getOldValue();
					TagDefinition td = removeOperationResult.getSecond();
					if (tagTree.containsId(td)) {
						removeTagDefinitionFromTree(td, removeOperationResult.getFirst());
					}
				} else {
					TagDefinition tagDefinition = ((Pair<TagDefinition, TagsetDefinition>) evt.getNewValue()).getFirst();

					Property prop = tagTree.getContainerProperty(tagDefinition, TagTreePropertyName.caption);
					if (prop != null) {
						prop.setValue(tagDefinition.getName());
					}
				}
			}
		};

		this.tagManager.addPropertyChangeListener(TagManagerEvent.tagDefinitionChanged, tagDefinitionChangedListener);

		userPropertyDefinitionChangedListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();

				if (oldValue == null) { // insert

					@SuppressWarnings("unchecked")
					Pair<PropertyDefinition, TagDefinition> newPair = (Pair<PropertyDefinition, TagDefinition>) newValue;
					if (tagTree.containsId(newPair.getSecond())) {
						addUserDefinedPropertyDefinition(newPair.getFirst(), newPair.getSecond());
					}
				} else if (newValue == null) { // delete
					@SuppressWarnings("unchecked")
					Pair<PropertyDefinition, TagDefinition> oldPair = (Pair<PropertyDefinition, TagDefinition>) oldValue;
					if (tagTree.containsId(oldPair.getFirst())) {
						removeUserDefinedPropertyDefinitionFromTree(oldPair.getFirst());
					}
				} else { // update

					PropertyDefinition pd = (PropertyDefinition) evt.getNewValue();
					if (tagTree.containsId(pd)) {
						Property contProp = tagTree.getContainerProperty(pd, TagTreePropertyName.caption);

						if (contProp != null) {
							contProp.setValue(pd.getName());
						}
					}
				}
			}
		};

		this.tagManager.addPropertyChangeListener(TagManagerEvent.userPropertyDefinitionChanged,
				userPropertyDefinitionChangedListener);

		if (withTagButtons) {
			btInsertTag.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleInsertTagDefinitionRequest();
				}
			});

			btRemoveTag.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleRemoveTagDefinitionRequest();
				}

			});

			btEditTag.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleEditTagDefinitionRequest();
				}
			});
		}

		if (withPropertyButtons) {
			btInsertProperty.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleInsertPropertyDefinitionRequest();
				}
			});

			btEditProperty.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleEditPropertyDefinitionRequest();
				}
			});

			btRemoveProperty.addClickListener(new ClickListener() {

				public void buttonClick(ClickEvent event) {
					handleDeletePropertyDefinitionRequest();
				}
			});
		}

		tagTree.addValueChangeListener(new ButtonStateManager(withTagsetButtons, withTagButtons, withPropertyButtons,
				withDocumentButtons, btLoadIntoDocument, btRemoveTagset, btEditTagset, btInsertTag, btRemoveTag,
				btEditTag, btInsertProperty, btRemoveProperty, btEditProperty));

		if (propertyPanel != null) {
			tagTree.addValueChangeListener(new PanelStateManager(propertyPanel));
		}
	}

	private void removeUserDefinedPropertyDefinitionFromTree(PropertyDefinition propertyDefinition) {
		Object parent = tagTree.getParent(propertyDefinition);
		this.tagTree.removeItem(propertyDefinition);
		if (!tagTree.hasChildren(parent)) {
			tagTree.setChildrenAllowed(parent, false);
		}

	}

	private void handleDeletePropertyDefinitionRequest() {
		final Object selectedValue = tagTree.getValue();
		if (!(selectedValue instanceof PropertyDefinition)) {
			return;
		}

		final PropertyDefinition pd = (PropertyDefinition) selectedValue;
		final TagDefinition parent = (TagDefinition) tagTree.getParent(pd);
		final TagsetDefinition tagsetDefinition = getTagsetDefinition(parent);
		ConfirmDialog.show(UI.getCurrent(), Messages.getString("TagsetTree.deleteProperty"), //$NON-NLS-1$
				Messages.getString("TagsetTree.deletePropertyQuestion"), Messages.getString("TagsetTree.Yes"), //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("TagsetTree.No"), //$NON-NLS-1$
				new ConfirmDialog.Listener() {

					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {
							tagManager.removeUserDefinedPropertyDefinition(pd, parent, tagsetDefinition);
						}
					}
				});
	}

	private void handleEditPropertyDefinitionRequest() {
		final Object selectedValue = tagTree.getValue();
		if (!(selectedValue instanceof PropertyDefinition)) {
			return;
		}
		final PropertyDefinition pd = (PropertyDefinition) selectedValue;
		final TagDefinition parent = (TagDefinition) tagTree.getParent(pd);

		PropertyDefinitionDialog propertyDefinitionDialog = new PropertyDefinitionDialog(
				Messages.getString("TagsetTree.editProperty"), pd, //$NON-NLS-1$
				new SaveCancelListener<PropertyDefinition>() {

					public void cancelPressed() {
					}

					public void savePressed(PropertyDefinition result) {
						tagManager.updateUserDefinedPropertyDefinition(parent, result);
					}
				});
		propertyDefinitionDialog.show();

	}

	private void handleInsertPropertyDefinitionRequest() {

		final Object selectedParent = tagTree.getValue();

		if (selectedParent == null) {
			return;
		}

		PropertyDefinitionDialog propertyDefinitionDialog = new PropertyDefinitionDialog(
				Messages.getString("TagsetTree.createProperty"), //$NON-NLS-1$
				new SaveCancelListener<PropertyDefinition>() {

					public void cancelPressed() {
					}

					public void savePressed(PropertyDefinition result) {
						TagDefinition td = (TagDefinition) selectedParent;
						tagManager.addUserDefinedPropertyDefinition(td, result);
					}
				});
		propertyDefinitionDialog.show();
	}

	private void handleEditTagDefinitionRequest() {
		Object selValue = tagTree.getValue();

		if ((selValue != null) && (selValue instanceof TagDefinition)) {
			final TagDefinition selTagDefinition = (TagDefinition) selValue;
			final String tagDefNameProp = "name"; //$NON-NLS-1$
			final String tagDefColorProp = "color"; //$NON-NLS-1$

			PropertyCollection propertyCollection = new PropertyCollection(tagDefNameProp, tagDefColorProp);

			propertyCollection.getItemProperty(tagDefNameProp).setValue(selTagDefinition.getName());
			propertyCollection.getItemProperty(tagDefColorProp)
					.setValue(ColorConverter.toHex(selTagDefinition.getColor()));

			FormDialog<PropertysetItem> tagFormDialog = new FormDialog<PropertysetItem>(
					Messages.getString("TagsetTree.editTag"), //$NON-NLS-1$
					propertyCollection, new TagDefinitionFieldFactory(tagDefColorProp),
					new SaveCancelListener<PropertysetItem>() {
						public void cancelPressed() {
						}

						public void savePressed(PropertysetItem propertysetItem) {

							Property<?> nameProperty = propertysetItem.getItemProperty(tagDefNameProp);

							Property<?> colorProperty = propertysetItem.getItemProperty(tagDefColorProp);

							tagManager.setTagDefinitionTypeAndColor(
									selTagDefinition, (String) nameProperty.getValue(),
									ColorConverter.toRGBIntAsString((String) colorProperty.getValue()),
									getTagsetDefinition(selTagDefinition));
						}
					});
			tagFormDialog.show("50%"); //$NON-NLS-1$
		}

	}

	private void removeTagDefinitionFromTree(TagDefinition td, TagsetDefinition tagsetDefinition) {

		Collection<Object> children = new ArrayList<Object>();
		if (tagTree.hasChildren(td)) {
			children.addAll(tagTree.getChildren(td));
		}
		for (Object child : children) {
			if (child instanceof TagDefinition) {
				removeTagDefinitionFromTree((TagDefinition) child, tagsetDefinition);
			}
		}

		for (PropertyDefinition pd : td.getSystemPropertyDefinitions()) {
			tagTree.removeItem(pd);
		}
		for (PropertyDefinition pd : td.getUserDefinedPropertyDefinitions()) {
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

		if ((selValue != null) && (selValue instanceof TagDefinition)) {
			final TagDefinition td = (TagDefinition) selValue;

			ConfirmDialog.show(UI.getCurrent(), Messages.getString("TagsetTree.deleteTag"), //$NON-NLS-1$
					Messages.getString("TagsetTree.deleteTagQuestion"), Messages.getString("TagsetTree.Yes"), //$NON-NLS-1$ //$NON-NLS-2$
					Messages.getString("TagsetTree.No"), //$NON-NLS-1$
					new ConfirmDialog.Listener() {

						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								tagManager.removeTagDefinition(getTagsetDefinition(td), td);
							}
						}
					});
		}
	}


	private void handleInsertTagDefinitionRequest() {
		
		final Object selectedParent = tagTree.getValue();	
		boolean noOpenTagsets = tagTree.getItemIds().isEmpty();
		
		if (selectedParent == null ) {

			if (!noOpenTagsets) {
				Notification.show(Messages.getString("TagsetTree.infoTitle"), //$NON-NLS-1$
						Messages.getString("TagsetTree.selectTagsetParentTagFirst"), //$NON-NLS-1$
						Type.TRAY_NOTIFICATION);

			} else {

				CreateTagsetOptionsSelectionDialog createTagsetOptionsSelectionDialog = new CreateTagsetOptionsSelectionDialog(
						new TagsetSelectionListener() {
							@Override
							public void tagsetSelected(Object selectedParent) {
								handleInsertTagDefinitionRequest(selectedParent);
							}
						}, repository,currentWritableUserMarkupCollectionProvider);
				createTagsetOptionsSelectionDialog.show();
			}

		} else {
			handleInsertTagDefinitionRequest(selectedParent);
		}

	}

	@SuppressWarnings("unchecked")
	private void handleInsertTagDefinitionRequest(Object selectedParent) {
		final String tagDefNameProp = "name"; //$NON-NLS-1$
		final String tagDefColorProp = "color"; //$NON-NLS-1$

		PropertyCollection propertyCollection = new PropertyCollection(tagDefNameProp, tagDefColorProp);
		propertyCollection.getItemProperty(tagDefColorProp).setValue(ColorConverter.randomHex());

		FormDialog<PropertysetItem> tagFormDialog = new FormDialog<PropertysetItem>(
				Messages.getString("TagsetTree.createNewTag"), //$NON-NLS-1$
				propertyCollection, new TagDefinitionFieldFactory(tagDefColorProp),
				new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {
					}

					public void savePressed(PropertysetItem propertysetItem) {

						Property<?> nameProperty = propertysetItem.getItemProperty(tagDefNameProp);

						Property<?> colorProperty = propertysetItem.getItemProperty(tagDefColorProp);

						String baseID = null;
						TagsetDefinition tagsetDefinition = null;

						if (selectedParent instanceof TagsetDefinition) {
							baseID = ""; //$NON-NLS-1$
							tagsetDefinition = (TagsetDefinition) selectedParent;
						} else if (selectedParent instanceof TagDefinition) {
							baseID = ((TagDefinition) selectedParent).getUuid();
							tagsetDefinition = getTagsetDefinition((TagDefinition) selectedParent);
						} else {
							throw new IllegalStateException("a parent of a TagDefinition has to be either a" //$NON-NLS-1$
									+ "TagDefinition or a TagsetDefinition and not a " //$NON-NLS-1$
									+ selectedParent.getClass().getName());
						}
						IDGenerator idGenerator = new IDGenerator();
						TagDefinition tagDefinition = new TagDefinition(null, idGenerator.generate(),
								(String) nameProperty.getValue(), new Version(),
								(baseID.isEmpty() ? null : ((TagDefinition) selectedParent).getId()), baseID,
								tagsetDefinition.getUuid());
						PropertyDefinition colorPropertyDef = new PropertyDefinition(
								idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()),
								PropertyDefinition.SystemPropertyName.catma_displaycolor.name(),
								Collections.singleton(
										ColorConverter.toRGBIntAsString((String) colorProperty.getValue())));
						tagDefinition.addSystemPropertyDefinition(colorPropertyDef);
						tagManager.addTagDefinition(tagsetDefinition, tagDefinition);
					}
				});
		tagFormDialog.show("50%"); //$NON-NLS-1$

	}

	public TagsetDefinition getTagsetDefinition(TagDefinition tagDefinition) {
		Object parent = tagTree.getParent(tagDefinition);
		if (parent instanceof TagsetDefinition) {
			return (TagsetDefinition) parent;
		} else {
			return getTagsetDefinition((TagDefinition) parent);
		}
	}

	private void handleRemoveTagsetDefinitionRequest() {
		Object selValue = tagTree.getValue();

		if ((selValue != null) && (selValue instanceof TagsetDefinition)) {
			final TagsetDefinition td = (TagsetDefinition) selValue;

			ConfirmDialog.show(UI.getCurrent(), Messages.getString("TagsetTree.deleteTagset"), //$NON-NLS-1$
					Messages.getString("TagsetTree.deleteTagsetQuestion"), Messages.getString("TagsetTree.Yes"), //$NON-NLS-1$ //$NON-NLS-2$
					Messages.getString("TagsetTree.No"), //$NON-NLS-1$
					new ConfirmDialog.Listener() {

						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								tagManager.removeTagsetDefinition(td);
							}
						}
					});
		}
	}

	private void handleInsertTagsetDefinitionRequest() {
		if (tagLibrary == null) {
			Notification.show(Messages.getString("TagsetTree.infoTitle"), //$NON-NLS-1$
					Messages.getString("TagsetTree.selectTagLibraryFirst"), //$NON-NLS-1$
					Type.TRAY_NOTIFICATION);
			return;
		}

		final String tagsetdefinitionnameProperty = "name"; //$NON-NLS-1$

		PropertyCollection propertyCollection = new PropertyCollection(tagsetdefinitionnameProperty);

		FormDialog<PropertysetItem> tagsetFormDialog = new FormDialog<PropertysetItem>(
				Messages.getString("TagsetTree.createNewTagset"), //$NON-NLS-1$
				propertyCollection, new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {
					}

					public void savePressed(PropertysetItem propertysetItem) {
						Property<?> property = propertysetItem.getItemProperty(tagsetdefinitionnameProperty);
						TagsetDefinition td = new TagsetDefinition(null, new IDGenerator().generate(),
								(String) property.getValue(), new Version());

						tagManager.addTagsetDefinition(td);
					}
				});
		configureTagsetFormDialog(tagsetFormDialog, tagsetdefinitionnameProperty);
		tagsetFormDialog.show();
	}

	private void handleEditTagsetDefinitionRequest() {
		final String tagsetdefinitionnameProperty = "name"; //$NON-NLS-1$

		Object selValue = tagTree.getValue();

		if ((selValue != null) && (selValue instanceof TagsetDefinition)) {

			final TagsetDefinition curSelTagsetDefinition = (TagsetDefinition) selValue;

			PropertyCollection propertyCollection = new PropertyCollection();
			propertyCollection.addItemProperty(tagsetdefinitionnameProperty,
					new StringProperty(curSelTagsetDefinition.getName()));

			FormDialog<PropertysetItem> tagsetFormDialog = new FormDialog<PropertysetItem>(
					Messages.getString("TagsetTree.editTagset"), //$NON-NLS-1$
					propertyCollection, new SaveCancelListener<PropertysetItem>() {
						public void cancelPressed() {
						}

						public void savePressed(PropertysetItem propertysetItem) {
							Property<?> property = propertysetItem.getItemProperty(tagsetdefinitionnameProperty);

							tagManager.setTagsetDefinitionName(curSelTagsetDefinition, (String) property.getValue());
						}
					});
			configureTagsetFormDialog(tagsetFormDialog, tagsetdefinitionnameProperty);

			tagsetFormDialog.show();
		}
	}

	private void configureTagsetFormDialog(FormDialog<PropertysetItem> formDialog, String propertyId) {
		formDialog.getField(propertyId).setRequired(true);
		formDialog.getField(propertyId).setRequiredError(Messages.getString("TagsetTree.enterNameObligation")); //$NON-NLS-1$
	}


	private void handleLoadIntoDocumentRequest(TagsetSelectionListener tagsetSelectionListener) {

		Object selValue = tagTree.getValue();

		if ((selValue != null) && (selValue instanceof TagsetDefinition)) {

			final TagsetDefinition curSelTagsetDefinition = (TagsetDefinition) selValue;

			CatmaApplication application = ((CatmaApplication) UI.getCurrent());
			application.addTagsetToActiveDocument(curSelTagsetDefinition, tagsetSelectionListener);
		}
	}
	

	private void initComponents() {
		setSizeFull();

		tagTree = new EndorsedTreeTable();
		tagTree.setImmediate(true);
		tagTree.setSizeFull();
		tagTree.setSelectable(true);
		tagTree.setMultiSelect(false);

		tagTree.setContainerDataSource(new HierarchicalContainer());

		tagTree.addContainerProperty(TagTreePropertyName.caption, String.class, null);
		tagTree.setColumnHeader(TagTreePropertyName.caption, Messages.getString("TagsetTree.Tagsets")); //$NON-NLS-1$

		tagTree.addContainerProperty(TagTreePropertyName.icon, Resource.class, null);

		tagTree.setItemCaptionPropertyId(TagTreePropertyName.caption);
		tagTree.setItemIconPropertyId(TagTreePropertyName.icon);
		tagTree.setItemCaptionMode(ItemCaptionMode.PROPERTY);

		tagTree.setVisibleColumns(new Object[] { TagTreePropertyName.caption });

		if (colorButtonListener != null) {
			tagTree.addGeneratedColumn(TagTreePropertyName.color, new ColorButtonColumnGenerator(colorButtonListener));
			tagTree.setColumnReorderingAllowed(true);
		} else {
			tagTree.addGeneratedColumn(TagTreePropertyName.color, new ColorLabelColumnGenerator());
		}
		tagTree.setColumnHeader(TagTreePropertyName.color, Messages.getString("TagsetTree.TagColor")); //$NON-NLS-1$
		addComponent(tagTree);
		setExpandRatio(tagTree, 0.7f);

		VerticalLayout buttonGrid = new VerticalLayout();

		if (withReloadButton) {
			VerticalLayout reloadPanel = new VerticalLayout();
			reloadPanel.setSpacing(true);
			reloadPanel.setMargin(new MarginInfo(true, true, true, true));

			btReload = new Button(Messages.getString("TagsetTree.reloadTagsets"), FontAwesome.REFRESH); //$NON-NLS-1$
			btReload.setWidth("100%"); //$NON-NLS-1$
			reloadPanel.addComponent(btReload);

			buttonGrid.addComponent(reloadPanel);
		}

		if (withDocumentButtons) {
			VerticalLayout documentPanel = new VerticalLayout();
			documentPanel.setSpacing(true);
			documentPanel.setMargin(new MarginInfo(false, true, false, true));

			btLoadIntoDocument = new Button(Messages.getString("TagsetTree.loadTagset")); //$NON-NLS-1$
			btLoadIntoDocument.addStyleName("primary-button"); //$NON-NLS-1$
			btLoadIntoDocument.setWidth("100%"); //$NON-NLS-1$
			btLoadIntoDocument.setEnabled(true);
			documentPanel.addComponent(btLoadIntoDocument);

			buttonGrid.addComponent(documentPanel);
		}

		if (withTagsetButtons) {
			VerticalLayout tagsetPanel = new VerticalLayout();
			tagsetPanel.setSpacing(true);
			tagsetPanel.setMargin(new MarginInfo(true, true, false, true));

			Label tagsetLabel = new Label();
			tagsetLabel.addStyleName("tagsettree-label"); //$NON-NLS-1$
			tagsetLabel.setIcon(new ClassResource("tagmanager/resources/grndiamd.gif")); //$NON-NLS-1$
			tagsetLabel.setCaption(Messages.getString("TagsetTree.Tagset")); //$NON-NLS-1$

			tagsetPanel.addComponent(tagsetLabel);

			btInsertTagset = new Button(Messages.getString("TagsetTree.createTagset")); //$NON-NLS-1$
			btInsertTagset.addStyleName("secondary-button"); //$NON-NLS-1$
			btInsertTagset.setEnabled(true);
			btInsertTagset.setWidth("100%"); //$NON-NLS-1$
			tagsetPanel.addComponent(btInsertTagset);

			btRemoveTagset = new Button(Messages.getString("TagsetTree.deleteTagset")); //$NON-NLS-1$
			btRemoveTagset.setWidth("100%"); //$NON-NLS-1$
			tagsetPanel.addComponent(btRemoveTagset);

			btEditTagset = new Button(Messages.getString("TagsetTree.editTagset")); //$NON-NLS-1$
			btEditTagset.setWidth("100%"); //$NON-NLS-1$
			tagsetPanel.addComponent(btEditTagset);

			buttonGrid.addComponent(tagsetPanel);
		}

		if (withTagButtons) {
			VerticalLayout tagPanel = new VerticalLayout();
			tagPanel.setSpacing(true);
			tagPanel.setMargin(new MarginInfo(true, true, false, true));

			Label tagLabel = new Label();
			tagLabel.addStyleName("tagsettree-label"); //$NON-NLS-1$

			tagLabel.setIcon(new ClassResource("tagmanager/resources/reddiamd.gif")); //$NON-NLS-1$
			tagLabel.setCaption(Messages.getString("TagsetTree.Tag")); //$NON-NLS-1$

			tagPanel.addComponent(tagLabel);
			tagPanel.setComponentAlignment(tagLabel, Alignment.BOTTOM_LEFT);

			btInsertTag = new Button(Messages.getString("TagsetTree.createTag")); //$NON-NLS-1$
			btInsertTag.addStyleName("secondary-button"); //$NON-NLS-1$
			btInsertTag.setWidth("100%"); //$NON-NLS-1$
			if (withTagsetButtons) {
				btInsertTag.setEnabled(true);
			}
			tagPanel.addComponent(btInsertTag);

			btRemoveTag = new Button(Messages.getString("TagsetTree.deleteTag")); //$NON-NLS-1$
			btRemoveTag.setWidth("100%"); //$NON-NLS-1$
			tagPanel.addComponent(btRemoveTag);

			btEditTag = new Button(Messages.getString("TagsetTree.editTag")); //$NON-NLS-1$
			btEditTag.setWidth("100%"); //$NON-NLS-1$
			tagPanel.addComponent(btEditTag);

			buttonGrid.addComponent(tagPanel);
		}

		if (withPropertyButtons) {
			propertyPanel = new VerticalLayout();
			propertyPanel.setSpacing(true);
			propertyPanel.setMargin(new MarginInfo(true, true, false, true));

			Label propertyLabel = new Label();
			propertyLabel.addStyleName("tagsettree-label"); //$NON-NLS-1$

			propertyLabel.setIcon(new ClassResource("tagmanager/resources/ylwdiamd.gif")); //$NON-NLS-1$
			propertyLabel.setCaption(Messages.getString("TagsetTree.Property")); //$NON-NLS-1$
			propertyLabel.setHeight("15px"); //$NON-NLS-1$
			propertyLabel.addStyleName("tagsettree-button-top-margin"); //$NON-NLS-1$

			propertyPanel.addComponent(propertyLabel);

			propertyPanel.setComponentAlignment(propertyLabel, Alignment.BOTTOM_LEFT);

			btInsertProperty = new Button(Messages.getString("TagsetTree.createProperty")); //$NON-NLS-1$
			btInsertProperty.setWidth("100%"); //$NON-NLS-1$
			btInsertProperty.addStyleName("tagsettree-button-top-margin"); //$NON-NLS-1$
			propertyPanel.addComponent(btInsertProperty);

			btRemoveProperty = new Button(Messages.getString("TagsetTree.removeProperty")); //$NON-NLS-1$
			btRemoveProperty.addStyleName("tagsettree-button-top-margin"); //$NON-NLS-1$
			btRemoveProperty.setWidth("100%"); //$NON-NLS-1$
			propertyPanel.addComponent(btRemoveProperty);

			btEditProperty = new Button(Messages.getString("TagsetTree.editProperty")); //$NON-NLS-1$
			btEditProperty.setWidth("100%"); //$NON-NLS-1$
			btEditProperty.addStyleName("tagsettree-button-top-margin"); //$NON-NLS-1$
			propertyPanel.addComponent(btEditProperty);

			buttonGrid.addComponent(propertyPanel);
		}
		if (buttonGrid.getComponentCount() > 0) {
			addComponent(buttonGrid);
			setExpandRatio(buttonGrid, 0.3f);
		}
	}

	private void clearTagsetDefinitions() {
		tagTree.removeAllItems();
	}

	private void setTagsetDefinitions(Collection<TagsetDefinition> tagsetDefinitions) {
		clearTagsetDefinitions();

		for (TagsetDefinition tagsetDefinition : tagsetDefinitions) {
			addTagsetDefinition(tagsetDefinition);
		}
	}


	public void addTagsetDefinition(TagsetDefinition tagsetDefinition,
			TagsetSelectionListener tagsetSelectionListener) {

		ClassResource tagsetIcon = new ClassResource("tagmanager/resources/grndiamd.gif"); //$NON-NLS-1$

		tagTree.addItem(tagsetDefinition);
		tagTree.getContainerProperty(tagsetDefinition, TagTreePropertyName.caption)
				.setValue(tagsetDefinition.getName());
		tagTree.getContainerProperty(tagsetDefinition, TagTreePropertyName.icon).setValue(tagsetIcon);

		tagTree.setCollapsed(tagsetDefinition, false);
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			addTagDefinition(tagDefinition);
		}

		for (TagDefinition tagDefinition : tagsetDefinition) {
			establishHierarchy(tagsetDefinition, tagDefinition);
		}

		for (TagDefinition tagDefinition : tagsetDefinition) {
			configureChildren(tagDefinition);
		}
		if (tagsetSelectionListener != null) {
			tagsetSelectionListener.tagsetSelected(tagsetDefinition);
		}

	}


	public void addTagsetDefinition(TagsetDefinition tagsetDefinition) {
		addTagsetDefinition(tagsetDefinition, null);

	}

	private void configureChildren(TagDefinition tagDefinition) {
		if (!tagTree.hasChildren(tagDefinition)) {
			tagTree.setChildrenAllowed(tagDefinition, false);
		}
	}

	private void establishHierarchy(TagsetDefinition tagsetDefinition, TagDefinition tagDefinition) {
		String baseID = tagDefinition.getParentUuid();
		if (baseID.isEmpty()) {
			tagTree.setChildrenAllowed(tagsetDefinition, true);
			tagTree.setParent(tagDefinition, tagsetDefinition);
		} else {
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			tagTree.setChildrenAllowed(parent, true);
			tagTree.setParent(tagDefinition, parent);
		}
	}

	private void addTagDefinition(TagDefinition tagDefinition) {
		ClassResource tagIcon = new ClassResource("tagmanager/resources/reddiamd.gif"); //$NON-NLS-1$

		tagTree.addItem(tagDefinition);
		tagTree.getContainerProperty(tagDefinition, TagTreePropertyName.caption).setValue(tagDefinition.getName());
		tagTree.getContainerProperty(tagDefinition, TagTreePropertyName.icon).setValue(tagIcon);

		for (PropertyDefinition propertyDefinition : tagDefinition.getUserDefinedPropertyDefinitions()) {
			addUserDefinedPropertyDefinition(propertyDefinition, tagDefinition);
		}
	}

	private void addUserDefinedPropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) {
		ClassResource propertyIcon = new ClassResource("tagmanager/resources/ylwdiamd.gif"); //$NON-NLS-1$

		tagTree.addItem(propertyDefinition);
		tagTree.setChildrenAllowed(tagDefinition, true);
		tagTree.setParent(propertyDefinition, tagDefinition);
		tagTree.getContainerProperty(propertyDefinition, TagTreePropertyName.caption)
				.setValue(propertyDefinition.getName());
		tagTree.getContainerProperty(propertyDefinition, TagTreePropertyName.icon).setValue(propertyIcon);
		tagTree.setChildrenAllowed(propertyDefinition, false);

	}

	public void close(boolean closeLibrary) {
		tagManager.removePropertyChangeListener(TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);

		tagManager.removePropertyChangeListener(TagManagerEvent.tagDefinitionChanged, tagDefinitionChangedListener);
		tagManager.removePropertyChangeListener(TagManagerEvent.userPropertyDefinitionChanged,
				userPropertyDefinitionChangedListener);
	}

	public void setTagLibrary(TagLibrary tagLibrary) {
		this.tagLibrary = tagLibrary;
		if (this.tagLibrary == null) {
			clearTagsetDefinitions();
		} else {
			this.setTagsetDefinitions(tagLibrary.getTagsetDefinitions());
		}
	}

	public TreeTable getTagTree() {
		return tagTree;
	}

	public TagManager getTagManager() {
		return tagManager;
	}

	public TagDefinition getTagDefinition(String tagDefinitionID) {
		for (Object item : tagTree.getItemIds()) {
			if ((item instanceof TagDefinition) && ((TagDefinition) item).getUuid().equals(tagDefinitionID)) {
				return (TagDefinition) item;
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
		tagTree.addValueChangeListener(valueChangeListener);
	}

	public void removeValueChangeListener(ValueChangeListener valueChangeListener) {
		tagTree.removeValueChangeListener(valueChangeListener);
	}

	public List<TagsetDefinition> getTagsetDefinitions() {
		ArrayList<TagsetDefinition> result = new ArrayList<TagsetDefinition>();

		for (Object itemId : tagTree.getItemIds()) {
			if (itemId instanceof TagsetDefinition) {
				result.add((TagsetDefinition) itemId);
			}

		}

		return result;
	}

	public void addActionHandler(Action.Handler actionHandler) {
		tagTree.addActionHandler(actionHandler);
	}

	public void addBtReloadListener(ClickListener listener) {
		btReload.addClickListener(listener);
	}
	
	public void addBtLoadIntoDocumentListener(ClickListener listener) {
		btLoadIntoDocument.addClickListener(listener);
	}
}