package de.catma.tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RootsFirstDepthFirstIterator implements Iterator<TagDefinition> {
	
	private Iterator<TagDefinition> iterator;

	public RootsFirstDepthFirstIterator(TagsetDefinition tagsetDefinition) {
		List<TagDefinition> tags = new ArrayList<TagDefinition>(tagsetDefinition.size());
		for (TagDefinition tag : tagsetDefinition.getRootTagDefinitions()) {
			tags.add(tag);
			addChildren(tag, tagsetDefinition, tags);
		}
		this.iterator = tags.iterator();
	}

	private void addChildren(TagDefinition tag, TagsetDefinition tagsetDefinition, List<TagDefinition> tags) {
		for (TagDefinition child : tagsetDefinition.getChildren(tag)) {
			tags.add(child);
			addChildren(child, tagsetDefinition, tags);
		}
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public TagDefinition next() {
		return iterator.next();
	}

}
