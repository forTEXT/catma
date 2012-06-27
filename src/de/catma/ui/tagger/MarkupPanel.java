package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

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
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
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
	
	public MarkupPanel(
			TagManager tagManager,
			Repository repository, ColorButtonListener colorButtonListener, 
			PropertyChangeListener tagDefinitionSelectionListener) {
		this.colorButtonListener = colorButtonListener;
		initComponents(
				tagManager, repository, tagDefinitionSelectionListener);
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
	}

	private void initComponents(
			final TagManager tagManager, Repository repository, 
			PropertyChangeListener tagDefinitionSelectionListener) {
		
		tabSheet = new TabSheet();
		tabSheet.setSizeFull();
		
		tagsetTree = new TagsetTree(tagManager, null, false, colorButtonListener);
		tabSheet.addTab(tagsetTree, "Active Tagsets");
		
		markupCollectionsPanel = 
				new MarkupCollectionsPanel(tagManager, repository);
		markupCollectionsPanel.addPropertyChangeListener(
				MarkupCollectionPanelEvent.tagDefinitionSelected, 
				tagDefinitionSelectionListener);
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
		markupCollectionsPanel.openUserMarkupCollection(
				userMarkupCollection);
	}

	public void close() {
		markupCollectionsPanel.close();
		tagsetTree.getTagManager().removePropertyChangeListener(
				TagManagerEvent.tagLibraryChanged, tagLibraryChangedListener);
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
	
	public void removeTagInstances(List<String> tagInstanceIDs) {
		markupCollectionsPanel.removeTagInstances(tagInstanceIDs);
		
	}
}