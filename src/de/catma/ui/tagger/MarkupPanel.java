package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.Application;
import com.vaadin.data.Container;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.terminal.ClassResource;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Alignment;
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
	private Application application;
	private PropertyChangeListener tagDefinitionSelectionListener;
	private PropertyChangeListener tagDefinitionsRemovedListener;
	
	public MarkupPanel(
			Repository repository, ColorButtonListener colorButtonListener, 
			PropertyChangeListener tagDefinitionSelectionListener,
			PropertyChangeListener tagDefinitionsRemovedListener) {
		this.colorButtonListener = colorButtonListener;
		this.repository = repository;
		this.tagDefinitionSelectionListener = tagDefinitionSelectionListener;
		this.tagDefinitionsRemovedListener = tagDefinitionsRemovedListener;
		
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
		VerticalLayout tabContent = new VerticalLayout();
		tabContent.setSpacing(true);
		
		Label helpLabel = new Label();
		
		helpLabel.setIcon(new ClassResource(
				"ui/resources/icon-help.gif", 
				application));
		helpLabel.setWidth("20px");
		helpLabel.setDescription(
				"<h3>Hints</h3>" +
			    "<h4>Creating Tags</h4>" +
				"<ol><li>First you have to tell CATMA which Tagset you want to use. " +
				"Open a Tag Library from the Repository Manager and drag a Tagset to the \"Active Tagsets\" section." +
				" If you already have an active Tagset you want to use, you can skip this step.</li>" +
				"<li>Now you can select the Tagset and click the \"Create Tag\"-Button.</li></ol>"+
				"<h4>Tag this Source Document</h4>" +
				"<ol><li>First you have to tell CATMA which Tagset you want to use. " +
				"Open a Tag Library from the Repository Manager and drag a Tagset to the \"Active Tagsets\" section." +
				" If you already have an active Tagset you want to use, you can skip this step.</li>" +
				"<li>Now you can mark the text sequence you want to tag.</li><li>Click the colored button of the desired Tag to apply it to the marked sequence.</li></ol> " +
				"When you click on a tagged text, i. e. a text that is underlined with colored bars you should see " +
				"the available Tag Instances in the section on the lower right of this view.");
		tabContent.addComponent(helpLabel);
		tabContent.setComponentAlignment(helpLabel, Alignment.MIDDLE_RIGHT);
		
		tagsetTree = 
			new TagsetTree(
				repository.getTagManager(), null, false, colorButtonListener);
		tabContent.addComponent(tagsetTree);
		
		tabSheet.addTab(tabContent, "Active Tagsets");
		
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
			
			application = getApplication();
			//TODO: maybe accept drags only from the TaggerView table, drag only tagset defs
			//TODO: allow tagsetdefs removal, remove from the editable set of the coll panel
			initComponents( 
					tagDefinitionSelectionListener,
					tagDefinitionsRemovedListener);
			initActions();

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
			final UserMarkupCollection userMarkupCollection) {
		
		final UserMarkupCollectionManager umcManager = 
				new UserMarkupCollectionManager(repository);
		umcManager.add(userMarkupCollection);
		
		List<TagsetDefinition> activeTagsetDefs = tagsetTree.getTagsetDefinitions();
		final List<TagsetDefinition> outOfSyncTagsetDefs = new ArrayList<TagsetDefinition>();

		for (TagsetDefinition activeTagsetDef : activeTagsetDefs) {
			List<UserMarkupCollection> toBeUpdated = 
					umcManager.getUserMarkupCollections(activeTagsetDef, false);
			
			if (!toBeUpdated.isEmpty()) { // the list will be empty or contain exactly one element
				outOfSyncTagsetDefs.add(activeTagsetDef);
			}
		}
		
		if (outOfSyncTagsetDefs.isEmpty()) {
			markupCollectionsPanel.openUserMarkupCollection(
					userMarkupCollection);
			if (!userMarkupCollection.isEmpty()) {
				tabSheet.setSelectedTab(markupCollectionsPanel);
			}
		}
		else {
			ConfirmDialog.show(
					application.getMainWindow(), 
					"One or more of the active Tagsets are newer than their "
					+ " correpsonding Tagsets in the User Markup Collection you want to open!"
					+ " The Collection will be updated with the versions of the active Tagsets! " 
					+ " Do you really want to update the attached Markup Collections?",
								
				        new ConfirmDialog.Listener() {

				            public void onClose(ConfirmDialog dialog) {
				            	List<UserMarkupCollection> oneElementCollection =
				            			new ArrayList<UserMarkupCollection>();
				            	oneElementCollection.add(userMarkupCollection);
				            	
				            	for (TagsetDefinition outOfSyncTagsetDef : outOfSyncTagsetDefs) {
				            		umcManager.updateUserMarkupCollections(
				            			oneElementCollection,
				            			outOfSyncTagsetDef);
				            	}
				            	
				    			markupCollectionsPanel.openUserMarkupCollection(
				    					userMarkupCollection);
				    			if (!userMarkupCollection.isEmpty()) {
				    				tabSheet.setSelectedTab(markupCollectionsPanel);
				    			}
				    		}
					});

		}
		
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