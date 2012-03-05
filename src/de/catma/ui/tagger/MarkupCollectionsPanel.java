package de.catma.ui.tagger;

import com.vaadin.data.Container;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbstractSelect.AcceptItem;

public class MarkupCollectionsPanel extends VerticalLayout {
	private static enum MarkupCollectionTreeProperty {
		caption("Markup Collections")
		;
		
		private String displayString;

		private MarkupCollectionTreeProperty(String displayString) {
			this.displayString = displayString;
		}
		
		@Override
		public String toString() {
			return displayString;
		}
		
	}
	
	private TreeTable markupTable;
	private String userMarkupItem = "User Markup Collections";
	private String staticMarkupItem = "Static Markup Collections";

	public MarkupCollectionsPanel() {
		initComponents();
		initActions();
	}

	private void initActions() {
		markupTable.setDropHandler(new DropHandler() {
			
			public AcceptCriterion getAcceptCriterion() {
				//TODO: restrict somehow
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
                
//                if (sourceItemId instanceof )
                
                // hier gehts weiter: markup collection mit repository oeffnen und einfuegen
                // drag'n'drop oder doch besser per button?
			}
		});
		
	}

	private void initComponents() {
		markupTable = new TreeTable();
		markupTable.setContainerDataSource(new HierarchicalContainer());
		
		markupTable.addContainerProperty(
				MarkupCollectionTreeProperty.caption, 
				String.class, null);
		markupTable.setItemCaptionMode(Tree.ITEM_CAPTION_MODE_PROPERTY);
		markupTable.setItemCaptionPropertyId(
				MarkupCollectionTreeProperty.caption);
		markupTable.setVisibleColumns(
				new Object[] {MarkupCollectionTreeProperty.caption});
		
		markupTable.addItem(userMarkupItem).getItemProperty(
				MarkupCollectionTreeProperty.caption).setValue(userMarkupItem);
		markupTable.addItem(staticMarkupItem).getItemProperty(
				MarkupCollectionTreeProperty.caption).setValue(staticMarkupItem);
		
		addComponent(markupTable);
	}
	
	
}
