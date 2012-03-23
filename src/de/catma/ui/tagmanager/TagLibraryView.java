package de.catma.ui.tagmanager;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table.TableDragMode;

import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.core.tag.TagsetDefinition;

public class TagLibraryView extends HorizontalLayout {
	
	private TagLibrary tagLibrary;
	private TagsetTree tagsetTree;
	private boolean init = true;
	
	public TagLibraryView(TagManager tagManager, TagLibrary tagLibrary) {
		super();
		this.tagLibrary = tagLibrary;
		initComponents(tagManager, tagLibrary);
	}

	private void initComponents(TagManager tagManager, TagLibrary tagLibrary) {
		setWidth("100%");
		tagsetTree = new TagsetTree(tagManager, tagLibrary);
		addComponent(tagsetTree);
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init) {
			WebApplicationContext context = 
					((WebApplicationContext) getApplication().getContext());
			WebBrowser wb = context.getBrowser();
			tagsetTree.setHeight(wb.getScreenHeight()*0.52f, UNITS_PIXELS);
			
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
	
}
