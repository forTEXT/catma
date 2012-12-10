package de.catma.ui.analyzer.querybuilder;

import java.util.Map;

import de.catma.tag.TagsetDefinition;

public interface TagsetDefinitionDictionaryListener {

	public void tagsetDefinitionDictionarySelected(
			Map<String, TagsetDefinition> tagsetDefinitionsByUuid);
}
