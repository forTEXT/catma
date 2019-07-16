package de.catma.repository.git.serialization.models;

import java.util.SortedSet;
import java.util.TreeSet;

public class GitTagsetHeader extends GitHeaderBase {
	
	private TreeSet<String> deletedDefinitions;
	
	public GitTagsetHeader() {
		super();
		this.deletedDefinitions = new TreeSet<>();
	}

	public GitTagsetHeader(String name, String description, TreeSet<String> deletedDefinitions) {
		super(name, description);
		this.deletedDefinitions = deletedDefinitions;
	}
	
	public SortedSet<String> getDeletedDefinitions() {
		return deletedDefinitions;
	}
	
	public void setDeletedDefinitions(TreeSet<String> deletedDefinitions) {
		this.deletedDefinitions = deletedDefinitions;
	}
	
}
