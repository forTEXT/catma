package de.catma.ui.tagmanager;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table.TableDragMode;

import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;

public class TagLibraryView extends HorizontalLayout {
	
	private TagLibrary tagLibrary;
	private TagsetTree tagsetTree;
	
	public TagLibraryView(TagLibrary tagLibrary) {
		super();
		this.tagLibrary = tagLibrary;
		initComponents();
		initActions();
	}
	
	private void initActions() {
		
//		btInsertTagset.addListener(new ClickListener() {
//			
//			public void buttonClick(ClickEvent event) {
//				Object value = tagTree.getValue();
//				if (value instanceof TagsetDefinition) {
//					try {
//						((CleaApplication)getApplication()).attachTagsetDefinition(
//								(TagsetDefinition)value);
//					} catch (JSONException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				
//			}
//		});
		
	}

	private void initComponents() {
		setWidth("100%");
		tagsetTree = new TagsetTree();
		addComponent(tagsetTree);
	}
	
	@Override
	public void attach() {
		super.attach();
		
		WebApplicationContext context = 
				((WebApplicationContext) getApplication().getContext());
		WebBrowser wb = context.getBrowser();
		tagsetTree.setHeight(wb.getScreenHeight()*0.52f, UNITS_PIXELS);
		
		tagsetTree.getTagTree().setDragMode(TableDragMode.ROW);

		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			tagsetTree.addTagsetDefinition(tagsetDefinition);
		}
	}

	public String getTagLibraryName() {
		return tagLibrary.getName();
	}
	
}
