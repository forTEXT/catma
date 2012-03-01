package de.catma.ui.tagger;

import com.vaadin.data.Container;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.tagmanager.TagsetTree;

public class MarkupPanel extends VerticalLayout {
	
	private TagsetTree tagsetTree;

	public MarkupPanel() {
		initComponents();
	}

	private void initComponents() {
		tagsetTree = new TagsetTree(false);
		addComponent(tagsetTree);
	}
	
	@Override
	public void attach() {
		super.attach();
		
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
                
                Container.Hierarchical source = 
                		(Container.Hierarchical)transferable.getSourceContainer();

                Object sourceItemId = transferable.getItemId();
                
                if (!(sourceItemId instanceof TagsetDefinition)) {
                	return;
                }
                else {
                	tagsetTree.addTagsetDefinition(
                			(TagsetDefinition)sourceItemId);
                }
			}
		});
	}
}
