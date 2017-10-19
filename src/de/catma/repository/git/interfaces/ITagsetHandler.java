package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.tag.TagDefinition;

public interface ITagsetHandler {
	String create(String name, String description, String projectId) throws TagsetHandlerException;
	void delete(String tagsetId) throws TagsetHandlerException;
	String addTagDefinition(String tagsetId, TagDefinition tagDefinition) throws TagsetHandlerException;
}
