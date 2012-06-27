package de.catma.ui.tagmanager;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table.TableDragMode;

import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.tabbedview.ClosableTab;

public class TagLibraryView extends HorizontalLayout implements ClosableTab {
	
	private TagLibrary tagLibrary;
	private TagsetTree tagsetTree;
	private boolean init = true;
	
	public TagLibraryView(TagManager tagManager, TagLibrary tagLibrary) {
		super();
		this.tagLibrary = tagLibrary;
		initComponents(tagManager, tagLibrary);
	}

	private void initComponents(TagManager tagManager, TagLibrary tagLibrary) {
		setSizeFull();
		tagsetTree = new TagsetTree(tagManager, tagLibrary);
		addComponent(tagsetTree);
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init) {
			tagsetTree.getTagTree().setDragMode(TableDragMode.ROW);
	
			for (TagsetDefinition tagsetDefinition : tagLibrary) {
				tagsetTree.addTagsetDefinition(tagsetDefinition);
			}
			init = false;
		}
	}

	TagLibrary getTagLibrary() {
		return tagLibrary;
	}

	public void close() {
		tagsetTree.close();
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}
