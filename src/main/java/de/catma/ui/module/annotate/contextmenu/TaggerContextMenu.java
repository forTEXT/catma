package de.catma.ui.module.annotate.contextmenu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.tag.TagDefinition;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.annotate.Tagger;
import de.catma.util.ColorConverter;
import de.catma.util.Pair;

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
	private Map<Object, MenuItem> entryToMenuItemMap = new HashMap<>();
	private TagManager tagManager;
	private PropertyChangeListener tagChangedListener;

	
	public TaggerContextMenu(
			Tagger tagger, 
			TagManager tagManager) {
		this.tagManager = tagManager;

		initComponents(tagger);
		initListeners();
	}

	private void initListeners() {
		tagChangedListener = new PropertyChangeListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				
				Object newValue = evt.getNewValue();
				Object oldValue = evt.getOldValue();
				
				if (oldValue == null) { //created
					Pair<TagsetDefinition, TagDefinition> value = 
							(Pair<TagsetDefinition, TagDefinition>)newValue;
					
					TagsetDefinition tagset = value.getFirst();
					TagDefinition tag = value.getSecond();
					Object parent = tagset;
					if (!tag.getParentUuid().isEmpty()) {
						parent = tagset.getTagDefinition(tag.getParentUuid());
					}
					
					MenuItem parentItem = entryToMenuItemMap.get(parent);
					if (parentItem != null) {
						addToMenuItem(parentItem, tagset, tag);
					}
				}
				else if (newValue == null) { //removed
					Pair<TagsetDefinition,TagDefinition> deleted = (Pair<TagsetDefinition, TagDefinition>) oldValue;
					
					TagDefinition deletedTag = deleted.getSecond();
					MenuItem menuItem = entryToMenuItemMap.get(deletedTag);
					if (menuItem != null) {
						contextMenu.removeItem(menuItem);
					}
				}
				else { //update
					TagDefinition tag = (TagDefinition) newValue;
					TagsetDefinition tagset = (TagsetDefinition)oldValue;

	            	MenuItem menuItem = entryToMenuItemMap.get(tag);
	            	if (menuItem != null) {
	            		menuItem.setText(createTagMenuItemCaption(tag));
	            	}
	            	
				}
				
			}
		};
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
	}

	public void setTagSelectionListener(TagSelectionListener tagSelectionListener) {
		this.tagSelectionListener = tagSelectionListener;
	}
	
	private void initComponents(Tagger tagger) {
		contextMenu = new ContextMenu(tagger, true);
		contextMenu.setHtmlContentAllowed(true);
	}
	
	public void setTagsets(Collection<TagsetDefinition> tagsets) {
		contextMenu.removeItems();
		entryToMenuItemMap.clear();
		for (TagsetDefinition tagset : tagsets) {
			addTagset(tagset);
		}
	}

	private void addToMenuItem(MenuItem parentMenuItem, TagsetDefinition tagset, TagDefinition tag) {
		
		MenuItem tagMenuItem = parentMenuItem.addItem(
			createTagMenuItemCaption(tag), 
			new TagDefinitionCommand(tag, tagSelectionListener));	
		
		tagMenuItem.setStyleName("tagger-contextmenu-menuitem");
		entryToMenuItemMap.put(tag, tagMenuItem);
		
		for (TagDefinition childTag : tagset.getDirectChildren(tag)) {
			addToMenuItem(tagMenuItem, tagset, childTag);
		}
	}

	private String createTagMenuItemCaption(TagDefinition tag) {
		return
				"<div "
				+ "class=\"tagger-contextmenu-menuitem-caption\" "
				+ "style=\"background-color:#"+ColorConverter.toHex(tag.getColor())+"\">"
				+ "&nbsp;</div>" 
				+ tag.getName();
	}

	private void addTagset(TagsetDefinition tagset) {
		MenuItem tagsetItem = contextMenu.addItem(tagset.getName(), VaadinIcons.TAGS, null);
		entryToMenuItemMap.put(tagset, tagsetItem);
		
		tagset.getRootTagDefinitions()
			.forEach(rootTag -> addToMenuItem(tagsetItem, tagset, rootTag));
	}
	
	public void removeTagset(TagsetDefinition tagset) {
		contextMenu.removeItem(entryToMenuItemMap.get(tagset));
		entryToMenuItemMap.remove(tagset);
	}

	public void close() {
		tagSelectionListener = null;
		contextMenu.remove();
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
	}

	public void show(int x, int y) {
		contextMenu.open(x, y);
	}

}
