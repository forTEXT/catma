package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.data.Container;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.tagger.MarkupCollectionsPanel.MarkupCollectionPanelEvent;
import de.catma.ui.tagger.TagInstanceTree.TagIntanceActionListener;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;
import de.catma.ui.tagmanager.TagsetTree;
import de.catma.util.Pair;

public class MarkupPanel extends VerticalSplitPanel implements TagIntanceActionListener {

	private TagsetTree tagsetTree;
	private TabSheet tabSheet;
	private MarkupCollectionsPanel markupCollectionsPanel;
	private boolean init = true;
	private PropertyChangeListener tagLibraryChangedListener;
	private ColorButtonListener colorButtonListener;
	private Label writableUserMarkupCollectionLabel;
	private TagInstanceTree tagInstancesTree;
	private Repository repository;
	private PropertyChangeListener propertyValueChangeListener;
	
	public MarkupPanel(
			Repository repository, ColorButtonListener colorButtonListener, 
			PropertyChangeListener tagDefinitionSelectionListener,
			PropertyChangeListener tagDefinitionsRemovedListener) {
		this.colorButtonListener = colorButtonListener;
		this.repository = repository;
		initComponents( 
				tagDefinitionSelectionListener,
				tagDefinitionsRemovedListener);
		initActions();
	}

