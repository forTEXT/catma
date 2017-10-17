package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.TagsetHandlerException;

public interface ITagsetHandler {
	String create(String name, String description, String projectId) throws TagsetHandlerException;
	void delete(String tagsetId) throws TagsetHandlerException;
}
