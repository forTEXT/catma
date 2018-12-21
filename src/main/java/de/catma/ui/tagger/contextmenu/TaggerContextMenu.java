package de.catma.ui.tagger.contextmenu;

import java.util.Collection;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.tagger.Tagger;
import de.catma.util.ColorConverter;

public class TaggerContextMenu {
	
	public interface TagSelectionListener {
		public void tagSelected(TagDefinition tagDefinition);
	}
	
	private static final class TagDefinitionCommand implements Command {
		
		private TagDefinition tagDefinition;
		private TagSelectionListener tagSelectionListener;
		
		public TagDefinitionCommand(
				TagDefinition tagDefinition, TagSelectionListener tagSelectionListener) {
			super();
			this.tagDefinition = tagDefinition;
			this.tagSelectionListener = tagSelectionListener;
		}

		@Override
		public void menuSelected(MenuItem selectedItem) {
			if (tagSelectionListener != null) {
				tagSelectionListener.tagSelected(tagDefinition);
			}
			
		}
		
	}
	
	private ContextMenu contextMenu;
	private TagSelectionListener tagSelectionListener;
	
	public TaggerContextMenu(Tagger tagger) {
		initComponents(tagger);
	}

	public void setTagSelectionListener(TagSelectionListener tagSelectionListener) {
		this.tagSelectionListener = tagSelectionListener;
	}
	
	private void initComponents(Tagger tagger) {
		contextMenu = new ContextMenu(tagger, true);
		contextMenu.setHtmlContentAllowed(true);
	}
	
	public void setTagsets(Collection<TagsetDefinition> tagsets) {
		
		for (TagsetDefinition tagset : tagsets) {
			MenuItem tagsetItem = contextMenu.addItem(tagset.getName(), VaadinIcons.TAGS, null);
		
			tagset.getRootTagDefinitions()
				.forEach(rootTag -> addToMenuItem(tagsetItem, tagset, rootTag));
		
		}
	}

	private void addToMenuItem(MenuItem menuItem, TagsetDefinition tagset, TagDefinition tag) {
		
		MenuItem tagMenuItem = menuItem.addItem(
			"<div "
			+ "class=\"tagger-contextmenu-menuitem-caption\" "
			+ "style=\"background-color:#"+ColorConverter.toHex(tag.getColor())+"\">"
			+ "&nbsp;</div>" 
			+ tag.getName(), 
			new TagDefinitionCommand(tag, tagSelectionListener));	
		
		tagMenuItem.setStyleName("tagger-contextmenu-menuitem");

		for (TagDefinition childTag : tagset.getChildren(tag)) {
			addToMenuItem(tagMenuItem, tagset, childTag);
		}
	}

}
