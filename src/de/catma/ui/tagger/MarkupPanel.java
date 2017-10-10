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
package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.Container;
import com.vaadin.event.Action;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagInstanceInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.menu.CMenuAction;
import de.catma.ui.repository.CorpusContentSelectionDialog;
import de.catma.ui.tagger.ChooseAnnotationCollectionDialog.AnnotationCollectionListener;
import de.catma.ui.tagger.MarkupCollectionsPanel.MarkupCollectionPanelEvent;
import de.catma.ui.tagger.TagInstanceTree.TagIntanceActionListener;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;
import de.catma.ui.tagmanager.TagsetTree;

public class MarkupPanel extends VerticalSplitPanel implements TagIntanceActionListener {

	static interface TagInstanceSelectedListener {
		public void tagInstanceSelected(TagInstance tagInstance);
	}

	private static interface CollectionSelectionListener {
		public void collectionSelected();
	}

	private TagsetTree tagsetTree;
	private TabSheet tabSheet;
	private MarkupCollectionsPanel markupCollectionsPanel;
	private PropertyChangeListener tagLibraryChangedListener;
	// private ColorButtonListener colorButtonListener;
	private TagInstanceTree tagInstancesTree;
	private Repository repository;
	private PropertyChangeListener propertyValueChangeListener;
	private Button btnOpenTagset;
	private Button btHelp;

	private MarkupHelpWindow markupHelpWindow = new MarkupHelpWindow();
	private Label writableUserMarkupCollectionInfo;
	private Panel markupInfoScrollPanel;
	private TagInstanceSelectedListener tagInstanceSelectedListener;
	private Tagger tagger;
	MarkupPanel markupPanel = this;

	public MarkupPanel(Repository repository, Tagger tagger, PropertyChangeListener tagDefinitionSelectionListener,
			PropertyChangeListener tagDefinitionsRemovedListener,
			TagInstanceSelectedListener tagInstanceSelectedListener, String sourceDocumentId) {
		this.tagger = tagger;
		this.repository = repository;
		this.tagInstanceSelectedListener = tagInstanceSelectedListener;
		initComponents(tagDefinitionSelectionListener, tagDefinitionsRemovedListener, sourceDocumentId);
		initActions();

		tagsetTree.getTagTree().setDropHandler(new DropHandler() {

			public AcceptCriterion getAcceptCriterion() {

				return AcceptItem.ALL;
			}

			public void drop(DragAndDropEvent event) {
				DataBoundTransferable transferable = (DataBoundTransferable) event.getTransferable();

				if (!(transferable.getSourceContainer() instanceof Container.Hierarchical)) {
					return;
				}

				final Object sourceItemId = transferable.getItemId();

				if (sourceItemId instanceof TagsetDefinition) {

					TagsetDefinition incomingTagsetDef = (TagsetDefinition) sourceItemId;

					addOrUpdateTagsetDefinition(UI.getCurrent(), incomingTagsetDef);
				}
			}
		});

	}