	private void initActions() {
		this.tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { // removal
					TagLibrary tagLibrary = (TagLibrary) evt.getOldValue();
					for (TagsetDefinition tagsetDefinition : tagLibrary) {
						tagsetTree.removeTagsetDefinition(tagsetDefinition);
						markupCollectionsPanel.removeUpdateableTagsetDefinition(
								tagsetDefinition);
					}
				}
			}
		};
		tagsetTree.getTagManager().addPropertyChangeListener(
			TagManagerEvent.tagLibraryChanged, tagLibraryChangedListener);
		
		markupCollectionsPanel.addPropertyChangeListener(
				MarkupCollectionPanelEvent.tagDefinitionSelected, 
				new PropertyChangeListener() {
					
					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getNewValue() == null) {
							@SuppressWarnings("unchecked")
							List<TagReference> deselectedTagRefs = 
									(List<TagReference>) evt.getOldValue();
							showTagInstanceInfo(deselectedTagRefs.toArray(new TagReference[]{}));
						}
					}
				});
		markupCollectionsPanel.addPropertyChangeListener(
				MarkupCollectionPanelEvent.tagDefinitionsRemoved,
				new PropertyChangeListener() {
					
					@SuppressWarnings("unchecked")
					public void propertyChange(PropertyChangeEvent evt) {
						showTagInstanceInfo(
							tagInstancesTree.getTagInstanceIDs(
								(Set<TagDefinition>)evt.getOldValue()));
					}
				});
		
		propertyValueChangeListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if ((evt.getNewValue() != null) && (evt.getOldValue() != null)) {
					showTagInstanceInfo(
						tagInstancesTree.getTagInstanceIDs(
								Collections.<TagDefinition>emptySet()));
				}
				
			}
		};
		
		repository.addPropertyChangeListener(
			RepositoryChangeEvent.propertyValueChanged, 
			propertyValueChangeListener);
	}
	
	

	private void initComponents(
			PropertyChangeListener tagDefinitionSelectionListener, 
			PropertyChangeListener tagDefinitionsRemovedListener) {
		
		tabSheet = new TabSheet();
		tabSheet.setSizeFull();
		
		tagsetTree = 
			new TagsetTree(
				repository.getTagManager(), null, false, colorButtonListener);
		tabSheet.addTab(tagsetTree, "Active Tagsets");
		
		markupCollectionsPanel = 
				new MarkupCollectionsPanel(repository);
		markupCollectionsPanel.addPropertyChangeListener(
				MarkupCollectionPanelEvent.tagDefinitionSelected, 
				tagDefinitionSelectionListener);
		markupCollectionsPanel.addPropertyChangeListener(
				MarkupCollectionPanelEvent.tagDefinitionsRemoved,
				tagDefinitionsRemovedListener);
		
		markupCollectionsPanel.addPropertyChangeListener(
				MarkupCollectionPanelEvent.userMarkupCollectionSelected, 
				new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() != null) {
					writableUserMarkupCollectionLabel.setValue(
							evt.getNewValue());
				}
				else {
					writableUserMarkupCollectionLabel.setValue("");
				}
				colorButtonListener.setEnabled(evt.getNewValue() != null);
			}
		});
		
		tabSheet.addTab(
				markupCollectionsPanel, "Active Markup Collections");
		
		addComponent(tabSheet);
		
		Component markupInfoPanel = createInfoPanel();
		addComponent(markupInfoPanel);
	}
	
	private Component createInfoPanel() {
		VerticalLayout markupInfoPanel = new VerticalLayout();
		markupInfoPanel.setSpacing(true);
		writableUserMarkupCollectionLabel = new Label();
		writableUserMarkupCollectionLabel.addStyleName("bold-label-caption");
		writableUserMarkupCollectionLabel.setCaption(
				"Writable Markup Collection:");
		markupInfoPanel.addComponent(
				writableUserMarkupCollectionLabel);
		
		tagInstancesTree = new TagInstanceTree(this);
		tagInstancesTree.setSizeFull();
		markupInfoPanel.addComponent(tagInstancesTree);
		
		return markupInfoPanel;
	}


	@Override
	public void attach() {
		super.attach();
		if (init) {
			//TODO: maybe accept drags only from the TaggerView table, drag only tagset defs
			//TODO: allow tagsetdefs removal, remove from the editable set of the coll panel
			
			tagsetTree.getTagTree().setDropHandler(new DropHandler() {
				
				public AcceptCriterion getAcceptCriterion() {
					
					return AcceptItem.ALL;
				}
				
				public void drop(DragAndDropEvent event) {
					DataBoundTransferable transferable = 
							(DataBoundTransferable)event.getTransferable();
					
	                if (!(transferable.getSourceContainer() 
	                		instanceof Container.Hierarchical)) {
	                    return;
	                }
	
	                final Object sourceItemId = transferable.getItemId();
	                
	                if (sourceItemId instanceof TagsetDefinition) {
	                	
	                	TagsetDefinition incomingTagsetDef =
	                			(TagsetDefinition)sourceItemId;
	                	
	                	markupCollectionsPanel.addOrUpdateTagsetDefinition(
	                			incomingTagsetDef, 
	                			new ConfirmListener() {
	        			            public void confirmed() {
	        			            	tagsetTree.addTagsetDefinition(
	        			            			(TagsetDefinition)sourceItemId);
	        			            }
	                			});
	                }
				}
			});
			init = false;
		}
	}

	public void openUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {
//		
//		final UserMarkupCollectionManager umcManager = 
//				new UserMarkupCollectionManager(repository);
//		umcManager.add(userMarkupCollection);
//		
//		umcManager.getUserMarkupCollections(tagsetDefinition, inSynch)
//		List<TagsetDefinition> activeTagsetDefs = tagsetTree.getTagsetDefinitions();
//		for (TagsetDef)
//		
//		
//		if (!isInSyncWithActiveTagsetDefs(userMarkupCollection, activeTagsetDefs)) {
//			
//		}
		
		markupCollectionsPanel.openUserMarkupCollection(
				userMarkupCollection);
		if (!userMarkupCollection.isEmpty()) {
			tabSheet.setSelectedTab(markupCollectionsPanel);
		}
	}


	private boolean isInSyncWithActiveTagsetDefs(
			UserMarkupCollection userMarkupCollection,
			List<TagsetDefinition> activeTagsetDefs) {
		
		for (TagsetDefinition activeTsDef : activeTagsetDefs) {
			if (userMarkupCollection.getTagLibrary().contains(activeTsDef)) {
				TagsetDefinition incomingTsDef = 
						userMarkupCollection.getTagLibrary().getTagsetDefinition(activeTsDef.getUuid());
				if (!incomingTsDef.isSynchronized(activeTsDef)) {
					return false;
				}
			}
		}
		return true;
	}

	public void close() {
		markupCollectionsPanel.close();
		
		tagsetTree.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagLibraryChanged, tagLibraryChangedListener);
		repository.removePropertyChangeListener(
			RepositoryChangeEvent.propertyValueChanged, 
			propertyValueChangeListener);
		
		tagsetTree.close();
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

	public void showTagInstanceInfo(List<String> instanceIDs) {
		List<Pair<String,TagInstance>> tagInstances = 
				markupCollectionsPanel.getTagInstances(instanceIDs);
		tagInstancesTree.setTagInstances(tagInstances);
	}	
	
	public void showTagInstanceInfo(TagReference[] deselectedTagRefs) {
		Set<TagDefinition> exclusionFilter = new HashSet<TagDefinition>();
		for (TagReference tr : deselectedTagRefs) {
			exclusionFilter.add(tr.getTagDefinition());
		}
		showTagInstanceInfo(
			tagInstancesTree.getTagInstanceIDs(exclusionFilter));
	}
	
	public void removeTagInstances(List<String> tagInstanceIDs) {
		markupCollectionsPanel.removeTagInstances(tagInstanceIDs);
		
	}
	
	public void updateProperty(TagInstance tagInstance, Property property) {
		markupCollectionsPanel.updateProperty(tagInstance, property);
		
	}
}