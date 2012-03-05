package de.catma.ui.tagger;

import com.vaadin.data.Container;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;
import de.catma.ui.tagmanager.TagsetTree;

public class MarkupPanel extends VerticalLayout {

	private TagsetTree tagsetTree;
	private TabSheet tabSheet;
	
	public MarkupPanel(ColorButtonListener colorButtonListener) {
		initComponents(colorButtonListener);
	}

	private void initComponents(ColorButtonListener colorButtonListener) {
		tabSheet = new TabSheet();
		tagsetTree = new TagsetTree(false, colorButtonListener);
		tabSheet.addTab(tagsetTree, "Currently active Tagsets");
		
		MarkupCollectionsPanel markupCollectionsPanel = 
				new MarkupCollectionsPanel();
		
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
}