	private void initActions() {
		this.tagLibraryChangedListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { // removal
					TagLibrary tagLibrary = (TagLibrary) evt.getOldValue();
					for (TagsetDefinition tagsetDefinition : tagLibrary) {
						closeTagsetDefinition(tagsetDefinition);
					}
				}
			}
		};
		tagsetTree.getTagManager().addPropertyChangeListener(TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);

		markupCollectionsPanel.addPropertyChangeListener(MarkupCollectionPanelEvent.tagDefinitionSelected,
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getNewValue() == null) {
							@SuppressWarnings("unchecked")
							List<TagReference> deselectedTagRefs = (List<TagReference>) evt.getOldValue();
							showTagInstanceInfo(deselectedTagRefs.toArray(new TagReference[] {}));
						}
					}
				});
		markupCollectionsPanel.addPropertyChangeListener(MarkupCollectionPanelEvent.tagDefinitionsRemoved,
				new PropertyChangeListener() {

					@SuppressWarnings("unchecked")
					public void propertyChange(PropertyChangeEvent evt) {
						showTagInstanceInfo(tagInstancesTree.getTagInstanceIDs((Set<TagDefinition>) evt.getOldValue()),
								null);
					}
				});

		propertyValueChangeListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				if ((evt.getNewValue() != null) && (evt.getOldValue() != null)) {
					showTagInstanceInfo(tagInstancesTree.getTagInstanceIDs(Collections.<TagDefinition>emptySet()),
							null);
				}

			}
		};

		repository.addPropertyChangeListener(RepositoryChangeEvent.propertyValueChanged, propertyValueChangeListener);

		tagsetTree.addActionHandler(new Action.Handler() {
			private CMenuAction<TagsetDefinition> close = new CMenuAction<TagsetDefinition>(
					Messages.getString("MarkupPanel.Close")) { //$NON-NLS-1$
				@Override
				public void handle(TagsetDefinition tagsetDefinition) {
					closeTagsetDefinition(tagsetDefinition);
				}
			};

			@SuppressWarnings("unchecked")
			public void handleAction(Action action, Object sender, Object target) {

				if (target instanceof TagsetDefinition) {
					TagsetDefinition umc = (TagsetDefinition) target;
					((CMenuAction<TagsetDefinition>) action).handle(umc);
				}

			}

			public Action[] getActions(Object target, Object sender) {
				if ((target != null) && (target instanceof TagsetDefinition)) {
					return new Action[] { close };
				} else {
					return new Action[] {};
				}
			}
		});

		btnOpenTagset.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				handleOpenTagsetRequest();
			}
		});

		btHelp.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {

				if (markupHelpWindow.getParent() == null) {
					UI.getCurrent().addWindow(markupHelpWindow);
				} else {
					UI.getCurrent().removeWindow(markupHelpWindow);
				}

			}
		});

	}

	private void closeTagsetDefinition(TagsetDefinition tagsetDefinition) {
		tagsetTree.removeTagsetDefinition(tagsetDefinition);
		markupCollectionsPanel.removeUpdateableTagsetDefinition(tagsetDefinition);
	}

	private void initComponents(PropertyChangeListener tagDefinitionSelectionListener,
			PropertyChangeListener tagDefinitionsRemovedListener, final String sourceDocumentId) {

		setHeight("98%"); // necessary to prevent that the //$NON-NLS-1$
							// markuppanel flows out of the TaggerView
		tabSheet = new TabSheet();
		tabSheet.setSizeFull();
		VerticalLayout tabContent = new VerticalLayout();
		tabContent.setSpacing(true);
		tabContent.setSizeFull();
		tabContent.addStyleName("catma-tagger-panels"); //$NON-NLS-1$

		final ColorButtonListener colorButtonListener = new ColorButtonListener() {
			private boolean enabled = false;

			public void colorButtonClicked(TagDefinition tagDefinition) {
				if (enabled) {
					tagger.addTagInstanceWith(tagDefinition);
				} else {
					ChooseAnnotationCollectionDialog chooseAnnotationCollectionDialog = new ChooseAnnotationCollectionDialog(
							repository, 
							sourceDocumentId, 
							new AnnotationCollectionListener() {
								
								@Override
								public void openOrCreateCollection() {
									handleOpenUserMarkupCollectionRequest(
											repository.getSourceDocument(sourceDocumentId),
											new CollectionSelectionListener() {
												@Override
												public void collectionSelected() {
													if (enabled) {
														tagger.addTagInstanceWith(tagDefinition);
													} else {
														HTMLNotification.show(
																Messages.getString("TaggerView.infoTitle"), //$NON-NLS-1$
																Messages.getString(
																		"TaggerView.errorAddingAnnotationToActiveDocument"), //$NON-NLS-1$
																Type.TRAY_NOTIFICATION);
													}
												}
											});
									
									
								}
								@Override
								public void defaultCollectionCreated(UserMarkupCollection userMarkupCollection) {
									markupCollectionsPanel.openUserMarkupCollection(userMarkupCollection);
									tagger.addTagInstanceWith(tagDefinition);
									HTMLNotification.show(Messages.getString("TaggerView.infoTitle"), //$NON-NLS-1$
											Messages.getString("TaggerView.dialogAnAnnotationCollectionWasCreated"), //$NON-NLS-1$
											Type.TRAY_NOTIFICATION);
								}
							});
					chooseAnnotationCollectionDialog.show();
				}
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}
		};

		HorizontalLayout buttonHeaderPanel = new HorizontalLayout();
		buttonHeaderPanel.setWidth("95%"); //$NON-NLS-1$
		buttonHeaderPanel.setMargin(new MarginInfo(true, false, false, false));

		btnOpenTagset = new Button(Messages.getString("MarkupPanel.OpenTagset")); //$NON-NLS-1$
		btnOpenTagset.addStyleName("primary-button"); //$NON-NLS-1$

		buttonHeaderPanel.addComponent(btnOpenTagset);

		btHelp = new Button(FontAwesome.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$

		buttonHeaderPanel.addComponent(btHelp);
		buttonHeaderPanel.setComponentAlignment(btHelp, Alignment.MIDDLE_RIGHT);

		tabContent.addComponent(buttonHeaderPanel);

		tagsetTree = new TagsetTree(repository.getTagManager(), null, false, false, true, true, false,
				colorButtonListener);

		tabContent.addComponent(tagsetTree);
		tabContent.setExpandRatio(tagsetTree, 1.0f);

		tabSheet.addTab(tabContent, Messages.getString("MarkupPanel.ActiveTagsets")); //$NON-NLS-1$

		markupCollectionsPanel = new MarkupCollectionsPanel(repository, new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				handleOpenUserMarkupCollectionRequest(repository.getSourceDocument(sourceDocumentId));
			}
		});
		markupCollectionsPanel.addPropertyChangeListener(MarkupCollectionPanelEvent.tagDefinitionSelected,
				tagDefinitionSelectionListener);
		markupCollectionsPanel.addPropertyChangeListener(MarkupCollectionPanelEvent.tagDefinitionsRemoved,
				tagDefinitionsRemovedListener);

		markupCollectionsPanel.addPropertyChangeListener(MarkupCollectionPanelEvent.userMarkupCollectionSelected,
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getNewValue() != null) {
							writableUserMarkupCollectionInfo.setValue(evt.getNewValue().toString());
						} else {
							writableUserMarkupCollectionInfo
									.setValue(Messages.getString("MarkupPanel.noAnnotationCollectionAvailable")); //$NON-NLS-1$
						}
						colorButtonListener.setEnabled(evt.getNewValue() != null);
					}
				});

		tabSheet.addTab(markupCollectionsPanel, Messages.getString("MarkupPanel.activeAnnotations")); //$NON-NLS-1$

		addComponent(tabSheet);

		Component markupInfoPanel = createInfoPanel();
		addComponent(markupInfoPanel);
	}

	private void handleOpenUserMarkupCollectionRequest(final SourceDocument sourceDocument) {
		CorpusContentSelectionDialog dialog = new CorpusContentSelectionDialog(repository, sourceDocument, null,
				new SaveCancelListener<Corpus>() {
					public void cancelPressed() {
						/* noop */}

					public void savePressed(Corpus result) {

						List<UserMarkupCollectionReference> markupCollectionReferences = result
								.getUserMarkupCollectionRefs(sourceDocument);

						try {
							for (UserMarkupCollectionReference umcRef : markupCollectionReferences) {
								UserMarkupCollection userMarkupCollection = repository.getUserMarkupCollection(umcRef);
								openUserMarkupCollection(userMarkupCollection);
							}
						} catch (IOException ioe) {
							((CatmaApplication) UI.getCurrent())
									.showAndLogError(Messages.getString("MarkupPanel.errorFetchingAnnotations"), ioe); //$NON-NLS-1$
						}
					}
				}, Messages.getString("MarkupPanel.openAnnotations"), //$NON-NLS-1$
				Messages.getString("MarkupPanel.chooseAnnotationsToOpen") //$NON-NLS-1$
		);
		dialog.show();
	}

	//
	private void handleOpenUserMarkupCollectionRequest(final SourceDocument sourceDocument,
			final CollectionSelectionListener collectionSelectionListener) {
		CorpusContentSelectionDialog dialog = new CorpusContentSelectionDialog(repository, sourceDocument, null,
				new SaveCancelListener<Corpus>() {
					public void cancelPressed() {
						/* noop */}

					public void savePressed(Corpus result) {

						List<UserMarkupCollectionReference> markupCollectionReferences = result
								.getUserMarkupCollectionRefs(sourceDocument);

						try {
							for (UserMarkupCollectionReference umcRef : markupCollectionReferences) {
								UserMarkupCollection userMarkupCollection = repository.getUserMarkupCollection(umcRef);
								openUserMarkupCollection(userMarkupCollection, false);

							}
							// hier ruf ich den event- handler auf ...
							collectionSelectionListener.collectionSelected();
						} catch (IOException ioe) {
							((CatmaApplication) UI.getCurrent())
									.showAndLogError(Messages.getString("MarkupPanel.errorFetchingAnnotations"), ioe); //$NON-NLS-1$
						}
					}
				}, Messages.getString("MarkupPanel.openAnnotations"), //$NON-NLS-1$
				Messages.getString("MarkupPanel.chooseAnnotationsToOpen") //$NON-NLS-1$
		);
		dialog.show();
	}

	private void handleOpenTagsetRequest() {
		new TagsetSelectionDialog(repository).show();
	}

	private Component createInfoPanel() {
		markupInfoScrollPanel = new Panel();
		markupInfoScrollPanel.setSizeFull();

		VerticalLayout markupInfoPanel = new VerticalLayout();
		markupInfoPanel.setSizeUndefined();
		markupInfoPanel.setWidth("100%"); //$NON-NLS-1$

		markupInfoPanel.setSpacing(true);
		markupInfoPanel.addStyleName("catma-tagger-panels"); //$NON-NLS-1$
		HorizontalLayout writableMarkupCollPanel = new HorizontalLayout();
		writableMarkupCollPanel.setSpacing(true);

		markupInfoPanel.addComponent(writableMarkupCollPanel);

		Label writableUserMarkupCollectionLabel = new Label(
				Messages.getString("MarkupPanel.writableAnnotationCollection")); //$NON-NLS-1$

		writableUserMarkupCollectionLabel.addStyleName("bold-label-caption"); //$NON-NLS-1$
		writableUserMarkupCollectionLabel.addStyleName("catma-label-spacing"); //$NON-NLS-1$
		writableMarkupCollPanel.addComponent(writableUserMarkupCollectionLabel);

		writableUserMarkupCollectionInfo = new Label();
		writableUserMarkupCollectionInfo.addStyleName("bold-label-caption"); //$NON-NLS-1$
		writableUserMarkupCollectionInfo.addStyleName("catma-label-spacing"); //$NON-NLS-1$
		writableMarkupCollPanel.addComponent(writableUserMarkupCollectionInfo);

		tagInstancesTree = new TagInstanceTree(this);
		tagInstancesTree.setSizeFull();
		markupInfoPanel.addComponent(tagInstancesTree);
		markupInfoScrollPanel.setContent(markupInfoPanel);
		return markupInfoScrollPanel;
	}

	public void addOrUpdateTagsetDefinition(UI ui, final TagsetDefinition tagsetDefinition) {
		markupCollectionsPanel.addOrUpdateTagsetDefinition(ui, tagsetDefinition, new ConfirmListener() {
			public void confirmed() {
				tagsetTree.addTagsetDefinition(tagsetDefinition);
			}
		});
	}

	public void openUserMarkupCollection(final UserMarkupCollection userMarkupCollection) {
		openUserMarkupCollection(userMarkupCollection,true);
		
	}
	private void openUserMarkupCollection(final UserMarkupCollection userMarkupCollection, boolean tabswitch) {

		final UserMarkupCollectionManager umcManager = new UserMarkupCollectionManager(repository);
		umcManager.add(userMarkupCollection);

		List<TagsetDefinition> activeTagsetDefs = tagsetTree.getTagsetDefinitions();
		final List<TagsetDefinition> outOfSyncTagsetDefs = new ArrayList<TagsetDefinition>();

		for (TagsetDefinition activeTagsetDef : activeTagsetDefs) {
			List<UserMarkupCollection> toBeUpdated = umcManager.getOutOfSyncUserMarkupCollections(activeTagsetDef);

			if (!toBeUpdated.isEmpty()) { // the list will be empty or contain
											// exactly one element
				outOfSyncTagsetDefs.add(activeTagsetDef);
			}
		}

		if (outOfSyncTagsetDefs.isEmpty()) {
			markupCollectionsPanel.openUserMarkupCollection(userMarkupCollection);
			if (tabswitch && !userMarkupCollection.isEmpty()) {
				tabSheet.setSelectedTab(markupCollectionsPanel);
			}
		} else {
			ConfirmDialog.show(UI.getCurrent(), Messages.getString("synchTagsetsQuestion"), //$NON-NLS-1$

					new ConfirmDialog.Listener() {

						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								for (TagsetDefinition outOfSyncTagsetDef : outOfSyncTagsetDefs) {
									umcManager.updateUserMarkupCollections(
											Collections.singletonList(userMarkupCollection), outOfSyncTagsetDef);
								}

								markupCollectionsPanel.openUserMarkupCollection(userMarkupCollection);
								if (tabswitch && !userMarkupCollection.isEmpty()) {
									tabSheet.setSelectedTab(markupCollectionsPanel);
								}
							}
						}
					});

		}

	}

	public void close() {
		markupCollectionsPanel.close();

		tagsetTree.getTagManager().removePropertyChangeListener(TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);
		repository.removePropertyChangeListener(RepositoryChangeEvent.propertyValueChanged,
				propertyValueChangeListener);

		tagsetTree.close(false);
	}

	public TagDefinition getTagDefinition(String tagDefinitionID) {
		return tagsetTree.getTagDefinition(tagDefinitionID);
	}

	public void addTagReferences(List<TagReference> tagReferences) {
		markupCollectionsPanel.addTagReferences(tagReferences);
	}

	public TagsetDefinition getTagsetDefinition(TagDefinition tagDefinition) {
		return tagsetTree.getTagsetDefinition(tagDefinition);
	}

	public TagsetDefinition getTagsetDefinition(String tagDefinitionID) {
		return tagsetTree.getTagsetDefinition(tagDefinitionID);
	}

	public UserMarkupCollection getCurrentWritableUserMarkupCollection() {
		return markupCollectionsPanel.getCurrentWritableUserMarkupCollection();
	}

	public List<UserMarkupCollection> getUserMarkupCollections() {
		return markupCollectionsPanel.getUserMarkupCollections();
	}

	public Repository getRepository() {
		return markupCollectionsPanel.getRepository();
	}

	public void showTagInstanceInfo(Collection<String> instanceIDs, String tagInstanceID) {
		List<TagInstanceInfo> tagInstances = markupCollectionsPanel.getTagInstances(instanceIDs);

		tagInstancesTree.setTagInstances(tagInstances);
		if (tagInstanceID != null) {
			tagInstancesTree.setValue(tagInstanceID);

		}
		markupInfoScrollPanel.setScrollTop(1);
	}

	public void showTagInstanceInfo(TagReference[] deselectedTagRefs) {
		Set<TagDefinition> exclusionFilter = new HashSet<TagDefinition>();
		for (TagReference tr : deselectedTagRefs) {
			exclusionFilter.add(tr.getTagDefinition());
		}
		showTagInstanceInfo(tagInstancesTree.getTagInstanceIDs(exclusionFilter), null);
	}

	public void removeTagInstances(List<String> tagInstanceIDs) {
		markupCollectionsPanel.removeTagInstances(tagInstanceIDs);

	}

	public void updateProperty(TagInstance tagInstance, Collection<Property> properties) {
		markupCollectionsPanel.updateProperty(tagInstance, properties);
	}

	public void showPropertyEditDialog(TagInstance tagInstance) {
		showTagInstanceInfo(Collections.singletonList(tagInstance.getUuid()), null);
		if (!tagInstance.getUserDefinedProperties().isEmpty()) {
			tagInstancesTree.showPropertyEditDialog(tagInstance);
		}
	}

	@Override
	public void tagInstanceSelected(TagInstance tagInstance) {
		tagInstanceSelectedListener.tagInstanceSelected(tagInstance);
	}

	public TagInstanceInfo getTagInstanceInfo(String tagInstanceId) {
		return markupCollectionsPanel.getTagInstanceInfo(tagInstanceId);
	}
}