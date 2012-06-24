package de.catma.ui.tagmanager;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
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
		setWidth("100%");
		tagsetTree = new TagsetTree(tagManager, tagLibrary);
		addComponent(tagsetTree);
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init) {
			//FIXME: this is bullshit, see RepositoryView and clean this mess up
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
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}
