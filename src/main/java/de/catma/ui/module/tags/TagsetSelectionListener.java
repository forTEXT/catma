package de.catma.ui.module.tags;

import java.util.Collection;

import de.catma.tag.TagsetDefinition;

public interface TagsetSelectionListener {
	public void tagsetsSelected(Collection<TagsetDefinition> tagsets);
}
