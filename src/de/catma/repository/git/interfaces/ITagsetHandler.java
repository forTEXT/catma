package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;

public interface ITagsetHandler {
	String create(String name, String description, Version version, String projectId) throws TagsetHandlerException;
	void delete(String tagsetId) throws TagsetHandlerException;
	String addTagDefinition(String tagsetId, TagDefinition tagDefinition) throws TagsetHandlerException;
	TagsetDefinition open(String tagsetId, String projectId) throws TagsetHandlerException;
}
