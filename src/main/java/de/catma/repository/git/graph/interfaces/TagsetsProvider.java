package de.catma.repository.git.graph.interfaces;

import de.catma.tag.TagsetDefinition;

import java.util.List;

public interface TagsetsProvider {
    List<TagsetDefinition> getTagsets();
}
