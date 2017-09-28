package de.catma.repository.git.interfaces;

public interface ITagsetHandler {
	String create(String name, String description);
	void delete(String tagsetId);
}
