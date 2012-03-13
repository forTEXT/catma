package de.catma.ui.tagger;

import com.vaadin.data.Container;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagManager;
import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.tagger.MarkupCollectionsPanel.TagDefinitionSelectionListener;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;
import de.catma.ui.tagmanager.TagsetTree;

public class MarkupPanel extends VerticalLayout {

	private TagsetTree tagsetTree;
	private TabSheet tabSheet;
	private MarkupCollectionsPanel markupCollectionsPanel;
	
	public MarkupPanel(
			TagManager tagManager,
			ColorButtonListener colorButtonListener, 
			TagDefinitionSelectionListener tagDefinitionSelectionListener) {
		initComponents(
			tagManager, colorButtonListener, tagDefinitionSelectionListener);
	}

	private void initComponents(
			TagManager tagManager, ColorButtonListener colorButtonListener, 
			TagDefinitionSelectionListener tagDefinitionSelectionListener) {
		tabSheet = new TabSheet();
		tagsetTree = new TagsetTree(tagManager, null, false, colorButtonListener);
		tabSheet.addTab(tagsetTree, "Currently active Tagsets");
		
		markupCollectionsPanel = 
				new MarkupCollectionsPanel(tagDefinitionSelectionListener);
		
		tabSheet.addTab(
				markupCollectionsPanel, "Currently active Markup Collections");
		
		addComponent(tabSheet);
	}
	
	@Override
	public void attach() {
		super.attach();
		//TODO: maybe accept drags only from the TaggerView table, drag only tagset defs
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

                Object sourceItemId = transferable.getItemId();
                
                if (sourceItemId instanceof TagsetDefinition) {
                	tagsetTree.addTagsetDefinition(
                			(TagsetDefinition)sourceItemId);
                }
			}
		});
	}

	public void openUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {
		markupCollectionsPanel.openUserMarkupCollection(userMarkupCollection);
	}
}
