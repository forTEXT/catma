package de.catma.ui.module.tags;

import com.vaadin.data.TreeData;
import com.vaadin.server.SerializablePredicate;

final class TagsetSearchFilterProvider implements SerializablePredicate<TagsetTreeItem> {

	private final String searchInput;
	private final TreeData<TagsetTreeItem> tagsetData;

	public TagsetSearchFilterProvider(String searchInput, TreeData<TagsetTreeItem> tagsetData) {
		super();
		this.searchInput = searchInput.toLowerCase();
		this.tagsetData = tagsetData;
	}

	@Override
	public boolean test(TagsetTreeItem t) {
		return testWithChildren(t);
	}

	private boolean testTagsetTreeItem(TagsetTreeItem t) {
		String strValue = t.toString();
		
		if (strValue != null && strValue.toLowerCase().startsWith(searchInput)) {
			return true;
		}

		
		return false;
	}

	public boolean testWithChildren(TagsetTreeItem t) {
		if (t == null) {
			return false;
		}
		
		if (testTagsetTreeItem(t)) {
			return true;
		}
		
		for (TagsetTreeItem child : tagsetData.getChildren(t)) {
			if (testWithChildren(child)) {
				return true;
			}
		}
		
		return false;
	}
}